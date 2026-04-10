package codesAndStandards.springboot.registrationlogin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersionDto {
    private Long versionId;
    private Long documentId;
    private String versionNumber;
    private String filePath;
}
