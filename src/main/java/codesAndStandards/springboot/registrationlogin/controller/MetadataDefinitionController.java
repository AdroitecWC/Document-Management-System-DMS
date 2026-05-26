package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeMetadataDto;
import codesAndStandards.springboot.registrationlogin.dto.MetadataDefinitionDto;
import codesAndStandards.springboot.registrationlogin.entity.DocumentTypeMetadata;
import codesAndStandards.springboot.registrationlogin.entity.DocumentType;
import codesAndStandards.springboot.registrationlogin.entity.MetadataDefinition;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeMetadataRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeRepository;
import codesAndStandards.springboot.registrationlogin.repository.MetadataDefinitionRepository;
import codesAndStandards.springboot.registrationlogin.service.ActivityLogService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import codesAndStandards.springboot.registrationlogin.service.MetadataDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata-definitions")
@RequiredArgsConstructor
@Slf4j
public class MetadataDefinitionController {

    private final MetadataDefinitionService metadataDefinitionService;
    private final DocumentTypeMetadataRepository documentTypeMetadataRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final MetadataDefinitionRepository metadataDefinitionRepository;
    private final LicenseService licenseService;
    private final ActivityLogService activityLogService;

    /**
     * Get all metadata definitions
     * READ-ONLY — all editions
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllMetadataDefinitions() {
        try {
            List<MetadataDefinitionDto> definitions = metadataDefinitionService.getAllMetadataDefinitions();
            return ResponseEntity.ok(definitions);
        } catch (Exception e) {
            log.error("Error fetching metadata definitions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch metadata definitions"));
        }
    }

    /**
     * Get metadata definition by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            MetadataDefinitionDto dto = metadataDefinitionService.getById(id);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get metadata definitions for a specific document type
     * Returns definitions with mandatory flag from the join table
     */
    @GetMapping("/by-doc-type/{docTypeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByDocumentType(@PathVariable Long docTypeId) {
        try {
            List<MetadataDefinitionDto> definitions = metadataDefinitionService.getByDocumentTypeId(docTypeId);
            return ResponseEntity.ok(definitions);
        } catch (Exception e) {
            log.error("Error fetching metadata for doc type {}", docTypeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch metadata definitions"));
        }
    }

    /**
     * Get mandatory metadata definitions for a specific document type
     */
    @GetMapping("/mandatory/{docTypeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMandatoryByDocumentType(@PathVariable Long docTypeId) {
        try {
            List<MetadataDefinitionDto> definitions = metadataDefinitionService.getMandatoryByDocumentTypeId(docTypeId);
            return ResponseEntity.ok(definitions);
        } catch (Exception e) {
            log.error("Error fetching mandatory metadata for doc type {}", docTypeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch mandatory metadata definitions"));
        }
    }

    /**
     * Create new metadata definition — Admin only, ED2 only
     */
    @PostMapping
    @PreAuthorize("hasRole('superadmin') or hasAuthority('METADATA_CREATE')")
    public ResponseEntity<?> create(@RequestBody MetadataDefinitionDto dto) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }
        if (!"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Metadata management requires Professional Edition (ED2)"));
        }

        try {
            if (dto.getFieldName() == null || dto.getFieldName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Field name is required"));
            }
            if (dto.getFieldType() == null || dto.getFieldType().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Field type is required"));
            }

            MetadataDefinitionDto created = metadataDefinitionService.create(dto);
            log.info("Metadata definition created: {}", created.getFieldName());

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_CREATE,
                    "Created metadata field: '" + created.getFieldName() +
                            "' (Type: " + created.getFieldType() + ")"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(created);

        } catch (IllegalArgumentException e) {

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_CREATE_FAIL,
                    "Failed to create metadata field: " + e.getMessage()
            );

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_CREATE_FAIL,
                    "Failed to create metadata field: " + e.getMessage()
            );

            log.error("Error creating metadata definition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create metadata definition: " + e.getMessage()));
        }
    }

    /**
     * Update metadata definition — Admin only, ED2 only
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('METADATA_UPDATE')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MetadataDefinitionDto dto) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }
        if (!"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Metadata management requires Professional Edition (ED2)"));
        }

        try {
            MetadataDefinitionDto updated = metadataDefinitionService.update(id, dto);
            log.info("Metadata definition updated: {}", updated.getFieldName());

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_UPDATE,
                    "Updated metadata field: '" + updated.getFieldName()+"'"
            );

            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_UPDATE_FAIL,
                    "Failed to update metadata field ID " + id + ": " + e.getMessage()
            );

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_UPDATE_FAIL,
                    "Failed to update metadata field ID " + id + ": " + e.getMessage()
            );

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating metadata definition", e);
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_UPDATE_FAIL,
                    "Failed to update metadata field ID " + id + ": " + e.getMessage()
            );

            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete metadata definition — Admin only, ED2 only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('METADATA_DELETE')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!licenseService.isLicenseValid()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "License expired or not found"));
        }
        if (!"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Metadata management requires Professional Edition (ED2)"));
        }

        try {
            // Fetch name BEFORE deleting
            MetadataDefinitionDto existing = metadataDefinitionService.getById(id);
            String fieldName = (existing != null && existing.getFieldName() != null)
                    ? existing.getFieldName()
                    : "ID: " + id;

            metadataDefinitionService.delete(id);
            log.info("Metadata definition deleted: {}", fieldName);

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_DELETE,
                    "Deleted metadata field: '" + fieldName + "'"
            );

            return ResponseEntity.ok(Map.of("message", "Metadata definition deleted successfully"));

        } catch (RuntimeException e) {

            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_DELETE_FAIL,
                    "Failed to delete metadata field ID " + id + ": " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting metadata definition", e);
            activityLogService.logByUsername(
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    ActivityLogService.METADATA_DELETE_FAIL,
                    "Failed to delete metadata field ID " + id + ": " + e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Link a metadata definition to a document type (with mandatory flag)
     * POST /api/metadata-definitions/link
     * Body: { "docTypeId": 1, "metadataId": 2, "mandatory": true }
     */
    @PostMapping("/link")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('METADATA_LINK')")
    public ResponseEntity<?> linkToDocumentType(@RequestBody DocumentTypeMetadataDto dto) {
        if (!licenseService.isLicenseValid() || !"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires valid ED2 license"));
        }

        try {
            if (documentTypeMetadataRepository.existsByDocTypeIdAndMetadataId(dto.getDocTypeId(), dto.getMetadataId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "This metadata field is already linked to this document type"));
            }

            DocumentType docType = documentTypeRepository.findById(dto.getDocTypeId())
                    .orElseThrow(() -> new RuntimeException("Document type not found"));
            MetadataDefinition metadata = metadataDefinitionRepository.findById(dto.getMetadataId())
                    .orElseThrow(() -> new RuntimeException("Metadata definition not found"));

            DocumentTypeMetadata.DocumentTypeMetadataId id =
                    new DocumentTypeMetadata.DocumentTypeMetadataId(dto.getDocTypeId(), dto.getMetadataId());

            DocumentTypeMetadata mapping = DocumentTypeMetadata.builder()
                    .id(id)
                    .documentType(docType)
                    .metadataDefinition(metadata)
                    .mandatory(dto.getMandatory() != null ? dto.getMandatory() : false)
                    .build();

            documentTypeMetadataRepository.save(mapping);

            log.info("Linked metadata '{}' to doc type '{}' (mandatory: {})",
                    metadata.getFieldName(), docType.getDocTypeName(), mapping.getMandatory());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Metadata linked to document type successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error linking metadata to document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to link: " + e.getMessage()));
        }
    }

    /**
     * Unlink a metadata definition from a document type
     * DELETE /api/metadata-definitions/unlink?docTypeId=1&metadataId=2
     */
    @DeleteMapping("/unlink")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('METADATA_LINK')")
    public ResponseEntity<?> unlinkFromDocumentType(
            @RequestParam Long docTypeId,
            @RequestParam Long metadataId) {

        if (!licenseService.isLicenseValid() || !"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires valid ED2 license"));
        }

        try {
            if (!documentTypeMetadataRepository.existsByDocTypeIdAndMetadataId(docTypeId, metadataId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "This link does not exist"));
            }

            DocumentTypeMetadata.DocumentTypeMetadataId id =
                    new DocumentTypeMetadata.DocumentTypeMetadataId(docTypeId, metadataId);
            documentTypeMetadataRepository.deleteById(id);

            log.info("Unlinked metadata {} from doc type {}", metadataId, docTypeId);
            return ResponseEntity.ok(Map.of("message", "Metadata unlinked from document type successfully"));

        } catch (Exception e) {
            log.error("Error unlinking metadata from document type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unlink: " + e.getMessage()));
        }
    }
}
