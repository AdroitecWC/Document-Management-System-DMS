package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "DocumentVersion")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            referencedColumnName = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_DocumentVersion_Documents")
    )
    private Document document;

    @Column(name = "version_number", length = 20, nullable = false)
    private String versionNumber;

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;
}
