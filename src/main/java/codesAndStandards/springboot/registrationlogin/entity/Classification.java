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
        name = "Classifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_Classifications_ClassificationName", columnNames = {"classification_name"})
        }
)
public class Classification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classification_id")
    private Long classificationId;

    @Column(name = "classification_name", length = 510, nullable = false)
    private String classificationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Classifications_Users_CreatedBy")
    )
    private User createdBy;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Classifications_Users_UpdatedBy")
    )
    private User updatedBy;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;
}
