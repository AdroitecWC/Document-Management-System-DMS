package codesAndStandards.springboot.registrationlogin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTypeMetadataDto {
    private Long docTypeId;
    private String docTypeName;
    private Long metadataId;
    private String fieldName;
    private String fieldType;
    private Boolean mandatory;
}
