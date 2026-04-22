package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Actions")
public class Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Integer actionId;

    @Column(name = "action_name", length = 100, nullable = false, unique = true)
    private String actionName;
}
