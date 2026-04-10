package codesAndStandards.springboot.registrationlogin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;

    @NotEmpty(message = "First name should not be empty")
    private String firstName;

    @NotEmpty(message = "Last name should not be empty")
    private String lastName;

    @NotEmpty(message = "Username should not be empty")
    private String username;

    @NotEmpty(message = "Email should not be empty")
    @Email
    private String email;

    private String password;

    // Integer to match Role entity PK type
    private Integer roleId;
    private String roleName;

    private String createdByUsername;
    private String createdAt;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
    private List<Long> groupIds;

    private List<GroupListDTO> groups;
}
