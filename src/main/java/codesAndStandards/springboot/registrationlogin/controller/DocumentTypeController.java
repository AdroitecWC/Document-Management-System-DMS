package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDetailDto;
import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDto;
import codesAndStandards.springboot.registrationlogin.service.DocTypeLifecycleService;
import codesAndStandards.springboot.registrationlogin.service.DocumentTypeService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import codesAndStandards.springboot.registrationlogin.service.ActivityLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-types")
@RequiredArgsConstructor
@Slf4j
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;
    private final LicenseService licenseService;
    private final ActivityLogService activityLogService;
    private final DocTypeLifecycleService lifecycleService;

    /**
     * Get all document types
     * Used by upload form dropdowns and document library filters
     * READ-ONLY — all editions
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllDocumentTypes() {
        try {
            List<DocumentTypeDto> types = documentTypeService.getAllDocumentTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error fetching document types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch document types"));
        }
    }

    /**
     * Get all document type names as simple string list
     * Used by bulk upload dropdowns
     */
    @GetMapping("/names")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getAllDocumentTypeNames() {
        try {
            List<DocumentTypeDto> types = documentTypeService.getAllDocumentTypes();
            List<String> names = types.stream()
                    .map(DocumentTypeDto::getDocTypeName)
                    .filter(n -> n != null && !n.trim().isEmpty())
                    .sorted()
                    .toList();
            return ResponseEntity.ok(names);
        } catch (Exception e) {
            log.error("Error fetching document type names", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Get document type by ID (with metadata definitions)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDocumentTypeById(@PathVariable Long id) {
        try {
            DocumentTypeDto type = documentTypeService.getDocumentTypeById(id);
            return ResponseEntity.ok(type);
        } catch (RuntimeException e) {
            log.error("Document type not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create new document type — Admin only, ED2 only
     */
    @PostMapping
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_CREATE')") // Using METADATA_UPDATE for managing structure
    public ResponseEntity<?> createDocumentType(@RequestBody DocumentTypeDto dto) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }
        if (!"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Document type management requires Professional Edition (ED2)"));
        }

        try {
            if (dto.getDocTypeName() == null || dto.getDocTypeName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Document type name is required"));
            }

            DocumentTypeDto created = documentTypeService.createDocumentType(dto);
            log.info("Document type created: {}", created.getDocTypeName());
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_CREATE,
                    "Created document type: '" + created.getDocTypeName() + "'"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException e) {
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_CREATE_FAIL,
                    "Failed to create document type: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating document type", e);
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_CREATE_FAIL,
                    "Failed to create document type: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create document type: " + e.getMessage()));
        }
    }

    /**
     * Update document type — Admin only, ED2 only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_UPDATE')")
    public ResponseEntity<?> updateDocumentType(@PathVariable Long id, @RequestBody DocumentTypeDto dto) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }
        if (!"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Document type management requires Professional Edition (ED2)"));
        }

        try {
            DocumentTypeDto updated = documentTypeService.updateDocumentType(id, dto);
            log.info("Document type updated: {}", updated.getDocTypeName());
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_UPDATE,
                    "Updated document type: '" + updated.getDocTypeName() + "' (ID: " + id + ")"
            );
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_UPDATE_FAIL,
                    "Failed to update document type ID " + id + ": " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating document type", e);
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_UPDATE_FAIL,
                    "Failed to update document type ID " + id + ": " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update document type: " + e.getMessage()));
        }
    }

    /**
     * Delete document type — Admin only, ED2 only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_DELETE')")
    public ResponseEntity<?> deleteDocumentType(@PathVariable Long id) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }
        if (!"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Document type management requires Professional Edition (ED2)"));
        }

        try {
            documentTypeService.deleteDocumentType(id);
            log.info("Document type deleted: {}", id);
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_DELETE,
                    "Deleted document type ID: " + id
            );
            return ResponseEntity.ok(Map.of("message", "Document type deleted successfully"));

        } catch (IllegalStateException e) {
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_DELETE_FAIL,
                    "Failed to delete document type ID " + id + ": " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting document type", e);
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.DOCTYPE_DELETE_FAIL,
                    "Failed to delete document type ID " + id + ": " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete document type: " + e.getMessage()));
        }
    }


    @GetMapping("/reference/people")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvailablePeople() {
        try { return ResponseEntity.ok(lifecycleService.getAvailablePeople()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }


    // ── New endpoints ────────────────────────────────────────────────

    @GetMapping("/full-list")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('SETTINGS_VIEW')")
    public ResponseEntity<?> getAllWithLifecycle() {
        try { return ResponseEntity.ok(lifecycleService.getAllSummaries()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{id}/full")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('SETTINGS_VIEW')")
    public ResponseEntity<?> getFullDetail(@PathVariable Long id) {
        try { return ResponseEntity.ok(lifecycleService.getFullDetail(id)); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}/full")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_UPDATE')")
    public ResponseEntity<?> saveFullDetail(@PathVariable Long id,
                                            @RequestBody DocumentTypeDetailDto dto, Authentication authentication) {
        if (!licenseService.isLicenseValid())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid license"));
        try {
            DocumentTypeDetailDto result = lifecycleService.saveFullDetail(id, dto, authentication.getName());
            activityLogService.logByUsername(authentication.getName(), ActivityLogService.DOCTYPE_UPDATE,
                    "Updated document type lifecycle data: '" + result.getDocTypeName() + "'");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/reference/states")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvailableStates() {
        try { return ResponseEntity.ok(lifecycleService.getAvailableStates()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/reference/groups")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAvailableGroups() {
        try { return ResponseEntity.ok(lifecycleService.getAvailableGroups()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{id}/team-members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long id) {
        try { return ResponseEntity.ok(lifecycleService.getTeamMembers(id)); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage())); }
    }

}
