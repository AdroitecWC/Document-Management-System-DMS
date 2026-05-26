package codesAndStandards.springboot.registrationlogin.dto;

import java.util.List;

public class CustomDocTypeDto {

    // ── Summary ────────────────────────────────────────────────
    private Long   customDocTypeId;
    private Long   docTypeId;
    private String parentTypeName;
    private String customTypeName;
    private String description;
    private int    stateCount;
    private int    transitionCount;

    // ── Detail (GET /{id} / expand) ────────────────────────────
    private List<StateInfo>       selectedStates;
    private List<TransitionActor> transitionActors;

    // ── Payload (POST/PUT) ─────────────────────────────────────
    private List<Long>            selectedStateIds;
    private List<ActorAssignment> actorAssignments;

    public static class StateInfo {
        public Long   stateId;
        public String stateName;
        public String color;
    }

    public static class TransitionActor {
        public Long    transitionId;
        public String  fromStateName;
        public String  toStateName;
        public Long    lcRoleId;
        public String  roleName;
        public Long    userId;
        public String  userName;
        public String  userEmail;
        public boolean assigned;
    }

    public static class ActorAssignment {
        public Long transitionId;
        public Long userId;         // null = unassign
    }

    // ── getters / setters ──────────────────────────────────────
    public Long   getCustomDocTypeId()                          { return customDocTypeId; }
    public void   setCustomDocTypeId(Long c)                   { this.customDocTypeId = c; }
    public Long   getDocTypeId()                               { return docTypeId; }
    public void   setDocTypeId(Long d)                         { this.docTypeId = d; }
    public String getParentTypeName()                          { return parentTypeName; }
    public void   setParentTypeName(String p)                  { this.parentTypeName = p; }
    public String getCustomTypeName()                          { return customTypeName; }
    public void   setCustomTypeName(String c)                  { this.customTypeName = c; }
    public String getDescription()                             { return description; }
    public void   setDescription(String d)                     { this.description = d; }
    public int    getStateCount()                              { return stateCount; }
    public void   setStateCount(int s)                         { this.stateCount = s; }
    public int    getTransitionCount()                         { return transitionCount; }
    public void   setTransitionCount(int t)                    { this.transitionCount = t; }
    public List<StateInfo>       getSelectedStates()           { return selectedStates; }
    public void   setSelectedStates(List<StateInfo> s)         { this.selectedStates = s; }
    public List<TransitionActor> getTransitionActors()         { return transitionActors; }
    public void   setTransitionActors(List<TransitionActor> t) { this.transitionActors = t; }
    public List<Long>            getSelectedStateIds()         { return selectedStateIds; }
    public void   setSelectedStateIds(List<Long> s)            { this.selectedStateIds = s; }
    public List<ActorAssignment> getActorAssignments()         { return actorAssignments; }
    public void   setActorAssignments(List<ActorAssignment> a) { this.actorAssignments = a; }
}