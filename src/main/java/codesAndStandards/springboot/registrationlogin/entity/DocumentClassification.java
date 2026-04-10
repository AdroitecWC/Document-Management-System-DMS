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
@Table(name = "DocumentClassification")
public class DocumentClassification {

    @EmbeddedId
    private DocumentClassificationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(
            name = "document_id",
            referencedColumnName = "document_id",
            foreignKey = @ForeignKey(name = "FK_DocumentClassification_Documents")
    )
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classificationId")
    @JoinColumn(
            name = "classification_id",
            referencedColumnName = "classification_id",
            foreignKey = @ForeignKey(name = "FK_DocumentClassification_Classifications")
    )
    private Classification classification;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentClassificationId implements Serializable {

        @Column(name = "document_id")
        private Long documentId;

        @Column(name = "classification_id")
        private Long classificationId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DocumentClassificationId)) return false;
            DocumentClassificationId that = (DocumentClassificationId) o;
            return Objects.equals(documentId, that.documentId) &&
                   Objects.equals(classificationId, that.classificationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId, classificationId);
        }
    }
}
