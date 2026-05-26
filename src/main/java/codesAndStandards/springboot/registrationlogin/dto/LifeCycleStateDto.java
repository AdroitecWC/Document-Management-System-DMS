package codesAndStandards.springboot.registrationlogin.dto;

public class LifeCycleStateDto {
    private Long   stateId;
    private String stateName;
    private String description;
    private String color;
    private int    usedInTypesCount;

    public Long   getStateId()                        { return stateId; }
    public void   setStateId(Long stateId)            { this.stateId = stateId; }
    public String getStateName()                      { return stateName; }
    public void   setStateName(String stateName)      { this.stateName = stateName; }
    public String getDescription()                    { return description; }
    public void   setDescription(String description)  { this.description = description; }
    public String getColor()                          { return color; }
    public void   setColor(String color)              { this.color = color; }
    public int    getUsedInTypesCount()               { return usedInTypesCount; }
    public void   setUsedInTypesCount(int count)      { this.usedInTypesCount = count; }
}