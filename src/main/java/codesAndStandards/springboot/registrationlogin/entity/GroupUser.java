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
        name = "GroupUser",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_GroupUser_UserId_GroupId", columnNames = {"user_id", "group_id"})
        }
)
public class GroupUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_user_id")
    private Long groupUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_GroupUser_Users_UserId")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "group_id",
            referencedColumnName = "group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_GroupUser_Groups")
    )
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_GroupUser_Users_CreatedBy")
    )
    private User createdBy;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createdAt;
}
