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
@Table(name = "ActivityLog")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_ActivityLog_Users")
    )
    private User user;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "details", columnDefinition = "nvarchar(MAX)", nullable = true)
    private String details;

    @Column(name = "timestamp", nullable = true,
            columnDefinition = "datetime DEFAULT GETDATE()")
    private LocalDateTime timestamp;
}
