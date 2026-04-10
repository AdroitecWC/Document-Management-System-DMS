package codesAndStandards.springboot.registrationlogin.dto;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;

public class ExtractedMultipartFile implements MultipartFile {

    private final File file;
    private final String filename;

    public ExtractedMultipartFile(File file, String filename) {
        this.file = file;
        this.filename = filename;
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public String getOriginalFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        // Detect content type from file extension instead of hardcoding PDF
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType != null) {
                return mimeType;
            }
        } catch (IOException ignored) {
        }
        // Fallback based on extension
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".doc")) return "application/msword";
            if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
            if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
            if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            if (lower.endsWith(".odt")) return "application/vnd.oasis.opendocument.text";
            if (lower.endsWith(".rtf")) return "application/rtf";
            if (lower.endsWith(".txt")) return "text/plain";
            if (lower.endsWith(".csv")) return "text/csv";
        }
        return "application/octet-stream";
    }

    @Override
    public boolean isEmpty() {
        return file.length() == 0;
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.copy(file.toPath(), dest.toPath());
    }
}
