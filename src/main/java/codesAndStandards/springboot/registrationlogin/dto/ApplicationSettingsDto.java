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
public class ApplicationSettingsDto {

    // Repository Settings
    private String repositoryPath;
    private Integer maxFileSizeMb;
    private String allowedFiles;

    // Metadata
    private LocalDateTime updatedAt;
    private String updatedByUsername;

    public String[] getAllowedFormatsArray() {
        if (allowedFiles == null || allowedFiles.trim().isEmpty()) {
            return new String[0];
        }
        return allowedFiles.split(",");
    }

    public boolean isFormatAllowed(String format) {
        if (allowedFiles == null || format == null) {
            return false;
        }
        String[] formats = getAllowedFormatsArray();
        for (String f : formats) {
            if (f.trim().equalsIgnoreCase(format.trim())) {
                return true;
            }
        }
        return false;
    }
}
