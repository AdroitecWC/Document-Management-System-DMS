package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentMetadataDto;
import codesAndStandards.springboot.registrationlogin.service.DocumentMetadataService;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-metadata")
@RequiredArgsConstructor
@Slf4j
public class DocumentMetadataController {

    private final DocumentMetadataService documentMetadataService;
    private final LicenseService licenseService;

    /**
     * Get all metadata values for a document
     * GET /api/document-metadata/by-document/{documentId}
     */
    @GetMapping("/by-document/{documentId}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> getByDocument(@PathVariable Long documentId) {
        try {
            List<DocumentMetadataDto> metadata = documentMetadataService.getMetadataByDocumentId(documentId);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("Error fetching metadata for document {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch document metadata"));
        }
    }

    /**
     * Get a single metadata entry by ID
     * GET /api/document-metadata/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(documentMetadataService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Save or update a single metadata value for a document
     * POST /api/document-metadata
     * Body: { "documentId": 1, "metadataId": 2, "value": "some value" }
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager')")
    public ResponseEntity<?> saveOrUpdate(@RequestBody DocumentMetadataDto dto) {
        try {
            DocumentMetadataDto saved = documentMetadataService.saveOrUpdate(dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving document metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save metadata: " + e.getMessage()));
        }
    }

    /**
     * Save multiple metadata values for a document at once
     * POST /api/document-metadata/bulk/{documentId}
     * Body: [{ "metadataId": 1, "value": "val1" }, { "metadataId": 2, "value": "val2" }]
     */
    @PostMapping("/bulk/{documentId}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager')")
    public ResponseEntity<?> saveBulk(@PathVariable Long documentId, @RequestBody List<DocumentMetadataDto> metadataList) {
        try {
            List<DocumentMetadataDto> saved = documentMetadataService.saveAll(documentId, metadataList);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error bulk saving document metadata for document {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save metadata: " + e.getMessage()));
        }
    }

    /**
     * Delete a specific metadata entry
     * DELETE /api/document-metadata/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            documentMetadataService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Metadata entry deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting document metadata {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete: " + e.getMessage()));
        }
    }

    /**
     * Delete all metadata for a document
     * DELETE /api/document-metadata/by-document/{documentId}
     */
    @DeleteMapping("/by-document/{documentId}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> deleteAllByDocument(@PathVariable Long documentId) {
        try {
            documentMetadataService.deleteAllByDocumentId(documentId);
            return ResponseEntity.ok(Map.of("message", "All metadata deleted for document " + documentId));
        } catch (Exception e) {
            log.error("Error deleting all metadata for document {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete: " + e.getMessage()));
        }
    }
}
