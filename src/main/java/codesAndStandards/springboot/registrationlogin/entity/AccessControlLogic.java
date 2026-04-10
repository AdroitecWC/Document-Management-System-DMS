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
        name = "AccessControlLogic",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_AccessControlLogic_DocumentId_GroupId", columnNames = {"document_id", "group_id"})
        }
)
public class AccessControlLogic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            referencedColumnName = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_AccessControlLogic_Documents")
    )
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "group_id",
            referencedColumnName = "group_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_AccessControlLogic_Groups")
    )
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_AccessControlLogic_Users")
    )
    private User createdBy;

    @Column(name = "created_at", nullable = false,
            columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime createdAt;
}
