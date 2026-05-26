package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "LifeCycleRoles")
public class LifeCycleRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lc_role_id")
    private Long lcRoleId;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    public Long getLcRoleId()                   { return lcRoleId; }
    public void setLcRoleId(Long lcRoleId)      { this.lcRoleId = lcRoleId; }
    public String getRoleName()                 { return roleName; }
    public void setRoleName(String roleName)    { this.roleName = roleName; }
    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }
}