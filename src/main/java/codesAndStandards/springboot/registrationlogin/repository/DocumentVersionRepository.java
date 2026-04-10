package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    /**
     * Find all versions for a document, ordered by version number
     */
    @Query("SELECT dv FROM DocumentVersion dv WHERE dv.document.documentId = :documentId ORDER BY dv.versionNumber ASC")
    List<DocumentVersion> findByDocumentId(@Param("documentId") Long documentId);

    /**
     * Find the latest version for a document (highest version_id)
     */
    @Query("SELECT dv FROM DocumentVersion dv WHERE dv.document.documentId = :documentId ORDER BY dv.versionId DESC")
    List<DocumentVersion> findByDocumentIdOrderByVersionIdDesc(@Param("documentId") Long documentId);

    /**
     * Get the latest version for a document
     */
    default Optional<DocumentVersion> findLatestByDocumentId(Long documentId) {
        List<DocumentVersion> versions = findByDocumentIdOrderByVersionIdDesc(documentId);
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }

    /**
     * Find a specific version by document and version number
     */
    @Query("SELECT dv FROM DocumentVersion dv WHERE dv.document.documentId = :documentId AND dv.versionNumber = :versionNumber")
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(@Param("documentId") Long documentId,
                                                               @Param("versionNumber") String versionNumber);

    /**
     * Count versions for a document
     */
    @Query("SELECT COUNT(dv) FROM DocumentVersion dv WHERE dv.document.documentId = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);

    /**
     * Delete all versions for a document
     */
    @Modifying
    @Query("DELETE FROM DocumentVersion dv WHERE dv.document.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * Get all file paths for a document (useful for cleanup when deleting)
     */
    @Query("SELECT dv.filePath FROM DocumentVersion dv WHERE dv.document.documentId = :documentId")
    List<String> findFilePathsByDocumentId(@Param("documentId") Long documentId);
}
