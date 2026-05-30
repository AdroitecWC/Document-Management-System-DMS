package codesAndStandards.springboot.registrationlogin.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "LifeCycleStates")
public class LifeCycleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "state_name", nullable = false, length = 50)
    private String stateName;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "color", length = 20)
    private String color;

    // No-arg constructor (required by JPA)
    public LifeCycleState() {}

    // ID-only constructor (used for proxy reference in DocumentLifeCycleState)
    public LifeCycleState(Long stateId) {
        this.stateId = stateId;
    }

    public Long getStateId()                    { return stateId; }
    public void setStateId(Long stateId)        { this.stateId = stateId; }
    public String getStateName()                { return stateName; }
    public void setStateName(String stateName)  { this.stateName = stateName; }
    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }
    public String getColor()                    { return color; }
    public void setColor(String color)          { this.color = color; }
}