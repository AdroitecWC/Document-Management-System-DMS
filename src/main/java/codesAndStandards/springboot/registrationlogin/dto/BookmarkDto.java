package codesAndStandards.springboot.registrationlogin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkDto {

    private Long id;
    private Long userId;
    private Integer documentId;
//    private Integer pageNumber;
    private String bookmarkName;
    private LocalDateTime createdAt;

    // Additional fields for display
    private String userName;
    private String documentTitle;
}