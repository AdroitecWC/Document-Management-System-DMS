package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    Optional<DocumentType> findByDocTypeName(String docTypeName);

    boolean existsByDocTypeName(String docTypeName);

    // Add this one method
    @Query("SELECT CASE WHEN COUNT(dt) > 0 THEN true ELSE false END " +
            "FROM DocumentType dt WHERE dt.docTypeName = :name AND dt.docTypeId <> :excludeId")
    boolean existsByDocTypeNameAndDocTypeIdNot(@Param("name") String name,
                                               @Param("excludeId") Long excludeId);
}
