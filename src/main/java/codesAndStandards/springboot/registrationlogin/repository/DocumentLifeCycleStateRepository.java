package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentLifeCycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentLifeCycleStateRepository extends JpaRepository<DocumentLifeCycleState, Long> {

    /**
     * Returns the most recent state entry for a document.
     * This is the document's CURRENT state.
     */
    Optional<DocumentLifeCycleState> findFirstByDocumentIdOrderByChangedAtDesc(Long documentId);

    /**
     * Finds the initial (Start) state for a given document type.
     * Adjust table/column names if yours differ.
     */
    @Query(value = """
            SELECT TOP 1 dts.state_id
            FROM DocumentTypeStates dts
            INNER JOIN Workflows w ON w.doc_type_id = dts.doc_type_id
            WHERE w.workflow_id = :workflowId
              AND dts.is_initial = 1
            """, nativeQuery = true)
    Optional<Long> findInitialStateIdForWorkflow(@Param("workflowId") Long workflowId);
}