package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {
    List<WorkflowState> findByWorkflowId(Long workflowId);

    @Modifying
    @Query("DELETE FROM WorkflowState s WHERE s.workflowId = :workflowId")
    void deleteByWorkflowId(@Param("workflowId") Long workflowId);
}