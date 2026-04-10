package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DocumentMetadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_DocumentMetadata_Documents"))
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_DocumentMetadata_MetadataDefinitions"))
    private MetadataDefinition metadataDefinition;

    @Column(name = "value", columnDefinition = "nvarchar(max)", nullable = true)
    private String value;
}
