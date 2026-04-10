package codesAndStandards.springboot.registrationlogin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadataDto {

    private Long id;
    private Long documentId;
    private Long metadataId;
    private String fieldName;   // From MetadataDefinition — for display
    private String fieldType;   // From MetadataDefinition — for display
    private String value;
}
