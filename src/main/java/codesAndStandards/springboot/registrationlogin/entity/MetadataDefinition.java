package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "MetadataDefinitions",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_MetadataDefinitions_FieldName", columnNames = {"field_name"})
        }
)
public class MetadataDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metadata_id")
    private Long metadataId;

    @Column(name = "field_name", length = 100, nullable = false)
    private String fieldName;

    @Column(name = "field_type", length = 100, nullable = false)
    private String fieldType;
}
