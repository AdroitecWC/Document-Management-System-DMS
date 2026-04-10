package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.DocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentTagRepository extends JpaRepository<DocumentTag, DocumentTag.DocumentTagId> {

    /**
     * Find all tags for a document
     */
    @Query("SELECT dt FROM DocumentTag dt " +
            "JOIN FETCH dt.tag " +
            "WHERE dt.document.documentId = :documentId")
    List<DocumentTag> findByDocumentId(@Param("documentId") Long documentId);

    /**
     * Find all documents for a tag
     */
    @Query("SELECT dt FROM DocumentTag dt " +
            "JOIN FETCH dt.document " +
            "WHERE dt.tag.tagId = :tagId")
    List<DocumentTag> findByTagId(@Param("tagId") Long tagId);

    /**
     * Check if a document-tag association exists
     */
    @Query("SELECT CASE WHEN COUNT(dt) > 0 THEN true ELSE false END " +
            "FROM DocumentTag dt " +
            "WHERE dt.document.documentId = :documentId AND dt.tag.tagId = :tagId")
    boolean existsByDocumentIdAndTagId(@Param("documentId") Long documentId,
                                       @Param("tagId") Long tagId);

    /**
     * Delete all tags for a document
     */
    @Modifying
    @Query("DELETE FROM DocumentTag dt WHERE dt.document.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * Delete all documents for a tag
     */
    @Modifying
    @Query("DELETE FROM DocumentTag dt WHERE dt.tag.tagId = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);

    /**
     * Delete a specific document-tag association
     */
    @Modifying
    @Query("DELETE FROM DocumentTag dt " +
            "WHERE dt.document.documentId = :documentId AND dt.tag.tagId = :tagId")
    void deleteByDocumentIdAndTagId(@Param("documentId") Long documentId,
                                     @Param("tagId") Long tagId);

    /**
     * Get tag IDs for a document
     */
    @Query("SELECT dt.tag.tagId FROM DocumentTag dt WHERE dt.document.documentId = :documentId")
    List<Long> findTagIdsByDocumentId(@Param("documentId") Long documentId);

    /**
     * Get document IDs for a tag
     */
    @Query("SELECT dt.document.documentId FROM DocumentTag dt WHERE dt.tag.tagId = :tagId")
    List<Long> findDocumentIdsByTagId(@Param("tagId") Long tagId);

    /**
     * Count documents with a tag
     */
    @Query("SELECT COUNT(dt) FROM DocumentTag dt WHERE dt.tag.tagId = :tagId")
    long countByTagId(@Param("tagId") Long tagId);
}
