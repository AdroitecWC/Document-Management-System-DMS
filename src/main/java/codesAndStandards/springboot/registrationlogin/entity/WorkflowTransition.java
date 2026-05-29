package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "WorkflowTransitions")
public class WorkflowTransition {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "from_state_id", nullable = false)
    private Long fromStateId;

    @Column(name = "to_state_id", nullable = false)
    private Long toStateId;

    /** FK → LifeCycleRoles.lc_role_id */
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    /** FK → Users.user_id; one row per responsible person for this transition */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}