package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.LifeCycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LifeCycleStateRepository extends JpaRepository<LifeCycleState, Long> {

    boolean existsByStateName(String stateName);

    boolean existsByStateNameAndStateIdNot(String stateName, Long stateId);

    @Query(value = "SELECT COUNT(DISTINCT doc_type_id) FROM DocumentTypeStates WHERE state_id = :stateId",
            nativeQuery = true)
    Long countUsageInDocTypes(@Param("stateId") Long stateId);
}