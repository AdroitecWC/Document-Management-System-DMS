package codesAndStandards.springboot.registrationlogin.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PermissionId implements Serializable {
    private Integer role;
    private Integer action;
}
