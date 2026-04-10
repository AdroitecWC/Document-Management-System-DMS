package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.ApplicationSettingsDto;
import codesAndStandards.springboot.registrationlogin.entity.ApplicationSetting;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.DocumentVersion;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.ApplicationSettingRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentVersionRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.service.ApplicationSettingsService;
import codesAndStandards.springboot.registrationlogin.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationSettingsServiceImpl implements ApplicationSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSettingsServiceImpl.class);

    @Autowired
    private ApplicationSettingRepository settingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private DocumentService documentService;

    @Value("${file.upload-dir}")
    private String repositoryPath;

    // ==================== GENERAL ====================

    @Override
    @Transactional(readOnly = true)
    public ApplicationSettingsDto getAllSettings() {
        ApplicationSettingsDto dto = new ApplicationSettingsDto();
        dto.setRepositoryPath(repositoryPath);
        dto.setMaxFileSizeMb(getSettingAsInteger("max_file_size_mb", 50));
        dto.setAllowedFiles(getSetting("allowed_files"));
        ApplicationSetting lastUpdated = settingsRepository.findBySettingName("max_file_size_mb").orElse(null);
        if (lastUpdated != null) {
            dto.setUpdatedAt(lastUpdated.getUpdatedAt());
            if (lastUpdated.getUpdatedBy() != null) {
                dto.setUpdatedByUsername(lastUpdated.getUpdatedBy().getUsername());
            }
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public String getSetting(String settingName) {
        return settingsRepository.findBySettingName(settingName)
                .map(ApplicationSetting::getSettingValue)
                .orElse(null);
    }

    @Override
    public String getRepositoryPath() {
        return repositoryPath;
    }

    // ==================== REPOSITORY ====================

    @Override
    @Transactional
    public void updateRepositorySettings(Integer maxFileSizeMb, String allowedFiles, String username, boolean skipFileCheck) throws Exception {
        logger.info("Updating repository settings - MaxSize: {}MB, Formats: {}, User: {}", maxFileSizeMb, allowedFiles, username);

        if (maxFileSizeMb == null || maxFileSizeMb < 1 || maxFileSizeMb > 1000)
            throw new IllegalArgumentException("Max file size must be between 1 and 1000 MB");
        if (allowedFiles == null || allowedFiles.trim().isEmpty())
            throw new IllegalArgumentException("At least one file format must be allowed");

        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);

        if (!skipFileCheck) {
            Integer currentMaxSize = getMaxFileSizeMB();
            if (currentMaxSize != null && maxFileSizeMb < currentMaxSize) {
                int affectedFilesCount = countFilesExceedingSize(maxFileSizeMb);
                if (affectedFilesCount > 0)
                    throw new IllegalStateException(String.format(
                            "Cannot reduce max file size to %dMB. %d existing file(s) exceed this limit.",
                            maxFileSizeMb, affectedFilesCount));
            }
        }

        saveSettingValue("max_file_size_mb", String.valueOf(maxFileSizeMb), user);

        String cleanedFormats = Arrays.stream(allowedFiles.split(","))
                .map(String::trim).map(String::toUpperCase).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        saveSettingValue("allowed_files", cleanedFormats, user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFormatAllowed(String format) {
        if (format == null || format.trim().isEmpty()) return false;
        String allowedFiles = getSetting("allowed_files");
        if (allowedFiles == null) return false;
        return Arrays.stream(allowedFiles.split(","))
                .anyMatch(f -> f.trim().equalsIgnoreCase(format.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMaxFileSizeMB() {
        return getSettingAsInteger("max_file_size_mb", 50);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllowedFormats() {
        String allowedFiles = getSetting("allowed_files");
        if (allowedFiles == null || allowedFiles.trim().isEmpty()) return Arrays.asList("PDF");
        return Arrays.stream(allowedFiles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    @Override
    public int countFilesExceedingSize(int newMaxSizeMb) {
        try {
            long maxSizeBytes = (long) newMaxSizeMb * 1024 * 1024;
            // Check file sizes via DocumentVersion file paths
            List<DocumentVersion> allVersions = documentVersionRepository.findAll();
            int count = 0;
            for (DocumentVersion ver : allVersions) {
                if (ver.getFilePath() != null) {
                    try {
                        File file = new File(ver.getFilePath());
                        if (file.exists() && file.length() > maxSizeBytes) count++;
                    } catch (Exception e) {
                        logger.warn("Could not check size for: {}", ver.getFilePath());
                    }
                }
            }
            return count;
        } catch (Exception e) {
            logger.error("Error counting files exceeding size: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ==================== TAG POLICIES ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTagPolicies() {
        Map<String, Object> policies = new LinkedHashMap<>();
        policies.put("maxTagsPerDocument", getMaxTagsPerDocument());
        return policies;
    }

    @Override
    @Transactional
    public void updateTagPolicies(Integer maxTagsPerDocument, String username) throws Exception {
        if (maxTagsPerDocument == null || maxTagsPerDocument < 1 || maxTagsPerDocument > 50)
            throw new IllegalArgumentException("Maximum tags per document must be between 1 and 50");
        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);
        saveSettingValue("max_tags_per_document", String.valueOf(maxTagsPerDocument), user);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getMaxTagsPerDocument() {
        return getSettingAsInteger("max_tags_per_document", 10);
    }

    // ==================== WATERMARK ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getWatermarkSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("watermarkEnabled", isWatermarkEnabled());
        settings.put("watermarkOpacity", getWatermarkOpacity());
        settings.put("watermarkPosition", getWatermarkPosition());
        settings.put("watermarkFontSize", getWatermarkFontSize());
        return settings;
    }

    @Override
    @Transactional
    public void updateWatermarkSettings(Boolean watermarkEnabled, Integer watermarkOpacity,
                                         String watermarkPosition, Integer watermarkFontSize, String username) throws Exception {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);
        saveSettingValue("watermark_enabled", watermarkEnabled ? "1" : "0", user);
        if (watermarkOpacity != null) saveSettingValue("watermark_opacity", String.valueOf(watermarkOpacity), user);
        if (watermarkPosition != null) saveSettingValue("watermark_position", watermarkPosition, user);
        if (watermarkFontSize != null) saveSettingValue("watermark_font_size", String.valueOf(watermarkFontSize), user);
    }

    @Override
    public Boolean isWatermarkEnabled() { return getSettingAsBoolean("watermark_enabled", true); }

    @Override
    public Integer getWatermarkOpacity() { return getSettingAsInteger("watermark_opacity", 30); }

    @Override
    public String getWatermarkPosition() { String v = getSetting("watermark_position"); return v != null ? v : "Diagonal"; }

    @Override
    public Integer getWatermarkFontSize() { return getSettingAsInteger("watermark_font_size", 48); }

    // ==================== SECURITY ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSecuritySettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("sessionTimeoutHours", getSettingAsInteger("session_timeout_hours", 8));
        settings.put("enforcePasswordPolicy", getSettingAsBoolean("enforce_password_policy", false));
        settings.put("minPasswordLength", getSettingAsInteger("min_password_length", 8));
        settings.put("requireUppercase", getSettingAsBoolean("require_uppercase", true));
        settings.put("requireLowercase", getSettingAsBoolean("require_lowercase", true));
        settings.put("requireNumber", getSettingAsBoolean("require_number", true));
        settings.put("requireSpecialChar", getSettingAsBoolean("require_special_char", true));
        return settings;
    }

    @Override
    @Transactional
    public void updateSecuritySettings(Integer sessionTimeoutHours, Boolean enforcePasswordPolicy,
                                       Integer minPasswordLength, Boolean requireUppercase,
                                       Boolean requireLowercase, Boolean requireNumber,
                                       Boolean requireSpecialChar, String username) throws Exception {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);
        if (sessionTimeoutHours == null || sessionTimeoutHours < 1 || sessionTimeoutHours > 72)
            throw new IllegalArgumentException("Session timeout must be between 1 and 72 hours");
        if (minPasswordLength == null || minPasswordLength < 4 || minPasswordLength > 32)
            throw new IllegalArgumentException("Min password length must be between 4 and 32");

        saveSettingValue("session_timeout_hours", String.valueOf(sessionTimeoutHours), user);
        saveSettingValue("enforce_password_policy", enforcePasswordPolicy ? "1" : "0", user);
        saveSettingValue("min_password_length", String.valueOf(minPasswordLength), user);
        saveSettingValue("require_uppercase", requireUppercase ? "1" : "0", user);
        saveSettingValue("require_lowercase", requireLowercase ? "1" : "0", user);
        saveSettingValue("require_number", requireNumber ? "1" : "0", user);
        saveSettingValue("require_special_char", requireSpecialChar ? "1" : "0", user);
    }

    @Override
    public Boolean isPasswordPolicyEnforced() { return getSettingAsBoolean("enforce_password_policy", false); }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPasswordPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("enforcePasswordPolicy", getSettingAsBoolean("enforce_password_policy", false));
        policy.put("minPasswordLength", getSettingAsInteger("min_password_length", 8));
        policy.put("requireUppercase", getSettingAsBoolean("require_uppercase", true));
        policy.put("requireLowercase", getSettingAsBoolean("require_lowercase", true));
        policy.put("requireNumber", getSettingAsBoolean("require_number", true));
        policy.put("requireSpecialChar", getSettingAsBoolean("require_special_char", true));
        return policy;
    }

    // ==================== ACTIVITY LOGGING ====================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getActivityLoggingSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("activityLoggingEnabled", getSettingAsBoolean("activity_logging_enabled", true));
        settings.put("logRetentionDays", getSettingAsInteger("log_retention_days", 90));
        return settings;
    }

    @Override
    @Transactional
    public void updateActivityLoggingSettings(Boolean activityLoggingEnabled, Integer logRetentionDays, String username) throws Exception {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);
        if (logRetentionDays != null && (logRetentionDays < 1 || logRetentionDays > 3650))
            throw new IllegalArgumentException("Log retention must be between 1 and 3650 days");
        saveSettingValue("activity_logging_enabled", activityLoggingEnabled ? "1" : "0", user);
        if (activityLoggingEnabled && logRetentionDays != null)
            saveSettingValue("log_retention_days", String.valueOf(logRetentionDays), user);
    }

    @Override
    public Boolean isActivityLoggingEnabled() { return getSettingAsBoolean("activity_logging_enabled", true); }

    @Override
    public Integer getLogRetentionDays() { return getSettingAsInteger("log_retention_days", 90); }

    // ==================== BULK DELETE ====================

    @Override
    public boolean verifyAdminPassword(String username, String rawPassword) {
        User user = userRepository.findByUsername(username);
        if (user == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    @Transactional
    public int bulkDeleteUsers(List<Long> userIds, String adminUsername) throws Exception {
        logger.warn("BULK DELETE USERS - Admin: {}, Count: {}", adminUsername, userIds.size());
        int deleted = 0;
        for (Long userId : userIds) {
            try {
                userRepository.deleteById(userId);
                deleted++;
            } catch (Exception e) {
                logger.error("Failed to delete user ID: {} - {}", userId, e.getMessage());
            }
        }
        return deleted;
    }

    @Override
    @Transactional
    public int bulkDeleteDocuments(List<Long> documentIds, String adminUsername) throws Exception {
        logger.warn("BULK DELETE DOCUMENTS - Admin: {}, Count: {}", adminUsername, documentIds.size());
        int deleted = 0;
        for (Long docId : documentIds) {
            try {
                documentService.deleteDocument(docId);
                deleted++;
            } catch (Exception e) {
                logger.error("Failed to delete document ID: {} - {}", docId, e.getMessage());
            }
        }
        return deleted;
    }

    @Override
    public List<Map<String, Object>> getAllUsersForBulkDelete(String currentUsername) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equals(currentUsername))
                .map(u -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", u.getUserId());
                    map.put("username", u.getUsername());
                    map.put("email", u.getEmail());
                    map.put("roleName", u.getRole() != null ? u.getRole().getRoleName() : "Unknown");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAllDocumentsForBulkDelete() {
        return documentRepository.findAll().stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", d.getDocumentId());
                    map.put("title", d.getTitle());
                    map.put("docTypeName", d.getDocumentType() != null ? d.getDocumentType().getDocTypeName() : "Unknown");
                    map.put("fileExtension", d.getFileExtension());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ==================== PRIVATE HELPERS ====================

    private void saveSettingValue(String settingName, String value, User user) {
        ApplicationSetting setting = settingsRepository
                .findBySettingName(settingName)
                .orElseThrow(() -> new RuntimeException("Setting '" + settingName + "' not found in DB"));
        setting.setSettingValue(value);
        setting.setUpdatedBy(user);
        settingsRepository.save(setting);
    }

    public Integer getSettingAsInteger(String settingName, Integer defaultValue) {
        try {
            String value = getSetting(settingName);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean getSettingAsBoolean(String settingName, Boolean defaultValue) {
        try {
            String value = getSetting(settingName);
            if (value == null) return defaultValue;
            return "1".equals(value) || "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
