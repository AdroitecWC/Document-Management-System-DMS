package codesAndStandards.springboot.registrationlogin.controller;

import codesAndStandards.springboot.registrationlogin.dto.DocumentDto;
import codesAndStandards.springboot.registrationlogin.service.DocumentService;
import codesAndStandards.springboot.registrationlogin.service.NetworkFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Converts DOCX / ODT / RTF → HTML using Pandoc (45 MB, cross-platform).
 * All images are embedded as base64 data-URIs (--embed-resources flag).
 * The resulting body HTML is returned to the browser for display in the viewer.
 *
 * Install Pandoc from https://pandoc.org/installing.html
 * Windows: add to PATH, or place pandoc.exe anywhere and set PANDOC_HOME env var.
 */
@RestController
public class PandocHtmlController {

    private static final Logger logger = LoggerFactory.getLogger(PandocHtmlController.class);

    // Override pandoc path via env var: PANDOC_PATH=C:\tools\pandoc.exe
    private static final String ENV_PANDOC_PATH = System.getenv("PANDOC_PATH");

    @Autowired private DocumentService    documentService;
    @Autowired private NetworkFileService networkFileService;

    @PreAuthorize("hasAnyAuthority('Manager', 'Admin', 'Viewer')")
    @GetMapping("/documents/pandoc-preview/{id}")
    public ResponseEntity<?> pandocPreview(@PathVariable Integer id) {
        File tempDir   = null;
        File inputFile = null;
        try {
            DocumentDto doc = documentService.findDocumentById(id);
            String fileType = doc.getFileType() != null ? doc.getFileType().toUpperCase() : "";
            byte[] fileBytes = networkFileService.readFileFromNetworkShare(documentService.getFilePath(id));

            // Locate pandoc executable
            String pandoc = findPandoc();
            if (pandoc == null) {
                return ResponseEntity.status(503)
                        .body(Map.of("error", "Pandoc is not installed. Download from pandoc.org (~45 MB)."));
            }

            // Write source to a temp file
            tempDir   = Files.createTempDirectory("pandoc_").toFile();
            inputFile = new File(tempDir, "input." + fileType.toLowerCase());
            Files.write(inputFile.toPath(), fileBytes);

            // Run pandoc — output to stdout so we avoid a second temp file
            // --embed-resources embeds all images as base64 (Pandoc 2.19+)
            // --self-contained is the older equivalent (kept as fallback in code below)
            ProcessBuilder pb = new ProcessBuilder(
                    pandoc,
                    inputFile.getAbsolutePath(),
                    "--embed-resources", "--standalone",
                    "-t", "html5",
                    "--wrap=none"
            );
            pb.redirectErrorStream(true); // stderr → stdout
            Process proc = pb.start();

            String fullOutput = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean done = proc.waitFor(90, TimeUnit.SECONDS);
            if (!done) {
                proc.destroyForcibly();
                return ResponseEntity.status(504).body(Map.of("error", "Pandoc conversion timed out."));
            }

            int exit = proc.exitValue();
            if (exit != 0) {
                // --embed-resources may not exist in older Pandoc — retry with --self-contained
                pb = new ProcessBuilder(
                        pandoc,
                        inputFile.getAbsolutePath(),
                        "--self-contained", "--standalone",
                        "-t", "html5",
                        "--wrap=none"
                );
                pb.redirectErrorStream(true);
                proc = pb.start();
                fullOutput = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                done = proc.waitFor(90, TimeUnit.SECONDS);
                if (!done) { proc.destroyForcibly(); }
                exit = proc.exitValue();
                if (exit != 0) {
                    logger.error("Pandoc failed (exit {}) for id={}: {}", exit, id, fullOutput.substring(0, Math.min(500, fullOutput.length())));
                    return ResponseEntity.status(500)
                            .body(Map.of("error", "Pandoc conversion failed: " + fullOutput.substring(0, Math.min(300, fullOutput.length()))));
                }
            }

            // Extract only the <body> content so we don't inject a full HTML doc
            String bodyHtml = extractBodyContent(fullOutput);
            logger.info("Pandoc converted {} (id={}) → {} chars HTML", fileType, id, bodyHtml.length());
            return ResponseEntity.ok(Map.of("html", bodyHtml, "format", fileType));

        } catch (Exception e) {
            logger.error("Pandoc preview failed for id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Conversion failed: " + e.getMessage()));
        } finally {
            if (inputFile != null) inputFile.delete();
            if (tempDir   != null) tempDir.delete();
        }
    }

    /** Strips the outer HTML shell produced by --standalone, returning only body content. */
    private String extractBodyContent(String fullHtml) {
        int bodyOpen = fullHtml.indexOf("<body");
        int bodyClose = fullHtml.lastIndexOf("</body>");
        if (bodyOpen >= 0 && bodyClose > bodyOpen) {
            int contentStart = fullHtml.indexOf('>', bodyOpen) + 1;
            return fullHtml.substring(contentStart, bodyClose).trim();
        }
        return fullHtml; // fallback: return as-is
    }

    /** Finds the pandoc executable. Checks env var, common OS paths, then PATH. */
    private String findPandoc() {
        // 1. Explicit env var override
        if (ENV_PANDOC_PATH != null && !ENV_PANDOC_PATH.isBlank()) {
            if (tryExec(ENV_PANDOC_PATH)) return ENV_PANDOC_PATH;
        }
        // 2. Common installation paths
        String[] candidates = {
                // Windows
                "C:/Program Files/Pandoc/pandoc.exe",
                "C:/Program Files (x86)/Pandoc/pandoc.exe",
                System.getProperty("user.home") + "/AppData/Local/Pandoc/pandoc.exe",
                // Linux / macOS
                "/usr/bin/pandoc",
                "/usr/local/bin/pandoc",
                "/opt/homebrew/bin/pandoc",   // macOS Apple Silicon
                "/home/linuxbrew/.linuxbrew/bin/pandoc",
                // PATH fallback (last resort)
                "pandoc"
        };
        for (String c : candidates) {
            if (tryExec(c)) return c;
        }
        return null;
    }

    private boolean tryExec(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
