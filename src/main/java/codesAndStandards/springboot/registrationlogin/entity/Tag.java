package codesAndStandards.springboot.registrationlogin.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "Tags",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_Tags_TagName", columnNames = {"tag_name"})
        }
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "tag_name", length = 510, nullable = false)
    private String tagName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Tags_Users_CreatedBy")
    )
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Tags_Users_UpdatedBy")
    )
    private User updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;
}
