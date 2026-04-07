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
                @UniqueConstraint(
                        name = "UQ_UserGroup",
                        columnNames = {"user_id", "groupId"}
                )
        }
)
public class GroupUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "groupUserId")
    private Long groupUserId;

    // FK: User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_GroupUser_User")
    )
    private User user;

    // FK: Group
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "groupId",
            referencedColumnName = "groupId",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_GroupUser_Group")
    )
    private Group group;

    // created_by WITHOUT FK constraint
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = true,
//            referencedColumnName = "user_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
