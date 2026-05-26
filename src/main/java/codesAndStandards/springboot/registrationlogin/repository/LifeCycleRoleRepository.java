package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.LifeCycleRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LifeCycleRoleRepository extends JpaRepository<LifeCycleRole, Long> {

    boolean existsByRoleName(String roleName);

    boolean existsByRoleNameAndLcRoleIdNot(String roleName, Long lcRoleId);

    @Query(value = "SELECT COUNT(DISTINCT w.doc_type_id) FROM WorkflowTransitions wt INNER JOIN Workflows w ON wt.workflow_id = w.workflow_id WHERE wt.role_id = ?1", nativeQuery = true)
    Long countUsageInDocTypes(Long roleId);
}