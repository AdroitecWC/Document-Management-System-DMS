package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//public interface RoleRepository extends JpaRepository<Role, Long> {
//    Role findByUsername(String username);
//}
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String roleName);
//    Optional<Role> findByUsername(String username);

    boolean existsByRoleName(String roleName);
//    boolean findByUsername(String username);
}
