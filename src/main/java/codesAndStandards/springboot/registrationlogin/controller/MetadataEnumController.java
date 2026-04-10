package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.MetadataEnumDto;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import codesAndStandards.springboot.registrationlogin.service.MetadataEnumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata-enums")
@RequiredArgsConstructor
@Slf4j
public class MetadataEnumController {

    private final MetadataEnumService metadataEnumService;
    private final LicenseService licenseService;

    /**
     * Get all enum values for a metadata definition
     * GET /api/metadata-enums/by-metadata/{metadataId}
     */
    @GetMapping("/by-metadata/{metadataId}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager', 'Viewer')")
    public ResponseEntity<?> getEnumValues(@PathVariable Long metadataId) {
        try {
            List<MetadataEnumDto> values = metadataEnumService.getEnumValuesByMetadataId(metadataId);
            return ResponseEntity.ok(values);
        } catch (Exception e) {
            log.error("Error fetching enum values for metadata {}", metadataId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch enum values"));
        }
    }

    /**
     * Get a single enum value by ID
     * GET /api/metadata-enums/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Admin', 'Manager')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(metadataEnumService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Add an enum value — Admin only, ED2 only
     * POST /api/metadata-enums
     */
    @PostMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> addEnumValue(@RequestBody MetadataEnumDto dto) {
        if (!licenseService.isLicenseValid() || !"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires valid ED2 license"));
        }

        try {
            MetadataEnumDto created = metadataEnumService.addEnumValue(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding enum value", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add enum value: " + e.getMessage()));
        }
    }

    /**
     * Update an enum value — Admin only, ED2 only
     * PUT /api/metadata-enums/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> updateEnumValue(@PathVariable Long id, @RequestBody MetadataEnumDto dto) {
        if (!licenseService.isLicenseValid() || !"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires valid ED2 license"));
        }

        try {
            MetadataEnumDto updated = metadataEnumService.updateEnumValue(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating enum value", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update enum value: " + e.getMessage()));
        }
    }

    /**
     * Delete an enum value — Admin only, ED2 only
     * DELETE /api/metadata-enums/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<?> deleteEnumValue(@PathVariable Long id) {
        if (!licenseService.isLicenseValid() || !"ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Requires valid ED2 license"));
        }

        try {
            metadataEnumService.deleteEnumValue(id);
            return ResponseEntity.ok(Map.of("message", "Enum value deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting enum value", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete enum value: " + e.getMessage()));
        }
    }
}
