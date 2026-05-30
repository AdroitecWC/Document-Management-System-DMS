package codesAndStandards.springboot.registrationlogin.dto;

import java.util.List;

public class WorkflowDto {

    // ── Summary ──────────────────────────────────────────────────────
    private Long    workflowId;
    private Long    docTypeId;
    private String  parentTypeName;
    private String  workflowName;
    private String  description;
    private boolean isActive;
    private int     stateCount;
    private int     transitionCount;

    // ── Detail (GET /{id}, expand) ───────────────────────────────────
    private List<StateInfo>      selectedStates;
    private List<TransitionInfo> transitions;

    // ── Payload (POST/PUT) ───────────────────────────────────────────
    private List<Long>              selectedStateIds;
    private List<TransitionPayload> transitionPayloads;

    // ─── nested ──────────────────────────────────────────────────────
    public static class StateInfo {
        public Long   stateId;
        public String stateName;
        public String color;
    }

    public static class UserInfo {
        public Long   userId;
        public String userName;
        public String userEmail;
        public String workflowName;
        public String docTypeName;
    }

    public static class TransitionInfo {
        public Long         id;
        public Long         fromStateId;
        public String       fromStateName;
        public Long         toStateId;
        public String       toStateName;
        public Long         roleId;
        public String       roleName;
        public List<UserInfo> users;
    }

    public static class TransitionPayload {
        public Long       id;           // null = new row
        public Long       fromStateId;
        public Long       toStateId;
        public Long       roleId;
        public List<Long> userIds;
    }

    // ─── getters / setters ───────────────────────────────────────────
    public Long    getWorkflowId()                                    { return workflowId; }
    public void    setWorkflowId(Long w)                              { this.workflowId = w; }
    public Long    getDocTypeId()                                     { return docTypeId; }
    public void    setDocTypeId(Long d)                               { this.docTypeId = d; }
    public String  getParentTypeName()                                { return parentTypeName; }
    public void    setParentTypeName(String p)                        { this.parentTypeName = p; }
    public String  getWorkflowName()                                  { return workflowName; }
    public void    setWorkflowName(String w)                          { this.workflowName = w; }
    public String  getDescription()                                   { return description; }
    public void    setDescription(String d)                           { this.description = d; }
    public boolean isActive()                                         { return isActive; }
    public void    setActive(boolean a)                               { this.isActive = a; }
    public int     getStateCount()                                    { return stateCount; }
    public void    setStateCount(int s)                               { this.stateCount = s; }
    public int     getTransitionCount()                               { return transitionCount; }
    public void    setTransitionCount(int t)                          { this.transitionCount = t; }
    public List<StateInfo>      getSelectedStates()                   { return selectedStates; }
    public void    setSelectedStates(List<StateInfo> s)               { this.selectedStates = s; }
    public List<TransitionInfo> getTransitions()                      { return transitions; }
    public void    setTransitions(List<TransitionInfo> t)             { this.transitions = t; }
    public List<Long>              getSelectedStateIds()              { return selectedStateIds; }
    public void    setSelectedStateIds(List<Long> s)                  { this.selectedStateIds = s; }
    public List<TransitionPayload> getTransitionPayloads()            { return transitionPayloads; }
    public void    setTransitionPayloads(List<TransitionPayload> t)   { this.transitionPayloads = t; }
}