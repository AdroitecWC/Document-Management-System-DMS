package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentVersionDto;
import codesAndStandards.springboot.registrationlogin.service.DocumentVersionService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import codesAndStandards.springboot.registrationlogin.service.NetworkFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-versions")
@RequiredArgsConstructor
@Slf4j
public class DocumentVersionController {

    private final DocumentVersionService documentVersionService;
    private final NetworkFileService networkFileService;
    private final LicenseService licenseService;

    /**
     * Get all versions for a document
     * GET /api/document-versions/by-document/{documentId}
     */
    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> getVersionsByDocument(@PathVariable Long documentId) {
        try {
            List<DocumentVersionDto> versions = documentVersionService.getVersionsByDocumentId(documentId);
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            log.error("Error fetching versions for document {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch versions"));
        }
    }

    /**
     * Get latest version for a document
     * GET /api/document-versions/latest/{documentId}
     */
    @GetMapping("/latest/{documentId}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> getLatestVersion(@PathVariable Long documentId) {
        try {
            DocumentVersionDto version = documentVersionService.getLatestVersion(documentId);
            return ResponseEntity.ok(version);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get a specific version by ID
     * GET /api/document-versions/{versionId}
     */
    @GetMapping("/{versionId}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> getVersionById(@PathVariable Long versionId) {
        try {
            DocumentVersionDto version = documentVersionService.getVersionById(versionId);
            return ResponseEntity.ok(version);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Download a specific version's file
     * GET /api/document-versions/{versionId}/download
     */
    @GetMapping("/{versionId}/download")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager')")
    public ResponseEntity<?> downloadVersion(@PathVariable Long versionId) {
        try {
            if (!licenseService.isLicenseValid()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "License expired or not found"));
            }

            DocumentVersionDto version = documentVersionService.getVersionById(versionId);
            String filePath = version.getFilePath();

            byte[] fileBytes = networkFileService.readFileFromNetworkShare(filePath);

            // Extract filename from path
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            if (fileName.contains("\\")) {
                fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            }

            log.info("Downloading version {} (file: {}, size: {} bytes)", versionId, fileName, fileBytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileBytes.length))
                    .body(fileBytes);

        } catch (RuntimeException e) {
            log.error("Version not found: {}", versionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error downloading version {}", versionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download version: " + e.getMessage()));
        }
    }

    /**
     * View a specific version's file inline (for viewer)
     * GET /api/document-versions/{versionId}/view
     */
    @GetMapping("/{versionId}/view")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> viewVersion(@PathVariable Long versionId) {
        try {
            DocumentVersionDto version = documentVersionService.getVersionById(versionId);
            String filePath = version.getFilePath();

            byte[] fileBytes = networkFileService.readFileFromNetworkShare(filePath);

            // Detect content type from file extension
            String contentType = detectContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileBytes.length))
                    .body(fileBytes);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error viewing version {}", versionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Error loading version: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Delete a specific version — Admin only
     * DELETE /api/document-versions/{versionId}
     */
    @DeleteMapping("/{versionId}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> deleteVersion(@PathVariable Long versionId) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }

        try {
            // Prevent deleting the only version
            DocumentVersionDto version = documentVersionService.getVersionById(versionId);
            List<DocumentVersionDto> allVersions = documentVersionService.getVersionsByDocumentId(version.getDocumentId());

            if (allVersions.size() <= 1) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Cannot delete the only version of a document. Delete the document instead."));
            }

            documentVersionService.deleteVersion(versionId);
            log.info("Version {} deleted", versionId);
            return ResponseEntity.ok(Map.of("message", "Version deleted successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting version {}", versionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete version: " + e.getMessage()));
        }
    }

    private String detectContentType(String filePath) {
        if (filePath == null) return "application/octet-stream";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".odt")) return "application/vnd.oasis.opendocument.text";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".rtf")) return "application/rtf";
        return "application/octet-stream";
    }
}
