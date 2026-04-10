package codesAndStandards.springboot.registrationlogin.dto;

import lombok.Data;

@Data
public class DocumentMetadata {
    private String filename;
    private String title;
    private String docTypeName;
    private String versionNumber;
    private String tags;              // Comma-separated tag names
    private String classifications;   // Comma-separated classification names
}
