package codesAndStandards.springboot.registrationlogin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    private Integer id;

    private String title;

    private String productCode;

    private String edition;

    // Stored as YYYY or YYYY-MM
    private String publishDate;

    // Used separately in upload form (year and month dropdowns)
    private String publishYear;
    private String publishMonth;

    @NotNull(message = "Number of pages is required")
    @Min(value = 1, message = "Number of pages must be at least 1")
    private Integer noOfPages;

    private String notes;

    private List<TagDto> tags;
    private List<ClassificationDto> classifications;

    private String filePath;

    // ⭐ NEW: File type/extension in UPPERCASE (e.g. "PDF", "DOCX", "XLSX", "PPTX", "TXT", "RTF", etc.)
    // Populated from Document entity. Fallback to "PDF" for legacy documents.
    private String fileType;

    private String uploadedAt;

    private String uploadedByUsername;

    // Comma-separated tag names (used for form binding and stored procedure)
    private String tagNames;

    // Comma-separated classification names
    private String classificationNames;

    // Comma-separated group IDs (used in upload/edit forms)
    private String groupIds;

    // Resolved group names (used for display in viewer/info panel)
    private String groupNames;

    // ⭐ Convenience helpers for the viewer and document library

    public boolean isPdf() {
        return "PDF".equalsIgnoreCase(fileType);
    }

    public boolean isWordDoc() {
        return "DOC".equalsIgnoreCase(fileType)
                || "DOCX".equalsIgnoreCase(fileType)
                || "ODT".equalsIgnoreCase(fileType);
    }

    public boolean isSpreadsheet() {
        return "XLS".equalsIgnoreCase(fileType)
                || "XLSX".equalsIgnoreCase(fileType)
                || "CSV".equalsIgnoreCase(fileType);
    }

    public boolean isPresentation() {
        return "PPT".equalsIgnoreCase(fileType)
                || "PPTX".equalsIgnoreCase(fileType);
    }

    public boolean isTextFile() {
        return "TXT".equalsIgnoreCase(fileType)
                || "RTF".equalsIgnoreCase(fileType);
    }

    // ⭐ Returns a human-friendly label for the file type (used in document library badges)
    public String getFileTypeLabel() {
        if (fileType == null) return "PDF";
        switch (fileType.toUpperCase()) {
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
            default:     return fileType.toUpperCase();
        }
    }

    // ⭐ Returns a Bootstrap color class for the file type badge (used in document library)
    public String getFileTypeBadgeClass() {
        if (fileType == null) return "danger";
        switch (fileType.toUpperCase()) {
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