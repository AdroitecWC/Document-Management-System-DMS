package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find documents uploaded by a specific user (by username)
     */
    List<Document> findByUploaderUser_Username(String username);

    /**
     * Find documents uploaded by a specific user (by userId)
     */
    List<Document> findByUploaderUser_UserId(Long userId);

    /**
     * Find document with its document type eagerly loaded
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.documentType WHERE d.documentId = :id")
    Optional<Document> findByIdWithDocumentType(@Param("id") Long id);

    /**
     * Find document with uploader user eagerly loaded
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploaderUser WHERE d.documentId = :id")
    Optional<Document> findByIdWithUploader(@Param("id") Long id);

    /**
     * Find document with all associations eagerly loaded
     */
    @Query("SELECT d FROM Document d " +
            "JOIN FETCH d.documentType " +
            "LEFT JOIN FETCH d.uploaderUser " +
            "WHERE d.documentId = :id")
    Optional<Document> findByIdWithAll(@Param("id") Long id);

    /**
     * Find all documents with document type eagerly loaded
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.documentType")
    List<Document> findAllWithDocumentType();

    /**
     * Find all documents with document type and uploader
     */
    @Query("SELECT d FROM Document d " +
            "JOIN FETCH d.documentType " +
            "LEFT JOIN FETCH d.uploaderUser")
    List<Document> findAllWithDocumentTypeAndUploader();

    /**
     * Find documents by tag (through DocumentTag join table)
     */
    @Query("SELECT d FROM Document d " +
            "JOIN DocumentTag dt ON d.documentId = dt.document.documentId " +
            "WHERE dt.tag.tagId = :tagId")
    List<Document> findByTagId(@Param("tagId") Long tagId);

    /**
     * Find documents by classification (through DocumentClassification join table)
     */
    @Query("SELECT d FROM Document d " +
            "JOIN DocumentClassification dc ON d.documentId = dc.document.documentId " +
            "WHERE dc.classification.classificationId = :classificationId")
    List<Document> findByClassificationId(@Param("classificationId") Long classificationId);

    /**
     * Find documents accessible by a user (through AccessControlLogic + GroupUser)
     */
    @Query("SELECT DISTINCT d FROM Document d " +
            "JOIN AccessControlLogic acl ON d.documentId = acl.document.documentId " +
            "JOIN GroupUser gu ON acl.group.groupId = gu.group.groupId " +
            "WHERE gu.user.userId = :userId")
    List<Document> findDocumentsAccessibleByUser(@Param("userId") Long userId);

    /**
     * Find documents by document type
     */
    List<Document> findByDocumentType_DocTypeId(Long docTypeId);

    /**
     * Find documents by file extension
     */
    List<Document> findByFileExtension(String fileExtension);

    /**
     * Search documents by title (case-insensitive)
     */
    @Query("SELECT d FROM Document d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Document> searchByTitle(@Param("searchTerm") String searchTerm);

    /**
     * Count documents by document type
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.documentType.docTypeId = :docTypeId")
    long countByDocumentType(@Param("docTypeId") Long docTypeId);
}
