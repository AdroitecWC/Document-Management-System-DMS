package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "DocumentTypes")
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_type_id")
    private Long docTypeId;

    @Column(name = "doc_type_name", length = 50, nullable = false)
    private String docTypeName;

    @Column(name = "description", length = 150, nullable = true)
    private String description;
}
