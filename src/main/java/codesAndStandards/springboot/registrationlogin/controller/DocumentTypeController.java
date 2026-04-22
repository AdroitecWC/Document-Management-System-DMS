package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDto;
import codesAndStandards.springboot.registrationlogin.service.DocumentTypeService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Get all document types
     * Used by upload form dropdowns and document library filters
     * READ-ONLY — all editions
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_TYPE_VIEW')")
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
    @PreAuthorize("hasAuthority('DOCUMENT_TYPE_VIEW')")
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
    @PreAuthorize("hasAuthority('DOCUMENT_TYPE_VIEW')")
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
    @PreAuthorize("hasAuthority('METADATA_UPDATE')") // Using METADATA_UPDATE for managing structure
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
            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create document type: " + e.getMessage()));
        }
    }

    /**
     * Update document type — Admin only, ED2 only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('METADATA_UPDATE')")
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
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update document type: " + e.getMessage()));
        }
    }

    /**
     * Delete document type — Admin only, ED2 only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('METADATA_UPDATE')")
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
            return ResponseEntity.ok(Map.of("message", "Document type deleted successfully"));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete document type: " + e.getMessage()));
        }
    }
}
