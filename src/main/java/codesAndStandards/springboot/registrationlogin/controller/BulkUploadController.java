package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.BulkUploadResult;
import codesAndStandards.springboot.registrationlogin.dto.BulkUploadValidationResult;
import codesAndStandards.springboot.registrationlogin.service.ActivityLogService;
import codesAndStandards.springboot.registrationlogin.service.BulkUploadService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/bulk-upload")
public class BulkUploadController {

    private static final Logger logger = LoggerFactory.getLogger(BulkUploadController.class);

    @Autowired
    private BulkUploadService bulkUploadService;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private LicenseService licenseService;

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Helper method to check license edition and return error response if needed
     */
    private ResponseEntity<?> checkLicenseEdition() {
        // Check if license is valid
        if (!licenseService.isLicenseValid()) {
            logger.warn("License validation failed - license invalid or expired");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "License expired or not found");
            error.put("code", "LICENSE_INVALID");
            error.put("message", "Please activate or renew your license");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        // Check if bulk upload is allowed (ED2 edition only)
        if (!licenseService.isBulkUploadAllowed()) {
            logger.warn("Bulk upload denied - Current edition: {}", licenseService.getCurrentEdition());

            String currentEdition = licenseService.getCurrentEdition();
            long daysRemaining = licenseService.getDaysRemaining();

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bulk upload feature not available in your edition");
            error.put("code", "EDITION_UPGRADE_REQUIRED");
            error.put("currentEdition", currentEdition != null ? currentEdition : "ED1");
            error.put("requiredEdition", "ED2");
            error.put("daysRemaining", daysRemaining);
            error.put("message", "Please upgrade to ED2 Professional edition to use bulk upload feature. Contact your administrator.");

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        return null; // License is valid and edition allows bulk upload
    }

    /**
     * Generate Excel template from uploaded PDF files.
     * Now accepts a list of filenames and their corresponding page counts.
     */
    @PostMapping("/generate-template")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> generateTemplate(
            @RequestParam(value = "filenames", required = false) List<String> filenames,
            @RequestParam(value = "pageCounts", required = false) List<String> pageCounts) {

        // ✅ CHECK LICENSE EDITION FIRST
        ResponseEntity<?> licenseCheck = checkLicenseEdition();
        if (licenseCheck != null) {
            return licenseCheck; // Return error response
        }

        try {
            logger.info("✅ ED2 License validated - Generating Excel template for {} PDF files",
                    filenames != null ? filenames.size() : 0);

            if (filenames == null || filenames.isEmpty()) {
                logger.warn("No filenames provided for template generation");
                return ResponseEntity.badRequest().build();
            }

            // Pass both filenames and pageCounts to the service
            ByteArrayOutputStream outputStream = bulkUploadService.generateExcelTemplate(filenames, pageCounts);

            if (outputStream == null || outputStream.size() == 0) {
                logger.error("Generated Excel template is empty");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "bulk_upload_template_" + timestamp + ".xlsx";

            logger.info("Excel template generated successfully: {} ({} bytes)", filename, resource.contentLength());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error generating Excel template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to generate template: " + e.getMessage()));
        }
    }

    /**
     * Download blank Excel template (without any filenames)
     * ✅ WITH LICENSE AND EDITION VALIDATION
     */
    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> downloadBlankTemplate() {

        // ✅ CHECK LICENSE EDITION FIRST
        ResponseEntity<?> licenseCheck = checkLicenseEdition();
        if (licenseCheck != null) {
            return licenseCheck; // Return error response
        }

        try {
            logger.info("✅ ED2 License validated - Downloading blank Excel template");

            // Create empty template (no filenames or page counts)
            ByteArrayOutputStream outputStream = bulkUploadService.generateExcelTemplate(null, null);

            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            logger.info("Blank template generated successfully ({} bytes)", resource.contentLength());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bulk_upload_template.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error downloading blank template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to download blank template: " + e.getMessage()));
        }
    }

    /**
     * Validate bulk upload files
     * ✅ WITH LICENSE AND EDITION VALIDATION
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('Admin')")
    @ResponseBody
    public ResponseEntity<?> validateBulkUpload(
            @RequestParam(value = "pdfFiles", required = false) MultipartFile[] pdfFiles,
            @RequestParam(value = "pdfFilenames", required = false) List<String> pdfFilenames,
            @RequestParam(value = "excelFile", required = false) MultipartFile excelFile,
            @RequestParam(value = "selfValidationJson", required = false) String selfValidationJson) {

        // ✅ CHECK LICENSE EDITION FIRST
        ResponseEntity<?> licenseCheck = checkLicenseEdition();
        if (licenseCheck != null) {
            return licenseCheck; // Return error response
        }

        try {
            logger.info("✅ ED2 License validated - Validating bulk upload: {} PDF files, {} Filenames, Excel file: {}, Has edited metadata: {}",
                    pdfFiles != null ? pdfFiles.length : 0,
                    pdfFilenames != null ? pdfFilenames.size() : 0,
                    excelFile != null ? excelFile.getOriginalFilename() : "null",
                    selfValidationJson != null && !selfValidationJson.isEmpty() ? "YES" : "NO");

            boolean hasPdfContent = (pdfFiles != null && pdfFiles.length > 0) || (pdfFilenames != null && !pdfFilenames.isEmpty());
            if (!hasPdfContent) {
                logger.warn("No PDF files or filenames provided for validation");
                BulkUploadValidationResult errorResult = new BulkUploadValidationResult();
                errorResult.addError("Validation Error", "No PDF files provided");
                return ResponseEntity.badRequest().body(errorResult);
            }

            if ((excelFile == null || excelFile.isEmpty()) && (selfValidationJson == null || selfValidationJson.isEmpty())) {
                logger.warn("No Excel file or metadata provided for validation");
                BulkUploadValidationResult errorResult = new BulkUploadValidationResult();
                errorResult.addError("Validation Error", "No Excel file provided");
                return ResponseEntity.badRequest().body(errorResult);
            }

            // Pass the edited metadata to the service layer
            BulkUploadValidationResult result = bulkUploadService.validateBulkUpload(
                    pdfFiles,
                    pdfFilenames,
                    excelFile,
                    selfValidationJson
            );

            logger.info("Validation complete: Total={}, Valid={}, Errors={}, Warnings={}",
                    result.getTotalDocuments(),
                    result.getValidDocuments(),
                    result.getErrors() != null ? result.getErrors().size() : 0,
                    result.getWarnings() != null ? result.getWarnings().size() : 0);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error validating bulk upload", e);
            BulkUploadValidationResult errorResult = new BulkUploadValidationResult();
            errorResult.addError("Validation Error", "Failed to validate files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    /**
     * Process bulk upload
     * ✅ WITH LICENSE AND EDITION VALIDATION
     */
    @PostMapping("/process")
    @PreAuthorize("hasAuthority('Admin')")
    @ResponseBody
    public ResponseEntity<?> processBulkUpload(
            @RequestParam(value = "pdfFiles", required = false) MultipartFile[] pdfFiles,
            @RequestParam(value = "excelFile", required = false) MultipartFile excelFile,
            @RequestParam(value = "selfValidationJson", required = false) String selfValidationJson,
            @RequestParam(value = "uploadOnlyValid", required = false) String uploadOnlyValid) {

        // ✅ CHECK LICENSE EDITION FIRST
        ResponseEntity<?> licenseCheck = checkLicenseEdition();
        if (licenseCheck != null) {
            return licenseCheck; // Return error response
        }

        String username = getCurrentUsername();

        try {
            boolean onlyValid = "true".equalsIgnoreCase(uploadOnlyValid);

            logger.info("✅ ED2 License validated - Processing bulk upload: {} PDF files, Excel file: {}, Has edited metadata: {}, Upload only valid: {}",
                    pdfFiles != null ? pdfFiles.length : 0,
                    excelFile != null ? excelFile.getOriginalFilename() : "null",
                    selfValidationJson != null && !selfValidationJson.isEmpty() ? "YES" : "NO",
                    onlyValid);

            if (pdfFiles == null || pdfFiles.length == 0) {
                logger.warn("No PDF files provided for processing");
                BulkUploadResult errorResult = new BulkUploadResult();
                errorResult.addError("No PDF files provided");
                return ResponseEntity.badRequest().body(errorResult);
            }

            if ((excelFile == null || excelFile.isEmpty()) && (selfValidationJson == null || selfValidationJson.isEmpty())) {
                logger.warn("No Excel file or metadata provided for processing");
                BulkUploadResult errorResult = new BulkUploadResult();
                errorResult.addError("No Excel file or metadata provided");
                return ResponseEntity.badRequest().body(errorResult);
            }

            long startTime = System.currentTimeMillis();

            // Pass the edited metadata to the service layer
            BulkUploadResult result = bulkUploadService.processBulkUpload(
                    pdfFiles,
                    excelFile,
                    selfValidationJson,
                    onlyValid
            );

            long duration = System.currentTimeMillis() - startTime;

            logger.info("Bulk upload processing completed in {} ms. Success: {}, Failed: {}",
                    duration,
                    result.getSuccessCount(),
                    result.getFailedCount());

       
            // ✅ LOG INDIVIDUAL DOCUMENT UPLOADS
            if (result.getSuccessfulUploads() != null && !result.getSuccessfulUploads().isEmpty()) {
                for (Map.Entry<String, String> entry : result.getSuccessfulUploads().entrySet()) {

                    String filename = entry.getKey();
                    String title = entry.getValue();

                    activityLogService.logByUsername(
                            username,
                            ActivityLogService.DOCUMENT_UPLOAD,
                            "Uploaded document: \"" + filename + "\" as \"" + title + "\" " +
                                    "(Bulk Upload" +
                                    (selfValidationJson != null && !selfValidationJson.isEmpty()
                                            ? ", edited metadata"
                                            : "") +
                                    ")"
                    );
                }
            }

            // Summary Log
            activityLogService.logByUsername(
                    username,
                    ActivityLogService.BULK_DOCUMENT_UPLOADED,
                    "Bulk upload processed: " + result.getSuccessCount() + " success, " + result.getFailedCount() + " failed"
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error processing bulk upload", e);

            // Log failure
            activityLogService.logByUsername(
                    username,
                    ActivityLogService.BULK_DOCUMENT_UPLOAD_FAIL,
                    "Bulk upload failed (Reason: " + e.getMessage() + ")"
            );

            BulkUploadResult errorResult = new BulkUploadResult();
            errorResult.addError("Upload Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
}