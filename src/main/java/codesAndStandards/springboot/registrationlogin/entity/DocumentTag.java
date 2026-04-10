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
@Table(name = "DocumentTag")
public class DocumentTag {

    @EmbeddedId
    private DocumentTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(
            name = "document_id",
            referencedColumnName = "document_id",
            foreignKey = @ForeignKey(name = "FK_DocumentTag_Documents")
    )
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(
            name = "tag_id",
            referencedColumnName = "tag_id",
            foreignKey = @ForeignKey(name = "FK_DocumentTag_Tags")
    )
    private Tag tag;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentTagId implements Serializable {

        @Column(name = "document_id")
        private Long documentId;

        @Column(name = "tag_id")
        private Long tagId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DocumentTagId)) return false;
            DocumentTagId that = (DocumentTagId) o;
            return Objects.equals(documentId, that.documentId) &&
                   Objects.equals(tagId, that.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentId, tagId);
        }
    }
}
