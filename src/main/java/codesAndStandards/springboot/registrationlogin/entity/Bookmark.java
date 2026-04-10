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
@Table(name = "Bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = true,
            foreignKey = @ForeignKey(name = "FK_Bookmarks_Users")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            referencedColumnName = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_Bookmarks_Documents")
    )
    private Document document;

    @Column(name = "bookmark_name", length = 100, nullable = false)
    private String bookmarkName;

    @Column(name = "created_at", nullable = true,
            columnDefinition = "datetime2 DEFAULT SYSDATETIMEOFFSET()")
    private LocalDateTime createdAt;
}
