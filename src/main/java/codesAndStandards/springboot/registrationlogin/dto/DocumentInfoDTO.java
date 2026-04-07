package codesAndStandards.springboot.registrationlogin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentInfoDTO {
    private int id;
    private String title;
    private String documentCode;
    private String category;
}