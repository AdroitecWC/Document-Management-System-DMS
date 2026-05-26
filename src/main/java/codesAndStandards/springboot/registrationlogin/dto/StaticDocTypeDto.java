package codesAndStandards.springboot.registrationlogin.dto;

import java.util.List;

public class StaticDocTypeDto {

    // ── Summary fields (always present) ──────────────────────────
    private Long   docTypeId;
    private String docTypeName;
    private String description;
    private int    fieldCount;
    private int    stateCount;
    private int    transitionCount;
    private int    peopleCount;
    private int    customTypeCount;

    // ── Detail fields (populated on GET /{id}) ───────────────────
    private List<MetaFieldEntry>    metadataDefinitions;
    private List<StateEntry>        states;
    private List<PeopleEntry>       people;
    private List<TransitionEntry>   transitions;

    // ── Payload fields (used on POST/PUT) ────────────────────────
    private List<Long>              metadataFieldIds;
    private List<Long>              mandatoryFieldIds;
    private List<Long>              stateIds;
    private List<Long>              peopleIds;
    private List<TransitionPayload> transitionPayloads;

    // ─── nested DTOs ─────────────────────────────────────────────

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

    public static class PeopleEntry {
        public Long   userId;
        public String username;
        public String email;
        public String fullName;
    }

    public static class TransitionEntry {
        public Long   id;
        public String transitionName;
        public Long   fromStateId;
        public String fromStateName;
        public Long   toStateId;
        public String toStateName;
        public Long   lcRoleId;      // LifeCycleRoles.lc_role_id
        public String roleName;
    }

    public static class TransitionPayload {
        public Long   id;            // null = new, non-null = existing
        public Long   fromStateId;
        public Long   toStateId;
        public Long   lcRoleId;      // LifeCycleRoles.lc_role_id (service resolves to StaticDocTypeRoles.id)
        public String transitionName;
    }

    // ─── getters / setters ───────────────────────────────────────
    public Long   getDocTypeId()                              { return docTypeId; }
    public void   setDocTypeId(Long d)                        { this.docTypeId = d; }
    public String getDocTypeName()                            { return docTypeName; }
    public void   setDocTypeName(String d)                    { this.docTypeName = d; }
    public String getDescription()                            { return description; }
    public void   setDescription(String d)                    { this.description = d; }
    public int    getFieldCount()                             { return fieldCount; }
    public void   setFieldCount(int f)                        { this.fieldCount = f; }
    public int    getStateCount()                             { return stateCount; }
    public void   setStateCount(int s)                        { this.stateCount = s; }
    public int    getTransitionCount()                        { return transitionCount; }
    public void   setTransitionCount(int t)                   { this.transitionCount = t; }
    public int    getPeopleCount()                            { return peopleCount; }
    public void   setPeopleCount(int p)                       { this.peopleCount = p; }
    public int    getCustomTypeCount()                        { return customTypeCount; }
    public void   setCustomTypeCount(int c)                   { this.customTypeCount = c; }
    public List<MetaFieldEntry>    getMetadataDefinitions()   { return metadataDefinitions; }
    public void   setMetadataDefinitions(List<MetaFieldEntry> m) { this.metadataDefinitions = m; }
    public List<StateEntry>        getStates()                { return states; }
    public void   setStates(List<StateEntry> s)               { this.states = s; }
    public List<PeopleEntry>       getPeople()                { return people; }
    public void   setPeople(List<PeopleEntry> p)              { this.people = p; }
    public List<TransitionEntry>   getTransitions()           { return transitions; }
    public void   setTransitions(List<TransitionEntry> t)     { this.transitions = t; }
    public List<Long>              getMetadataFieldIds()       { return metadataFieldIds; }
    public void   setMetadataFieldIds(List<Long> m)            { this.metadataFieldIds = m; }
    public List<Long>              getMandatoryFieldIds()      { return mandatoryFieldIds; }
    public void   setMandatoryFieldIds(List<Long> m)           { this.mandatoryFieldIds = m; }
    public List<Long>              getStateIds()               { return stateIds; }
    public void   setStateIds(List<Long> s)                    { this.stateIds = s; }
    public List<Long>              getPeopleIds()              { return peopleIds; }
    public void   setPeopleIds(List<Long> p)                   { this.peopleIds = p; }
    public List<TransitionPayload> getTransitionPayloads()     { return transitionPayloads; }
    public void   setTransitionPayloads(List<TransitionPayload> t) { this.transitionPayloads = t; }
}