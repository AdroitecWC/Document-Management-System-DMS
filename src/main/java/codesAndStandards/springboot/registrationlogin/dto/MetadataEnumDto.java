package codesAndStandards.springboot.registrationlogin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetadataEnumDto {

    private Long id;
    private Long metadataId;
    private String metadataFieldName; // For display purposes
    private String value;
}
