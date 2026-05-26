package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    List<Workflow> findByDocTypeId(Long docTypeId);
    int countByDocTypeId(Long docTypeId);
    boolean existsByDocTypeIdAndWorkflowName(Long docTypeId, String workflowName);
    boolean existsByDocTypeIdAndWorkflowNameAndWorkflowIdNot(Long docTypeId, String name, Long excludeId);
}