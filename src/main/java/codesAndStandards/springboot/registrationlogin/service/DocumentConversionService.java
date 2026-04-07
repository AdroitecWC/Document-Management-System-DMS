

package codesAndStandards.springboot.registrationlogin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentConversionService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentConversionService.class);

    // ⭐ LibreOffice executable path — configurable via application.properties
    @Value("${libreoffice.path:C:\\Program Files\\LibreOffice\\program\\soffice.exe}")
    private String libreofficePath;

    // ⭐ Temp directory for converted PDFs (cleaned up after serving)
    @Value("${conversion.temp-dir:${java.io.tmpdir}/dms-conversions}")
    private String tempDir;

    // ⭐ File types that need conversion to PDF for viewing
    private static final List<String> NEEDS_CONVERSION = Arrays.asList(
            "DOC", "DOCX", "ODT", "PPTX", "PPT", "RTF"
    );

    // ⭐ Conversion timeout in seconds
    private static final int CONVERSION_TIMEOUT_SECONDS = 60;

    /**
     * Check if a file type needs conversion for viewing
     */
    public boolean needsConversion(String fileType) {
        if (fileType == null) return false;
        return NEEDS_CONVERSION.contains(fileType.toUpperCase());
    }

    /**
     * Convert a document to PDF for viewing purposes only.
     * The converted PDF is stored in a temp directory and returned as bytes.
     * The original file is NOT modified.
     *
     * @param originalFilePath Path to the original file (DOC/ODT/PPTX/PPT)
     * @param fileType         File type (DOC, ODT, PPTX, PPT, RTF)
     * @return PDF bytes for viewing
     * @throws Exception if conversion fails
     */
    public byte[] convertToPdfForViewing(String originalFilePath, String fileType) throws Exception {
        logger.info("Converting {} file to PDF for viewing: {}", fileType, originalFilePath);

        // Verify LibreOffice exists
        File libreOfficeExe = new File(libreofficePath);
        if (!libreOfficeExe.exists()) {
            throw new RuntimeException(
                    "LibreOffice not found at: " + libreofficePath +
                            ". Please install LibreOffice or update 'libreoffice.path' in application.properties"
            );
        }

        // Verify source file exists
        File sourceFile = new File(originalFilePath);
        if (!sourceFile.exists()) {
            throw new RuntimeException("Source file not found: " + originalFilePath);
        }

        // Create temp output directory
        String conversionId = UUID.randomUUID().toString();
        Path tempOutputDir = Paths.get(tempDir, conversionId);
        Files.createDirectories(tempOutputDir);

        try {
            // Build LibreOffice conversion command
            // --headless: no GUI
            // --convert-to pdf: output format
            // --outdir: output directory
            ProcessBuilder pb = new ProcessBuilder(
                    libreofficePath,
                    "--headless",
                    "--norestore",
                    "--nofirststartwizard",
                    "--convert-to", "pdf",
                    "--outdir", tempOutputDir.toString(),
                    originalFilePath
            );

            pb.redirectErrorStream(true);
            pb.environment().put("HOME", tempOutputDir.toString());

            logger.info("Running LibreOffice command: {}", pb.command());

            Process process = pb.start();

            // Wait for conversion with timeout
            boolean completed = process.waitFor(CONVERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("LibreOffice conversion timed out after " + CONVERSION_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                logger.error("LibreOffice conversion failed with exit code {}: {}", exitCode, output);
                throw new RuntimeException("LibreOffice conversion failed with exit code: " + exitCode);
            }

            // Find the generated PDF file
            // LibreOffice names the output as: originalFileName.pdf
            String originalFileName = sourceFile.getName();
            String pdfFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.')) + ".pdf";
            Path pdfPath = tempOutputDir.resolve(pdfFileName);

            if (!Files.exists(pdfPath)) {
                // Search for any PDF in the temp dir
                File[] pdfFiles = tempOutputDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                if (pdfFiles == null || pdfFiles.length == 0) {
                    throw new RuntimeException("LibreOffice did not produce a PDF output file");
                }
                pdfPath = pdfFiles[0].toPath();
            }

            logger.info("✅ Conversion successful. PDF size: {} bytes", Files.size(pdfPath));

            // Read PDF bytes
            byte[] pdfBytes = Files.readAllBytes(pdfPath);

            return pdfBytes;

        } finally {
            // ⭐ Always clean up temp files after reading
            cleanupTempDir(tempOutputDir);
        }
    }

    /**
     * Clean up temporary conversion directory
     */
    private void cleanupTempDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                logger.debug("Cleaned up temp conversion dir: {}", dir);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up temp dir {}: {}", dir, e.getMessage());
        }
    }

    /**
     * Check if LibreOffice is available on this system
     */
    public boolean isLibreOfficeAvailable() {
        try {
            File exe = new File(libreofficePath);
            return exe.exists() && exe.canExecute();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the configured LibreOffice path
     */
    public String getLibreOfficePath() {
        return libreofficePath;
    }
}
