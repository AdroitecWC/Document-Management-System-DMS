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

    // Used on GET responses — full metadata details with mandatory flag
    private List<MetadataDefinitionDto> metadataDefinitions;

    // Used on POST/PUT request body
    private List<Long> metadataFieldIds;
    private List<Long> mandatoryFieldIds;
}