package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "doc_type_id",
            referencedColumnName = "doc_type_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_Documents_DocumentTypes")
    )
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "uploader_user_id",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Documents_Users")
    )
    private User uploaderUser;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createdAt;

    @Column(name = "file_extension", length = 10, nullable = false)
    private String fileExtension;
}
