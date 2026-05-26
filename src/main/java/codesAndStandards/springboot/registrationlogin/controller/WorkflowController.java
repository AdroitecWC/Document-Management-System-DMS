package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.WorkflowDto;
import codesAndStandards.springboot.registrationlogin.service.LicenseService;
import codesAndStandards.springboot.registrationlogin.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowService  workflowService;
    private final LicenseService   licenseService;

    @GetMapping
    @PreAuthorize("hasRole('superadmin') or hasAuthority('SETTINGS_VIEW')")
    public ResponseEntity<?> getAll() {
        try { return ResponseEntity.ok(workflowService.getAll()); }
        catch (Exception e) { return err(e); }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try { return ResponseEntity.ok(workflowService.getById(id)); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return err(e); }
    }

    /** Used by upload page Step 1 */
    @GetMapping("/by-doc-type/{docTypeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByDocType(@PathVariable Long docTypeId) {
        try { return ResponseEntity.ok(workflowService.getByDocType(docTypeId)); }
        catch (Exception e) { return err(e); }
    }

    @PostMapping
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_CREATE')")
    public ResponseEntity<?> create(@RequestBody WorkflowDto dto, Authentication auth) {
        if (!licenseService.isLicenseValid())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid license"));
        try { return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(dto, auth.getName())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return err(e); }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_UPDATE')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody WorkflowDto dto, Authentication auth) {
        if (!licenseService.isLicenseValid())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid license"));
        try { return ResponseEntity.ok(workflowService.update(id, dto, auth.getName())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return err(e); }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('superadmin') or hasAuthority('DOCUMENT_TYPE_DELETE')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!licenseService.isLicenseValid())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid license"));
        try { workflowService.delete(id); return ResponseEntity.ok(Map.of("success", true)); }
        catch (IllegalArgumentException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage())); }
        catch (Exception e) { return err(e); }
    }

    private ResponseEntity<?> err(Exception e) {
        log.error("Workflow error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
    }
}