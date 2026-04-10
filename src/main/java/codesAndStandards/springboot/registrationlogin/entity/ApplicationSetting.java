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
        name = "ApplicationSettings",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_ApplicationSettings_SettingName", columnNames = {"setting_name"})
        }
)
public class ApplicationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "setting_name", length = 300, nullable = false)
    private String settingName;

    @Column(name = "setting_value", columnDefinition = "nvarchar(MAX)", nullable = true)
    private String settingValue;

    @Column(name = "created_at", nullable = true,
            columnDefinition = "datetime2 DEFAULT GETDATE()")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true,
            columnDefinition = "datetime2 DEFAULT GETDATE()")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_ApplicationSettings_Users")
    )
    private User updatedBy;
}
