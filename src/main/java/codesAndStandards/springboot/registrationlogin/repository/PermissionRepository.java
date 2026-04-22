package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.Permission;
import codesAndStandards.springboot.registrationlogin.entity.PermissionId;
import codesAndStandards.springboot.registrationlogin.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, PermissionId> {
    
    @Query("SELECT p FROM Permission p JOIN FETCH p.action WHERE p.role = :role")
    List<Permission> findByRole(@Param("role") Role role);
}
