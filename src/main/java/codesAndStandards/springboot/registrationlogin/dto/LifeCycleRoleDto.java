package codesAndStandards.springboot.registrationlogin.dto;

public class LifeCycleRoleDto {
    private Long   lcRoleId;
    private String roleName;
    private String description;
    private int    usedInTypesCount;

    public Long   getLcRoleId()                       { return lcRoleId; }
    public void   setLcRoleId(Long lcRoleId)          { this.lcRoleId = lcRoleId; }
    public String getRoleName()                       { return roleName; }
    public void   setRoleName(String roleName)        { this.roleName = roleName; }
    public String getDescription()                    { return description; }
    public void   setDescription(String description)  { this.description = description; }
    public int    getUsedInTypesCount()               { return usedInTypesCount; }
    public void   setUsedInTypesCount(int count)      { this.usedInTypesCount = count; }
}