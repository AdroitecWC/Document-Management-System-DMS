package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentDto;
import codesAndStandards.springboot.registrationlogin.service.DocumentService;
import codesAndStandards.springboot.registrationlogin.service.NetworkFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Converts DOC / DOCX / ODT / RTF to PDF using LibreOffice headless,
 * then streams the resulting PDF back to the browser for PDF.js rendering.
 *
 * This approach preserves images, charts, fonts, and all formatting perfectly.
 */
@RestController
public class PdfConvertController {

    private static final Logger logger = LoggerFactory.getLogger(PdfConvertController.class);

    @Autowired private DocumentService    documentService;
    @Autowired private NetworkFileService networkFileService;

    @PreAuthorize("hasAnyAuthority('Manager', 'Admin', 'Viewer')")
    @GetMapping("/documents/pdf-convert/{id}")
    public ResponseEntity<?> convertToPdf(@PathVariable Long id) {
        File tempDir    = null;
        File tempInput  = null;
        File tempOutput = null;
        try {
            DocumentDto doc = documentService.findDocumentById(id);
            String fileType = doc.getFileType() != null ? doc.getFileType().toUpperCase() : "";
            String filePath = documentService.getFilePath(id);
            byte[] fileBytes = networkFileService.readFileFromNetworkShare(filePath);

            // Write source file to a temp directory
            tempDir   = Files.createTempDirectory("pdfconv_").toFile();
            tempInput = new File(tempDir, "input." + fileType.toLowerCase());
            Files.write(tempInput.toPath(), fileBytes);

            // Locate LibreOffice / soffice
            String soffice = findSoffice();
            if (soffice == null) {
                logger.warn("LibreOffice not found on this system for PDF conversion of id={}", id);
                return ResponseEntity.status(503)
                        .body(Map.of("error", "LibreOffice is not installed on the server. Cannot convert to PDF."));
            }

            // Run: soffice --headless --convert-to pdf --outdir <tempDir> <inputFile>
            ProcessBuilder pb = new ProcessBuilder(
                    soffice, "--headless", "--convert-to", "pdf",
                    "--outdir", tempDir.getAbsolutePath(),
                    tempInput.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            // Consume output to prevent blocking
            String output = new String(proc.getInputStream().readAllBytes());
            boolean finished = proc.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                logger.error("LibreOffice conversion timed out for id={}", id);
                return ResponseEntity.status(504).body(Map.of("error", "PDF conversion timed out."));
            }

            // LibreOffice names the output file by replacing the extension with .pdf
            tempOutput = new File(tempDir, "input.pdf");
            if (!tempOutput.exists()) {
                logger.error("LibreOffice produced no output for id={}. Output: {}", id, output);
                return ResponseEntity.status(500)
                        .body(Map.of("error", "PDF conversion failed. LibreOffice output: " + output));
            }

            byte[] pdfBytes = Files.readAllBytes(tempOutput.toPath());
            logger.info("Converted {} (id={}) to PDF — {} bytes", fileType, id, pdfBytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (Exception e) {
            logger.error("PDF conversion failed for id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Conversion failed: " + e.getMessage()));
        } finally {
            // Clean up temp files
            if (tempOutput != null) tempOutput.delete();
            if (tempInput  != null) tempInput.delete();
            if (tempDir    != null) tempDir.delete();
        }
    }

    /** Finds the soffice / LibreOffice executable on common install paths. */
    private String findSoffice() {
        String[] candidates = {
                // Windows
                "C:/Program Files/LibreOffice/program/soffice.exe",
                "C:/Program Files (x86)/LibreOffice/program/soffice.exe",
                // Linux
                "/usr/bin/soffice",
                "/usr/lib/libreoffice/program/soffice",
                "/opt/libreoffice/program/soffice",
                // macOS
                "/Applications/LibreOffice.app/Contents/MacOS/soffice",
                // PATH fallback
                "soffice"
        };
        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.getInputStream().readAllBytes(); // consume output
                if (p.waitFor(5, TimeUnit.SECONDS)) {
                    logger.info("Found LibreOffice at: {}", candidate);
                    return candidate;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
