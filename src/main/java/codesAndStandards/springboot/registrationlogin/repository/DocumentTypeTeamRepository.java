package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentTypeTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentTypeTeamRepository extends JpaRepository<DocumentTypeTeamMember, Long> {
    List<DocumentTypeTeamMember> findByDocTypeId(Long docTypeId);

    @Modifying
    @Query("DELETE FROM DocumentTypeTeamMember t WHERE t.docTypeId = :docTypeId")
    void deleteByDocTypeId(@Param("docTypeId") Long docTypeId);
}