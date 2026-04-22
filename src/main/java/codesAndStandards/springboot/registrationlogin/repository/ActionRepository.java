package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends JpaRepository<Action, Integer> {
}
