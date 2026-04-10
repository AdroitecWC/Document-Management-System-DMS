package codesAndStandards.springboot.registrationlogin.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class StoredProcedureRepository {

    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureRepository.class);

    private final EntityManager entityManager;

    public StoredProcedureRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Upload a new document.
     * New schema: Documents table has title, doc_type_id, uploader_user_id, file_extension.
     * File path is stored in DocumentVersion.
     * Tags and Classifications are linked via join tables.
     *
     * NOTE: You must update sp_UploadDocument in the database to match this new signature.
     */
    public Long uploadDocument(String title, Long docTypeId, String fileExtension,
                               String filePath, String versionNumber,
                               Long uploaderUserId,
                               String tagNames, String classificationNames) {

        logger.info("Executing stored procedure: sp_UploadDocument");

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_UploadDocument");

        query.registerStoredProcedureParameter("title", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("docTypeId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("fileExtension", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("filePath", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("versionNumber", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("uploaderUserId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("tagNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("classificationNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("newDocumentId", Long.class, ParameterMode.OUT);

        query.setParameter("title", title);
        query.setParameter("docTypeId", docTypeId);
        query.setParameter("fileExtension", fileExtension);
        query.setParameter("filePath", filePath);
        query.setParameter("versionNumber", versionNumber);
        query.setParameter("uploaderUserId", uploaderUserId);
        query.setParameter("tagNames", tagNames);
        query.setParameter("classificationNames", classificationNames);

        logger.debug("SP Parameters - Title: {}, DocTypeId: {}, Tags: {}, Classifications: {}",
                title, docTypeId, tagNames, classificationNames);

        query.execute();

        Long documentId = (Long) query.getOutputParameterValue("newDocumentId");

        logger.info("Stored procedure executed successfully. Returned Document ID: {}", documentId);

        return documentId;
    }

    /**
     * Delete a document and return its file paths (from DocumentVersion).
     *
     * NOTE: You must update sp_DeleteDocument in the database to match this new signature.
     * The SP should now return file paths from DocumentVersion table instead of Documents.
     */
    public Map<String, Object> deleteDocument(Long documentId) {

        logger.info("Executing stored procedure: sp_DeleteDocument for document ID: {}", documentId);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_DeleteDocument");

        query.registerStoredProcedureParameter("documentId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("filePaths", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("deleted", Boolean.class, ParameterMode.OUT);

        query.setParameter("documentId", documentId);

        query.execute();

        // filePaths may be comma-separated if multiple versions exist
        String filePaths = (String) query.getOutputParameterValue("filePaths");
        Boolean deleted = (Boolean) query.getOutputParameterValue("deleted");

        logger.info("Stored procedure executed. Document deleted: {}, File paths: {}", deleted, filePaths);

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted != null ? deleted : false);
        result.put("filePaths", filePaths);

        return result;
    }

    /**
     * Update a document's metadata.
     *
     * NOTE: You must update sp_UpdateDocument in the database to match this new signature.
     * Old fields (productCode, edition, publishDate, noOfPages, notes) are removed.
     */
    public boolean updateDocument(Long documentId, String title, Long docTypeId,
                                  String tagNames, String classificationNames) {

        logger.info("Executing stored procedure: sp_UpdateDocument for document ID: {}", documentId);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_UpdateDocument");

        query.registerStoredProcedureParameter("documentId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("title", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("docTypeId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("tagNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("classificationNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("updated", Boolean.class, ParameterMode.OUT);

        query.setParameter("documentId", documentId);
        query.setParameter("title", title);
        query.setParameter("docTypeId", docTypeId);
        query.setParameter("tagNames", tagNames);
        query.setParameter("classificationNames", classificationNames);

        logger.debug("SP Parameters - Title: {}, DocTypeId: {}, Tags: {}, Classifications: {}",
                title, docTypeId, tagNames, classificationNames);

        query.execute();

        Boolean updated = (Boolean) query.getOutputParameterValue("updated");

        logger.info("Stored procedure executed. Document updated: {}", updated);

        return updated != null && updated;
    }
}
