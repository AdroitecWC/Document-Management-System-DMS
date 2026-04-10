package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    /**
     * Find all metadata values for a specific document
     */
    List<DocumentMetadata> findByDocumentDocumentId(Long documentId);

    /**
     * Find a specific metadata value for a document + metadata field combination
     */
    Optional<DocumentMetadata> findByDocumentDocumentIdAndMetadataDefinitionMetadataId(Long documentId, Long metadataId);

    /**
     * Check if a metadata value exists for a document + metadata field
     */
    boolean existsByDocumentDocumentIdAndMetadataDefinitionMetadataId(Long documentId, Long metadataId);

    /**
     * Delete all metadata values for a document
     */
    void deleteByDocumentDocumentId(Long documentId);

    /**
     * Delete a specific metadata value for a document + metadata field
     */
    void deleteByDocumentDocumentIdAndMetadataDefinitionMetadataId(Long documentId, Long metadataId);

    /**
     * Find all documents that have a specific metadata field with a specific value
     */
    List<DocumentMetadata> findByMetadataDefinitionMetadataIdAndValue(Long metadataId, String value);
}
