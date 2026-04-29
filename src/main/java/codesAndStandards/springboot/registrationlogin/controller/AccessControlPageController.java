package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AccessControlPageController {

    private final UserRepository userRepository;

    /**
     * Display Access Control Management page
     */
    @GetMapping("/access-control")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('GROUP_VIEW')")
    public String accessControlPage(Model model, Principal principal) {
        log.info("Displaying Access Control Management page");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userRole;

        // Check if the authenticated user is a superadmin
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("superadmin"))) {
            userRole = "superadmin";
            log.info("Superadmin '{}' accessing Access Control page.", principal.getName());
        } else {
            // For regular users, fetch from DB
            User user = userRepository.findByUsername(principal.getName());
            if (user == null) {
                log.error("User '{}' not found in database, but authenticated. This should not happen for non-superadmin.", principal.getName());
                // Redirect to login or show an error
                return "redirect:/login?error=userNotFound";
            }
            userRole = user.getRole().getRoleName();
            log.info("User '{}' (Role: {}) accessing Access Control page.", principal.getName(), userRole);
        }

        model.addAttribute("userRole", userRole);
        return "access-control";
    }
}
