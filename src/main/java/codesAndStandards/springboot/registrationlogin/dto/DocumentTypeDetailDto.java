package codesAndStandards.springboot.registrationlogin.dto;

import java.util.List;

public class DocumentTypeDetailDto {

    // ── Summary (always present) ─────────────────────────────────────
    private Long   docTypeId;
    private String docTypeName;
    private String description;
    private int    fieldCount;
    private int    stateCount;
    private int    teamMemberCount;
    private int    workflowCount;

    // ── Detail (GET /{id}/full) ───────────────────────────────────────
    private List<MetaFieldEntry>  metadataDefinitions;
    private List<StateEntry>      states;
    private List<TeamGroupEntry>  teamGroups;
    private List<TeamUserEntry>   teamUsers;

    // ── Payload (POST/PUT /{id}/full) ────────────────────────────────
    private List<Long> metadataFieldIds;
    private List<Long> mandatoryFieldIds;
    private List<Long> stateIds;               // kept for backward compat
    private List<StateConfigEntry> stateConfigs; // preferred: carries isInitial/isFinal
    private List<Long> teamGroupIds;
    private List<Long> teamUserIds;             // kept for backward compat, no longer sent from UI

    // ─── nested ──────────────────────────────────────────────────────
    public static class MetaFieldEntry {
        public Long    metadataId;
        public String  fieldName;
        public String  fieldType;
        public boolean mandatory;
    }

    public static class StateEntry {
        public Long    stateId;
        public String  stateName;
        public String  color;
        public boolean isInitial;
        public boolean isFinal;
    }

    public static class TeamGroupEntry {
        public Long   groupId;
        public String groupName;
        public int    memberCount;
        public List<TeamUserEntry> members;

    }

    public static class TeamUserEntry {
        public Long   userId;
        public String username;
        public String email;
    }
    public static class StateConfigEntry {
        public Long    stateId;
        public boolean isInitial;
        public boolean isFinal;
    }

    // ─── getters / setters ───────────────────────────────────────────
    public Long   getDocTypeId()                                  { return docTypeId; }
    public void   setDocTypeId(Long d)                            { this.docTypeId = d; }
    public String getDocTypeName()                                { return docTypeName; }
    public void   setDocTypeName(String d)                        { this.docTypeName = d; }
    public String getDescription()                                { return description; }
    public void   setDescription(String d)                        { this.description = d; }
    public int    getFieldCount()                                 { return fieldCount; }
    public void   setFieldCount(int f)                            { this.fieldCount = f; }
    public int    getStateCount()                                 { return stateCount; }
    public void   setStateCount(int s)                            { this.stateCount = s; }
    public int    getTeamMemberCount()                            { return teamMemberCount; }
    public void   setTeamMemberCount(int t)                       { this.teamMemberCount = t; }
    public int    getWorkflowCount()                              { return workflowCount; }
    public void   setWorkflowCount(int w)                         { this.workflowCount = w; }
    public List<MetaFieldEntry>  getMetadataDefinitions()         { return metadataDefinitions; }
    public void   setMetadataDefinitions(List<MetaFieldEntry> m)  { this.metadataDefinitions = m; }
    public List<StateEntry>      getStates()                      { return states; }
    public void   setStates(List<StateEntry> s)                   { this.states = s; }
    public List<TeamGroupEntry>  getTeamGroups()                  { return teamGroups; }
    public void   setTeamGroups(List<TeamGroupEntry> g)           { this.teamGroups = g; }
    public List<TeamUserEntry>   getTeamUsers()                   { return teamUsers; }
    public void   setTeamUsers(List<TeamUserEntry> u)             { this.teamUsers = u; }
    public List<Long> getMetadataFieldIds()                       { return metadataFieldIds; }
    public void   setMetadataFieldIds(List<Long> m)               { this.metadataFieldIds = m; }
    public List<Long> getMandatoryFieldIds()                      { return mandatoryFieldIds; }
    public void   setMandatoryFieldIds(List<Long> m)              { this.mandatoryFieldIds = m; }
    public List<Long> getStateIds()                               { return stateIds; }
    public void   setStateIds(List<Long> s)                       { this.stateIds = s; }
    public List<Long> getTeamGroupIds()                           { return teamGroupIds; }
    public void   setTeamGroupIds(List<Long> g)                   { this.teamGroupIds = g; }
    public List<Long> getTeamUserIds()                            { return teamUserIds; }
    public void   setTeamUserIds(List<Long> u)                    { this.teamUserIds = u; }
    public List<StateConfigEntry> getStateConfigs()             { return stateConfigs; }
    public void setStateConfigs(List<StateConfigEntry> s)       { this.stateConfigs = s; }
}