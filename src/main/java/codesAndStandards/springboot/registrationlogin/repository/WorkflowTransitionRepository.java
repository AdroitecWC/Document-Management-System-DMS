package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    List<WorkflowTransition> findByWorkflowId(Long workflowId);

    @Modifying
    @Query("DELETE FROM WorkflowTransition t WHERE t.workflowId = :workflowId")
    void deleteByWorkflowId(@Param("workflowId") Long workflowId);

    List<WorkflowTransition> findByUserId(Long userId);

}