package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.UserDto;
import codesAndStandards.springboot.registrationlogin.entity.ActivityLog;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.entity.Role; // Import Role
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.service.ActivityLogService;
import codesAndStandards.springboot.registrationlogin.service.UserService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasRole('superadmin') or hasAuthority('ACTIVITY_LOG_VIEW')")
public class ActivityLogsController {

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LicenseService licenseService;

    /**
     * View all activity logs
     */
    @GetMapping("/activity-logs")
    public String viewLogs(Model model) {
        // Get all logs
        List<ActivityLog> logs = activityLogService.getAllLogs();

        // Get user details for each log, handling superadmin (userId = 0L)
        Map<Long, User> userMap = new HashMap<>();
        for (ActivityLog log : logs) {
            if (log.getUser() != null) {
                Long userId = log.getUser().getUserId();
                if (userId != null) { // Ensure userId is not null
                    if (userId == 0L) { // Handle superadmin
                        User superadminUser = new User();
                        superadminUser.setUserId(0L);
                        superadminUser.setUsername("superadmin");
                        Role superadminRole = new Role();
                        superadminRole.setRoleName("superadmin");
                        superadminUser.setRole(superadminRole);
                        userMap.put(0L, superadminUser);
                    } else if (!userMap.containsKey(userId)) {
                        User user = userRepository.findById(userId).orElse(null);
                        userMap.put(userId, user);
                    }
                }
            } else {
                // If log.getUser() is null, it might be a system action or an old log entry
                // For now, we'll just skip it or handle it as an unknown user.
                // If superadmin actions are logged with null user, this needs adjustment.
            }
        }

        // Get statistics
        Long todayCount       = activityLogService.getTodayCount();
        Long countSuccessLogs = activityLogService.countSuccessLogs();
        Long countFailedLogs  = activityLogService.countFailedLogs();

        // Count download logs
        long countDownloadLogs = logs.stream()
                .filter(log -> "DOCUMENT_DOWNLOAD".equals(log.getAction()))
                .count();

        // ✅ Count settings-related logs (all actions starting with SETTINGS_)
        long countSettingsLogs = logs.stream()
                .filter(log -> log.getAction() != null && (
                        log.getAction().startsWith("SETTINGS_") ||
                                log.getAction().startsWith("DOCTYPE_") ||
                                log.getAction().startsWith("METADATA_")
                ))
                .count();

        // License check
        String currentEdition         = licenseService.getCurrentEdition();
        boolean isProfessionalEdition = "ED2".equalsIgnoreCase(currentEdition);
        boolean hasValidLicense       = licenseService.isLicenseValid();

        model.addAttribute("logs", logs);
        model.addAttribute("userMap", userMap);
        model.addAttribute("todayCount", todayCount);
        model.addAttribute("totalLogs", logs.size());
        model.addAttribute("countSuccessLogs", countSuccessLogs);
        model.addAttribute("countFailedLogs", countFailedLogs);
        model.addAttribute("countDownloadLogs", countDownloadLogs);
        // ✅ Pass settings count to the view
        model.addAttribute("countSettingsLogs", countSettingsLogs);

        model.addAttribute("currentEdition", currentEdition != null ? currentEdition : "No License");
        model.addAttribute("isProfessionalEdition", isProfessionalEdition);
        model.addAttribute("hasValidLicense", hasValidLicense);

        return "activity-logs";
    }

    /**
     * Get current user's recent activity logs (today) for notification bell
     * GET /api/notifications/recent
     */
    @GetMapping("/api/notifications/recent")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyRecentNotifications() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.ok(List.of());
            }

            LocalDateTime since = LocalDate.now().atStartOfDay();
            List<ActivityLog> logs = activityLogService.getRecentByUserId(user.getUserId(), since);

            List<Map<String, Object>> result = logs.stream().map(log -> {
                Map<String, Object> item = new HashMap<>();
                item.put("logId", log.getLogId());
                item.put("action", log.getAction());
                item.put("details", log.getDetails());
                item.put("timestamp", log.getTimestamp().toString());
                return item;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }


    @GetMapping("/activity-logs/download")
    @ResponseBody
    @PreAuthorize("hasRole('superadmin') or hasAuthority('ACTIVITY_LOG_VIEW')")
    public ResponseEntity<?> logActivityLogDownload() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            activityLogService.logByUsername(
                    username,
                    ActivityLogService.ACTIVITY_LOG_DOWNLOAD,
                    "Downloaded activity logs as CSV"
            );
            return ResponseEntity.ok(Map.of("message", "Download logged successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to log download"));
        }
    }

    @GetMapping("/apis/users/{userId}")
    @ResponseBody
    public ResponseEntity<?> getUserDetails(@PathVariable Long userId) {
        try {
            System.out.println("API called - Fetching user with ID: " + userId);
            // Handle superadmin (userId = 0L) for API calls as well
            if (userId == 0L) {
                UserDto superadminDto = new UserDto();
                superadminDto.setId(0L); // Corrected from setUserId to setId
                superadminDto.setUsername("superadmin"); // Corrected username
                superadminDto.setRoleName("superadmin");
                superadminDto.setEmail("superadmin@example.com"); // Placeholder email
                return ResponseEntity.ok(superadminDto);
            }

            UserDto userDTO = userService.findUserById(userId);
            if (userDTO == null) {
                System.out.println("User not found with ID: " + userId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            System.out.println("User found: " + userDTO.getUsername());
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            System.err.println("Error fetching user: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error fetching user details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}