package codesAndStandards.springboot.registrationlogin.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    private Long id;

    private String title;

    // Document Type (from DocumentTypes table)
    private Long docTypeId;
    private String docTypeName;

    // File info
    private String fileExtension;

    // Current version info (from DocumentVersion table)
    private String filePath;
    private String versionNumber;
    private Long currentVersionId;

    // All versions (for version history display)
    private List<DocumentVersionDto> versions;

    // Tags and Classifications
    private List<TagDto> tags;
    private List<ClassificationDto> classifications;

    // Comma-separated names (used for form binding and stored procedures)
    private String tagNames;
    private String classificationNames;

    // Comma-separated group IDs (used in upload/edit forms)
    private String groupIds;

    // Resolved group names (used for display in viewer/info panel)
    private String groupNames;

    // Upload info
    private String uploadedAt;
    private String uploadedByUsername;
    private Long uploadedByUserId;

    // File type helpers (derived from fileExtension)

    public String getFileType() {
        return fileExtension != null ? fileExtension.toUpperCase().replace(".", "") : "PDF";
    }

    public boolean isPdf() {
        return "PDF".equalsIgnoreCase(getFileType());
    }

    public boolean isWordDoc() {
        String ft = getFileType();
        return "DOC".equalsIgnoreCase(ft)
                || "DOCX".equalsIgnoreCase(ft)
                || "ODT".equalsIgnoreCase(ft);
    }

    public boolean isSpreadsheet() {
        String ft = getFileType();
        return "XLS".equalsIgnoreCase(ft)
                || "XLSX".equalsIgnoreCase(ft)
                || "CSV".equalsIgnoreCase(ft);
    }

    public boolean isPresentation() {
        String ft = getFileType();
        return "PPT".equalsIgnoreCase(ft)
                || "PPTX".equalsIgnoreCase(ft);
    }

    public boolean isTextFile() {
        String ft = getFileType();
        return "TXT".equalsIgnoreCase(ft)
                || "RTF".equalsIgnoreCase(ft);
    }

    public String getFileTypeLabel() {
        String ft = getFileType();
        if (ft == null) return "PDF";
        switch (ft) {
            case "PDF":  return "PDF";
            case "DOC":
            case "DOCX": return "Word";
            case "ODT":  return "ODT";
            case "XLS":
            case "XLSX": return "Excel";
            case "CSV":  return "CSV";
            case "PPT":
            case "PPTX": return "PowerPoint";
            case "TXT":  return "Text";
            case "RTF":  return "RTF";
            default:     return ft;
        }
    }

    public String getFileTypeBadgeClass() {
        String ft = getFileType();
        if (ft == null) return "danger";
        switch (ft) {
            case "PDF":  return "danger";
            case "DOC":
            case "DOCX":
            case "ODT":  return "primary";
            case "XLS":
            case "XLSX":
            case "CSV":  return "success";
            case "PPT":
            case "PPTX": return "warning";
            case "TXT":
            case "RTF":  return "secondary";
            default:     return "dark";
        }
    }
}
