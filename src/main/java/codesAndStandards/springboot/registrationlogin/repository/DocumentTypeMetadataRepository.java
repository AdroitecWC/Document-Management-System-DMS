package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentTypeMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentTypeMetadataRepository extends JpaRepository<DocumentTypeMetadata, DocumentTypeMetadata.DocumentTypeMetadataId> {

    /**
     * Find all metadata mappings for a document type
     */
    @Query("SELECT dtm FROM DocumentTypeMetadata dtm " +
            "JOIN FETCH dtm.metadataDefinition " +
            "WHERE dtm.documentType.docTypeId = :docTypeId")
    List<DocumentTypeMetadata> findByDocTypeId(@Param("docTypeId") Long docTypeId);

    /**
     * Find all document type mappings for a metadata definition
     */
    @Query("SELECT dtm FROM DocumentTypeMetadata dtm " +
            "JOIN FETCH dtm.documentType " +
            "WHERE dtm.metadataDefinition.metadataId = :metadataId")
    List<DocumentTypeMetadata> findByMetadataId(@Param("metadataId") Long metadataId);

    /**
     * Find a specific mapping
     */
    @Query("SELECT dtm FROM DocumentTypeMetadata dtm " +
            "WHERE dtm.documentType.docTypeId = :docTypeId AND dtm.metadataDefinition.metadataId = :metadataId")
    Optional<DocumentTypeMetadata> findByDocTypeIdAndMetadataId(@Param("docTypeId") Long docTypeId,
                                                                 @Param("metadataId") Long metadataId);

    /**
     * Check if mapping exists
     */
    @Query("SELECT CASE WHEN COUNT(dtm) > 0 THEN true ELSE false END " +
            "FROM DocumentTypeMetadata dtm " +
            "WHERE dtm.documentType.docTypeId = :docTypeId AND dtm.metadataDefinition.metadataId = :metadataId")
    boolean existsByDocTypeIdAndMetadataId(@Param("docTypeId") Long docTypeId, @Param("metadataId") Long metadataId);

    /**
     * Delete all metadata mappings for a document type
     */
    @Modifying
    @Query("DELETE FROM DocumentTypeMetadata dtm WHERE dtm.documentType.docTypeId = :docTypeId")
    void deleteByDocTypeId(@Param("docTypeId") Long docTypeId);

    /**
     * Delete all document type mappings for a metadata definition
     */
    @Modifying
    @Query("DELETE FROM DocumentTypeMetadata dtm WHERE dtm.metadataDefinition.metadataId = :metadataId")
    void deleteByMetadataId(@Param("metadataId") Long metadataId);

    /**
     * Find mandatory metadata for a document type
     */
    @Query("SELECT dtm FROM DocumentTypeMetadata dtm " +
            "JOIN FETCH dtm.metadataDefinition " +
            "WHERE dtm.documentType.docTypeId = :docTypeId AND dtm.mandatory = true")
    List<DocumentTypeMetadata> findMandatoryByDocTypeId(@Param("docTypeId") Long docTypeId);
}
