package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "DocumentTypeMetadata")
public class DocumentTypeMetadata {

    @EmbeddedId
    private DocumentTypeMetadataId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("docTypeId")
    @JoinColumn(
            name = "doc_type_id",
            referencedColumnName = "doc_type_id",
            foreignKey = @ForeignKey(name = "FK_DocumentTypeMetadata_DocumentTypes")
    )
    private DocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("metadataId")
    @JoinColumn(
            name = "metadata_id",
            referencedColumnName = "metadata_id",
            foreignKey = @ForeignKey(name = "FK_DocumentTypeMetadata_MetadataDefinitions")
    )
    private MetadataDefinition metadataDefinition;

    @Column(name = "mandatory", nullable = false,
            columnDefinition = "bit DEFAULT 0")
    private Boolean mandatory;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentTypeMetadataId implements Serializable {

        @Column(name = "doc_type_id")
        private Long docTypeId;

        @Column(name = "metadata_id")
        private Long metadataId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DocumentTypeMetadataId)) return false;
            DocumentTypeMetadataId that = (DocumentTypeMetadataId) o;
            return Objects.equals(docTypeId, that.docTypeId) &&
                   Objects.equals(metadataId, that.metadataId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(docTypeId, metadataId);
        }
    }
}
