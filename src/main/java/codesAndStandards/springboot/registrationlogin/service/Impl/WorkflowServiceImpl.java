package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.WorkflowDto;
import codesAndStandards.springboot.registrationlogin.entity.*;
import codesAndStandards.springboot.registrationlogin.repository.*;
import codesAndStandards.springboot.registrationlogin.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    @Autowired private WorkflowRepository            workflowRepo;
    @Autowired private WorkflowStateRepository       wfStateRepo;
    @Autowired private WorkflowTransitionRepository  transitionRepo;
    @Autowired private DocumentTypeRepository        docTypeRepo;
    @Autowired private DocumentTypeStateRepository   docTypeStateRepo;
    @Autowired private LifeCycleStateRepository      lcStateRepo;
    @Autowired private LifeCycleRoleRepository       lcRoleRepo;
    @Autowired private UserRepository                userRepo;

    // ─── helpers ─────────────────────────────────────────────────────

    private WorkflowDto toSummary(Workflow wf) {
        WorkflowDto dto = new WorkflowDto();
        dto.setWorkflowId(wf.getWorkflowId());
        dto.setDocTypeId(wf.getDocTypeId());
        dto.setWorkflowName(wf.getWorkflowName());
        dto.setDescription(wf.getDescription());
        dto.setActive(wf.isActive());
        dto.setStateCount(wfStateRepo.findByWorkflowId(wf.getWorkflowId()).size());
        dto.setTransitionCount(transitionRepo.findByWorkflowId(wf.getWorkflowId()).size());
        DocumentType parent = docTypeRepo.findById(wf.getDocTypeId()).orElse(null);
        dto.setParentTypeName(parent != null ? parent.getDocTypeName() : "Unknown");
        return dto;
    }

    private WorkflowDto toDetail(Workflow wf) {
        WorkflowDto dto = toSummary(wf);

        // ── selected states ──────────────────────────────────────────
        List<WorkflowState> wfStates = wfStateRepo.findByWorkflowId(wf.getWorkflowId());
        List<Long> stateIds = wfStates.stream().map(WorkflowState::getStateId).collect(Collectors.toList());
        Map<Long, LifeCycleState> stateMap = stateIds.isEmpty() ? Collections.emptyMap()
                : lcStateRepo.findAllById(stateIds).stream()
                .collect(Collectors.toMap(LifeCycleState::getStateId, s -> s));

        dto.setSelectedStates(wfStates.stream().map(ws -> {
            WorkflowDto.StateInfo si = new WorkflowDto.StateInfo();
            LifeCycleState lcs = stateMap.get(ws.getStateId());
            si.stateId   = ws.getStateId();
            si.stateName = lcs != null ? lcs.getStateName() : String.valueOf(ws.getStateId());
            si.color     = (lcs != null && lcs.getColor() != null) ? lcs.getColor() : "#6b7280";
            return si;
        }).collect(Collectors.toList()));

        // ── transitions ──────────────────────────────────────────────
        List<WorkflowTransition> transitions = transitionRepo.findByWorkflowId(wf.getWorkflowId());

        // collect all state IDs used in transitions for lookup
        Set<Long> allStateIds = new HashSet<>(stateIds);
        transitions.forEach(t -> { allStateIds.add(t.getFromStateId()); allStateIds.add(t.getToStateId()); });
        Map<Long, LifeCycleState> allStates = allStateIds.isEmpty() ? Collections.emptyMap()
                : lcStateRepo.findAllById(new ArrayList<>(allStateIds)).stream()
                .collect(Collectors.toMap(LifeCycleState::getStateId, s -> s));

        // role lookup
        List<Long> roleIds = transitions.stream().map(WorkflowTransition::getRoleId).distinct().collect(Collectors.toList());
        Map<Long, LifeCycleRole> roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                : lcRoleRepo.findAllById(roleIds).stream()
                .collect(Collectors.toMap(LifeCycleRole::getLcRoleId, r -> r));

        // user lookup
        List<Long> userIds = transitions.stream().map(WorkflowTransition::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        dto.setTransitions(transitions.stream().map(t -> {
            WorkflowDto.TransitionInfo ti = new WorkflowDto.TransitionInfo();
            ti.id = t.getId();
            LifeCycleState fs = allStates.get(t.getFromStateId());
            LifeCycleState ts = allStates.get(t.getToStateId());
            ti.fromStateId   = t.getFromStateId();
            ti.fromStateName = fs != null ? fs.getStateName() : String.valueOf(t.getFromStateId());
            ti.toStateId     = t.getToStateId();
            ti.toStateName   = ts != null ? ts.getStateName() : String.valueOf(t.getToStateId());
            LifeCycleRole role = roleMap.get(t.getRoleId());
            ti.roleId   = t.getRoleId();
            ti.roleName = role != null ? role.getRoleName() : "Unknown";
            User u = userMap.get(t.getUserId());
            ti.userId    = t.getUserId();
            ti.userName  = u != null ? u.getUsername() : "Unknown";
            ti.userEmail = u != null && u.getEmail() != null ? u.getEmail() : "";
            return ti;
        }).collect(Collectors.toList()));

        return dto;
    }

    // ─── public API ──────────────────────────────────────────────────

    @Override @Transactional(readOnly = true)
    public List<WorkflowDto> getAll() {
        return workflowRepo.findAll().stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public WorkflowDto getById(Long id) {
        Workflow wf = workflowRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        return toDetail(wf);
    }

    @Override @Transactional(readOnly = true)
    public List<WorkflowDto> getByDocType(Long docTypeId) {
        return workflowRepo.findByDocTypeId(docTypeId).stream()
                .map(this::toDetail).collect(Collectors.toList());
    }

    @Override @Transactional
    public WorkflowDto create(WorkflowDto dto, String username) {
        validate(dto, null);
        User actor = userRepo.findByUsername(username);

        Workflow wf = Workflow.builder()
                .docTypeId(dto.getDocTypeId())
                .workflowName(dto.getWorkflowName().trim())
                .description(dto.getDescription())
                .isActive(true)
                .createdBy(actor != null ? actor.getUserId() : 0L)
                .createdAt(LocalDateTime.now())
                .build();
        wf = workflowRepo.save(wf);

        saveStatesAndTransitions(wf.getWorkflowId(), dto, actor);
        return toDetail(wf);
    }

    @Override @Transactional
    public WorkflowDto update(Long id, WorkflowDto dto, String username) {
        Workflow wf = workflowRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        validate(dto, id);
        User actor = userRepo.findByUsername(username);

        wf.setWorkflowName(dto.getWorkflowName().trim());
        wf.setDescription(dto.getDescription());
        workflowRepo.save(wf);

        saveStatesAndTransitions(id, dto, actor);
        return toDetail(wf);
    }

    @Override @Transactional
    public void delete(Long id) {
        Workflow wf = workflowRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        workflowRepo.delete(wf);
    }

    // ─── private helpers ─────────────────────────────────────────────

    private void validate(WorkflowDto dto, Long excludeId) {
        if (dto.getDocTypeId() == null)
            throw new IllegalArgumentException("Parent document type is required");
        if (dto.getWorkflowName() == null || dto.getWorkflowName().isBlank())
            throw new IllegalArgumentException("Workflow name is required");
        boolean dup = excludeId == null
                ? workflowRepo.existsByDocTypeIdAndWorkflowName(dto.getDocTypeId(), dto.getWorkflowName().trim())
                : workflowRepo.existsByDocTypeIdAndWorkflowNameAndWorkflowIdNot(
                dto.getDocTypeId(), dto.getWorkflowName().trim(), excludeId);
        if (dup) throw new IllegalArgumentException("A workflow with this name already exists for this document type");
    }

    private void saveStatesAndTransitions(Long workflowId, WorkflowDto dto, User actor) {
        long actorId = actor != null ? actor.getUserId() : 0L;

        if (dto.getSelectedStateIds() != null) {
            wfStateRepo.deleteByWorkflowId(workflowId);
            for (Long stateId : dto.getSelectedStateIds()) {
                wfStateRepo.save(WorkflowState.builder()
                        .workflowId(workflowId).stateId(stateId).build());
            }
        }

        if (dto.getTransitionPayloads() != null) {
            transitionRepo.deleteByWorkflowId(workflowId);
            for (WorkflowDto.TransitionPayload tp : dto.getTransitionPayloads()) {
                if (tp.fromStateId == null || tp.toStateId == null
                        || tp.roleId == null || tp.userId == null) continue;
                transitionRepo.save(WorkflowTransition.builder()
                        .workflowId(workflowId)
                        .fromStateId(tp.fromStateId)
                        .toStateId(tp.toStateId)
                        .roleId(tp.roleId)
                        .userId(tp.userId)
                        .createdBy(actorId)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }
    }
}