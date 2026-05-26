package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentTypeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentTypeStateRepository extends JpaRepository<DocumentTypeState, Long> {
    List<DocumentTypeState> findByDocTypeIdOrderByDisplayOrder(Long docTypeId);
    int countByDocTypeId(Long docTypeId);

    @Modifying
    @Query("DELETE FROM DocumentTypeState s WHERE s.docTypeId = :docTypeId")
    void deleteByDocTypeId(@Param("docTypeId") Long docTypeId);
}