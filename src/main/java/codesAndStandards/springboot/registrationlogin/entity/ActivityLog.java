package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "details", nullable = true)
    private String details;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = true, updatable = false)
    private LocalDateTime timestamp;
}
