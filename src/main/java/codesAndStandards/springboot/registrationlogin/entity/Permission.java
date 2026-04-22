package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Permissions")
@IdClass(PermissionId.class)
public class Permission {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;
}
