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
@Table(name = "Groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name", length = 100, nullable = false)
    private String groupName;

    @Column(name = "description", length = 500, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Groups_Users")
    )
    private User createdBy;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createdAt;
}
