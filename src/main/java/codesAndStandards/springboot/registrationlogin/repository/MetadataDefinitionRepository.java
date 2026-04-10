package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.MetadataDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataDefinitionRepository extends JpaRepository<MetadataDefinition, Long> {

    Optional<MetadataDefinition> findByFieldName(String fieldName);

    boolean existsByFieldName(String fieldName);

    /**
     * Find all metadata definitions for a specific document type (through DocumentTypeMetadata)
     */
    @Query("SELECT md FROM MetadataDefinition md " +
            "JOIN DocumentTypeMetadata dtm ON md.metadataId = dtm.metadataDefinition.metadataId " +
            "WHERE dtm.documentType.docTypeId = :docTypeId")
    List<MetadataDefinition> findByDocumentTypeId(@Param("docTypeId") Long docTypeId);

    /**
     * Find mandatory metadata definitions for a specific document type
     */
    @Query("SELECT md FROM MetadataDefinition md " +
            "JOIN DocumentTypeMetadata dtm ON md.metadataId = dtm.metadataDefinition.metadataId " +
            "WHERE dtm.documentType.docTypeId = :docTypeId AND dtm.mandatory = true")
    List<MetadataDefinition> findMandatoryByDocumentTypeId(@Param("docTypeId") Long docTypeId);
}
