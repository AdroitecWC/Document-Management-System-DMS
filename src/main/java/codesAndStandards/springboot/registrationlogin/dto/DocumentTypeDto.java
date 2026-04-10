package codesAndStandards.springboot.registrationlogin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentTypeDto {
    private Long docTypeId;
    private String docTypeName;
    private String description;
    private Integer documentCount;

    // Associated metadata definitions for this type
    private List<MetadataDefinitionDto> metadataDefinitions;
}
