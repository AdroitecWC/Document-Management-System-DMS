package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.WorkflowDto;
import java.util.List;

public interface WorkflowService {
    List<WorkflowDto> getAll();
    WorkflowDto       getById(Long id);
    List<WorkflowDto> getByDocType(Long docTypeId);   // for upload page (returns full detail)
    WorkflowDto       create(WorkflowDto dto, String username);
    WorkflowDto       update(Long id, WorkflowDto dto, String username);
    void              delete(Long id);
    List<WorkflowDto.TransitionInfo> getTransitionsByUser(Long userId);
}