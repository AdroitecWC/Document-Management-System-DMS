package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "DocumentTypeStates")
public class DocumentTypeState {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_type_id", nullable = false)
    private Long docTypeId;

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @Column(name = "is_initial", nullable = false)
    private boolean isInitial = false;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
}