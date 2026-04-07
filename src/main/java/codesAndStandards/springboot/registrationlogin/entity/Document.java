package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "edition")
    private String edition;

    // Stores YYYY or YYYY-MM as a string (month optional)
    @Column(name = "publication_date", length = 7)
    private String publishDate;

    @Column(name = "number_of_pages")
    private Integer noOfPages;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    // ⭐ NEW: Stores file type/extension in UPPERCASE (e.g. PDF, DOCX, XLSX, PPTX, TXT, etc.)
    // Allows viewer, download, and library to handle each type correctly
    @Column(name = "file_type", length = 10)
    private String fileType;

    @Column(name = "created_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploader_user_id")
    private User uploadedBy;

    // Many-to-Many with Tags
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "DocumentTags",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // Many-to-Many with Classifications
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "DocumentClassifications",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "classification_id")
    )
    private Set<Classification> classifications = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getDocuments().add(this);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getDocuments().remove(this);
    }

    public void addClassification(Classification classification) {
        this.classifications.add(classification);
        classification.getDocuments().add(this);
    }

    public void removeClassification(Classification classification) {
        this.classifications.remove(classification);
        classification.getDocuments().remove(this);
    }

    // ⭐ NEW: Helper — returns true if this document is a PDF
    public boolean isPdf() {
        return "PDF".equalsIgnoreCase(fileType);
    }

    // ⭐ NEW: Helper — returns true if this document is a Word document
    public boolean isWordDoc() {
        return "DOC".equalsIgnoreCase(fileType) || "DOCX".equalsIgnoreCase(fileType) || "ODT".equalsIgnoreCase(fileType);
    }

    // ⭐ NEW: Helper — returns true if this document is a spreadsheet
    public boolean isSpreadsheet() {
        return "XLS".equalsIgnoreCase(fileType) || "XLSX".equalsIgnoreCase(fileType) || "CSV".equalsIgnoreCase(fileType);
    }

    // ⭐ NEW: Helper — returns true if this document is a presentation
    public boolean isPresentation() {
        return "PPT".equalsIgnoreCase(fileType) || "PPTX".equalsIgnoreCase(fileType);
    }

    // ⭐ NEW: Helper — returns true if this document is plain text
    public boolean isTextFile() {
        return "TXT".equalsIgnoreCase(fileType) || "RTF".equalsIgnoreCase(fileType);
    }
}