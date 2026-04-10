package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentClassificationRepository extends JpaRepository<DocumentClassification, DocumentClassification.DocumentClassificationId> {

    /**
     * Find all classifications for a document
     */
    @Query("SELECT dc FROM DocumentClassification dc " +
            "JOIN FETCH dc.classification " +
            "WHERE dc.document.documentId = :documentId")
    List<DocumentClassification> findByDocumentId(@Param("documentId") Long documentId);

    /**
     * Find all documents for a classification
     */
    @Query("SELECT dc FROM DocumentClassification dc " +
            "JOIN FETCH dc.document " +
            "WHERE dc.classification.classificationId = :classificationId")
    List<DocumentClassification> findByClassificationId(@Param("classificationId") Long classificationId);

    /**
     * Check if a document-classification association exists
     */
    @Query("SELECT CASE WHEN COUNT(dc) > 0 THEN true ELSE false END " +
            "FROM DocumentClassification dc " +
            "WHERE dc.document.documentId = :documentId AND dc.classification.classificationId = :classificationId")
    boolean existsByDocumentIdAndClassificationId(@Param("documentId") Long documentId,
                                                   @Param("classificationId") Long classificationId);

    /**
     * Delete all classifications for a document
     */
    @Modifying
    @Query("DELETE FROM DocumentClassification dc WHERE dc.document.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * Delete all documents for a classification
     */
    @Modifying
    @Query("DELETE FROM DocumentClassification dc WHERE dc.classification.classificationId = :classificationId")
    void deleteByClassificationId(@Param("classificationId") Long classificationId);

    /**
     * Delete a specific document-classification association
     */
    @Modifying
    @Query("DELETE FROM DocumentClassification dc " +
            "WHERE dc.document.documentId = :documentId AND dc.classification.classificationId = :classificationId")
    void deleteByDocumentIdAndClassificationId(@Param("documentId") Long documentId,
                                                @Param("classificationId") Long classificationId);

    /**
     * Get classification IDs for a document
     */
    @Query("SELECT dc.classification.classificationId FROM DocumentClassification dc WHERE dc.document.documentId = :documentId")
    List<Long> findClassificationIdsByDocumentId(@Param("documentId") Long documentId);

    /**
     * Get document IDs for a classification
     */
    @Query("SELECT dc.document.documentId FROM DocumentClassification dc WHERE dc.classification.classificationId = :classificationId")
    List<Long> findDocumentIdsByClassificationId(@Param("classificationId") Long classificationId);

    /**
     * Count documents in a classification
     */
    @Query("SELECT COUNT(dc) FROM DocumentClassification dc WHERE dc.classification.classificationId = :classificationId")
    long countByClassificationId(@Param("classificationId") Long classificationId);
}
