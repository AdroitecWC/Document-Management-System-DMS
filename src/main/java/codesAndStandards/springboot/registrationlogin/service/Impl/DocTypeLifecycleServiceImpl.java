package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDetailDto;
import codesAndStandards.springboot.registrationlogin.entity.*;
import codesAndStandards.springboot.registrationlogin.repository.*;
import codesAndStandards.springboot.registrationlogin.service.DocTypeLifecycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocTypeLifecycleServiceImpl implements DocTypeLifecycleService {

    @Autowired private DocumentTypeRepository         docTypeRepo;
    @Autowired private DocumentTypeStateRepository    stateRepo;
    @Autowired private DocumentTypeTeamRepository     teamRepo;
    @Autowired private DocumentTypeMetadataRepository metaRepo;
    @Autowired private MetadataDefinitionRepository   metaDefRepo;
    @Autowired private WorkflowRepository             workflowRepo;
    @Autowired private LifeCycleStateRepository       lcStateRepo;
    @Autowired private UserRepository                 userRepo;
    @Autowired private GroupRepository                groupRepo;
    @Autowired private GroupUserRepository            groupUserRepo;

    // ─── helpers ─────────────────────────────────────────────────────

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId",   u.getUserId());
        m.put("username", u.getUsername());
        m.put("email",    u.getEmail() != null ? u.getEmail() : "");
        return m;
    }

    private int countTeamMembers(Long docTypeId) {
        List<DocumentTypeTeamMember> team = teamRepo.findByDocTypeId(docTypeId);
        Set<Long> seen = new HashSet<>();
        for (DocumentTypeTeamMember member : team) {
            if (member.getUserId() != null) {
                seen.add(member.getUserId());
            } else if (member.getGroupId() != null) {
                groupUserRepo.findByGroupId(member.getGroupId()).forEach(gu -> {
                    if (gu.getUser() != null) seen.add(gu.getUser().getUserId());
                });
            }
        }
        return seen.size();
    }

    // ─── summaries ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DocumentTypeDetailDto> getAllSummaries() {
        return docTypeRepo.findAll().stream().map(this::toSummary).collect(Collectors.toList());
    }

    private DocumentTypeDetailDto toSummary(DocumentType dt) {
        DocumentTypeDetailDto dto = new DocumentTypeDetailDto();
        dto.setDocTypeId(dt.getDocTypeId());
        dto.setDocTypeName(dt.getDocTypeName());
        dto.setDescription(dt.getDescription());
        dto.setFieldCount(metaRepo.countByDocTypeId(dt.getDocTypeId()));
        dto.setStateCount(stateRepo.countByDocTypeId(dt.getDocTypeId()));
        dto.setWorkflowCount(workflowRepo.countByDocTypeId(dt.getDocTypeId()));

        // team member count = direct users + all members of assigned groups (deduplicated)
        dto.setTeamMemberCount(countTeamMembers(dt.getDocTypeId()));
        return dto;
    }

    // ─── full detail ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DocumentTypeDetailDto getFullDetail(Long docTypeId) {
        DocumentType dt = docTypeRepo.findById(docTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Document type not found: " + docTypeId));
        DocumentTypeDetailDto dto = toSummary(dt);

        // ── metadata ─────────────────────────────────────────────────
        List<DocumentTypeMetadata> dtMeta = metaRepo.findByDocTypeId(docTypeId);
        dto.setMetadataDefinitions(dtMeta.stream().map(m -> {
            DocumentTypeDetailDto.MetaFieldEntry e = new DocumentTypeDetailDto.MetaFieldEntry();
            e.metadataId = m.getMetadataDefinition().getMetadataId();
            e.fieldName  = m.getMetadataDefinition().getFieldName();
            e.fieldType  = m.getMetadataDefinition().getFieldType();
            e.mandatory  = Boolean.TRUE.equals(m.getMandatory());
            return e;
        }).collect(Collectors.toList()));

        // ── states ───────────────────────────────────────────────────
        List<DocumentTypeState> docStates = stateRepo.findByDocTypeIdOrderByDisplayOrder(docTypeId);
        List<Long> stateIds = docStates.stream().map(DocumentTypeState::getStateId).collect(Collectors.toList());
        Map<Long, LifeCycleState> stateMap = stateIds.isEmpty() ? Collections.emptyMap()
                : lcStateRepo.findAllById(stateIds).stream()
                .collect(Collectors.toMap(LifeCycleState::getStateId, s -> s));

        dto.setStates(docStates.stream().map(s -> {
            DocumentTypeDetailDto.StateEntry e = new DocumentTypeDetailDto.StateEntry();
            LifeCycleState lcs = stateMap.get(s.getStateId());
            e.stateId   = s.getStateId();
            e.stateName = lcs != null ? lcs.getStateName() : String.valueOf(s.getStateId());
            e.color     = (lcs != null && lcs.getColor() != null) ? lcs.getColor() : "#6b7280";
            e.isInitial = s.isInitial();
            e.isFinal   = s.isFinal();
            return e;
        }).collect(Collectors.toList()));

        // ── team groups ───────────────────────────────────────────────
        List<DocumentTypeTeamMember> team = teamRepo.findByDocTypeId(docTypeId);

        List<Long> assignedGroupIds = team.stream()
                .filter(t -> t.getGroupId() != null)
                .map(DocumentTypeTeamMember::getGroupId)
                .collect(Collectors.toList());
        Map<Long, Group> groupMap = assignedGroupIds.isEmpty() ? Collections.emptyMap()
                : groupRepo.findAllById(assignedGroupIds).stream()
                .collect(Collectors.toMap(Group::getGroupId, g -> g));

        dto.setTeamGroups(assignedGroupIds.stream().map(gId -> {
            DocumentTypeDetailDto.TeamGroupEntry e = new DocumentTypeDetailDto.TeamGroupEntry();
            Group g = groupMap.get(gId);
            e.groupId    = gId;
            e.groupName  = g != null ? g.getGroupName() : String.valueOf(gId);
            e.memberCount = (int) groupUserRepo.countByGroupId(gId);
            return e;
        }).collect(Collectors.toList()));

        // ── team direct users ─────────────────────────────────────────
        List<Long> directUserIds = team.stream()
                .filter(t -> t.getUserId() != null)
                .map(DocumentTypeTeamMember::getUserId)
                .collect(Collectors.toList());
        Map<Long, User> directUserMap = directUserIds.isEmpty() ? Collections.emptyMap()
                : userRepo.findAllById(directUserIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        dto.setTeamUsers(directUserIds.stream().map(uId -> {
            DocumentTypeDetailDto.TeamUserEntry e = new DocumentTypeDetailDto.TeamUserEntry();
            User u = directUserMap.get(uId);
            e.userId   = uId;
            e.username = u != null ? u.getUsername() : String.valueOf(uId);
            e.email    = u != null && u.getEmail() != null ? u.getEmail() : "";
            return e;
        }).collect(Collectors.toList()));

        return dto;
    }

    // ─── save full detail ─────────────────────────────────────────────

    @Override
    @Transactional
    public DocumentTypeDetailDto saveFullDetail(Long docTypeId, DocumentTypeDetailDto dto, String username) {
        DocumentType dt = docTypeRepo.findById(docTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Document type not found: " + docTypeId));

        // update basic info
        if (dto.getDocTypeName() != null && !dto.getDocTypeName().isBlank()) {
            dt.setDocTypeName(dto.getDocTypeName().trim());
        }
        dt.setDescription(dto.getDescription());
        docTypeRepo.save(dt);

        User actor = userRepo.findByUsername(username);
        long actorId = actor != null ? actor.getUserId() : 0L;

        // ── metadata ─────────────────────────────────────────────────
        if (dto.getMetadataFieldIds() != null) {
            metaRepo.deleteByDocTypeId(docTypeId);
            List<Long> mandatory = dto.getMandatoryFieldIds() != null
                    ? dto.getMandatoryFieldIds() : Collections.emptyList();
            DocumentType dtRef = docTypeRepo.getReferenceById(docTypeId);
            for (Long mId : dto.getMetadataFieldIds()) {
                MetadataDefinition mdRef = metaDefRepo.getReferenceById(mId);
                DocumentTypeMetadata.DocumentTypeMetadataId eid =
                        new DocumentTypeMetadata.DocumentTypeMetadataId(docTypeId, mId);
                DocumentTypeMetadata dtm = new DocumentTypeMetadata();
                dtm.setId(eid);
                dtm.setDocumentType(dtRef);
                dtm.setMetadataDefinition(mdRef);
                dtm.setMandatory(mandatory.contains(mId));
                metaRepo.save(dtm);
            }
        }

        // ── states ───────────────────────────────────────────────────
        boolean hasStateConfigs = dto.getStateConfigs() != null && !dto.getStateConfigs().isEmpty();
        boolean hasStateIds     = dto.getStateIds()     != null && !dto.getStateIds().isEmpty();

        if (hasStateConfigs || hasStateIds) {
            stateRepo.deleteByDocTypeId(docTypeId);
            if (hasStateConfigs) {
                // Preferred path — carries isInitial / isFinal per state
                for (int i = 0; i < dto.getStateConfigs().size(); i++) {
                    DocumentTypeDetailDto.StateConfigEntry cfg = dto.getStateConfigs().get(i);
                    DocumentTypeState s = DocumentTypeState.builder()
                            .docTypeId(docTypeId)
                            .stateId(cfg.stateId)
                            .displayOrder(i)
                            .isInitial(cfg.isInitial)
                            .isFinal(cfg.isFinal)
                            .build();
                    stateRepo.save(s);
                }
            } else {
                // Fallback — stateIds only, no I/F flags
                for (int i = 0; i < dto.getStateIds().size(); i++) {
                    DocumentTypeState s = DocumentTypeState.builder()
                            .docTypeId(docTypeId)
                            .stateId(dto.getStateIds().get(i))
                            .displayOrder(i)
                            .build();
                    stateRepo.save(s);
                }
            }
        }

        // ── team ─────────────────────────────────────────────────────
        boolean teamChanged = dto.getTeamGroupIds() != null || dto.getTeamUserIds() != null;
        if (teamChanged) {
            teamRepo.deleteByDocTypeId(docTypeId);
            if (dto.getTeamGroupIds() != null) {
                for (Long gId : dto.getTeamGroupIds()) {
                    DocumentTypeTeamMember m = DocumentTypeTeamMember.builder()
                            .docTypeId(docTypeId).groupId(gId)
                            .addedBy(actorId).addedAt(LocalDateTime.now()).build();
                    teamRepo.save(m);
                }
            }
            if (dto.getTeamUserIds() != null) {
                for (Long uId : dto.getTeamUserIds()) {
                    DocumentTypeTeamMember m = DocumentTypeTeamMember.builder()
                            .docTypeId(docTypeId).userId(uId)
                            .addedBy(actorId).addedAt(LocalDateTime.now()).build();
                    teamRepo.save(m);
                }
            }
        }

        return getFullDetail(docTypeId);
    }

    // ─── reference data ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableStates() {
        return lcStateRepo.findAll().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stateId",   s.getStateId());
            m.put("stateName", s.getStateName());
            m.put("color",     s.getColor() != null ? s.getColor() : "#6b7280");
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableGroups() {
        return groupRepo.findAll().stream().map(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("groupId",     g.getGroupId());
            m.put("groupName",   g.getGroupName());
            m.put("memberCount", (int) groupUserRepo.countByGroupId(g.getGroupId()));
            // members array — used by the group-members preview panel in the UI
            List<Map<String, Object>> members = groupUserRepo.findByGroupId(g.getGroupId()).stream()
                    .filter(gu -> gu.getUser() != null)
                    .map(gu -> userToMap(gu.getUser()))
                    .collect(Collectors.toList());
            m.put("members", members);
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTeamMembers(Long docTypeId) {
        List<DocumentTypeTeamMember> team = teamRepo.findByDocTypeId(docTypeId);
        Set<Long> allGroupMemberIds = new HashSet<>(); // track who's in any group
        List<Map<String, Object>> grouped = new ArrayList<>();

        // One optgroup per assigned group — same user CAN appear in multiple groups
        for (DocumentTypeTeamMember member : team) {
            if (member.getGroupId() == null) continue;
            Long gId = member.getGroupId();
            Group g = groupRepo.findById(gId).orElse(null);

            List<Map<String, Object>> members = new ArrayList<>();
            groupUserRepo.findByGroupId(gId).forEach(gu -> {
                if (gu.getUser() != null) {
                    allGroupMemberIds.add(gu.getUser().getUserId());
                    members.add(userToMap(gu.getUser()));
                }
            });

            if (!members.isEmpty()) {
                Map<String, Object> groupEntry = new LinkedHashMap<>();
                groupEntry.put("groupId",   gId);
                groupEntry.put("groupName", g != null ? g.getGroupName() : "Group " + gId);
                groupEntry.put("members",   members);
                grouped.add(groupEntry);
            }
        }

        // Individual (direct) users who are NOT in any assigned group
        List<Map<String, Object>> directMembers = new ArrayList<>();
        for (DocumentTypeTeamMember member : team) {
            if (member.getUserId() != null && !allGroupMemberIds.contains(member.getUserId())) {
                userRepo.findById(member.getUserId())
                        .ifPresent(u -> directMembers.add(userToMap(u)));
            }
        }
        if (!directMembers.isEmpty()) {
            Map<String, Object> directEntry = new LinkedHashMap<>();
            directEntry.put("groupId",   0);
            directEntry.put("groupName", "Individual Members");
            directEntry.put("members",   directMembers);
            grouped.add(directEntry);
        }

        return grouped;
    }



    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailablePeople() {
        return userRepo.findAll().stream()
                .map(this::userToMap)
                .collect(Collectors.toList());
    }
}