package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MetadataEnum")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataEnum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_MetadataEnum_MetadataDefinitions"))
    private MetadataDefinition metadataDefinition;

    @Column(name = "value", length = 200, nullable = false)
    private String value;
}
