package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "Users",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_Users_Email", columnNames = {"email"}),
                @UniqueConstraint(name = "UQ_Users_Username", columnNames = {"username"})
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "first_name", length = 150, nullable = true)
    private String firstName;

    @Column(name = "last_name", length = 150, nullable = true)
    private String lastName;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "role_id",
            referencedColumnName = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_Users_Roles")
    )
    private Role role;

    @Column(name = "created_at", nullable = true,
            columnDefinition = "datetime2 DEFAULT SYSDATETIMEOFFSET()")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Users_Users_CreatedBy")
    )
    private User createdBy;
}
