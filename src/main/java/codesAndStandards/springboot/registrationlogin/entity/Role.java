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
        name = "Roles",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_Roles_RoleName", columnNames = {"role_name"})
        }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    // Check constraint enforced at DB level: 'Viewer', 'Manager', 'Admin', 'Super Admin'
    @Column(name = "role_name", length = 50, nullable = false)
    private String roleName;
}
