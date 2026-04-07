package codesAndStandards.springboot.registrationlogin.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Repository
public class StoredProcedureRepository {

    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureRepository.class);

    private final EntityManager entityManager;

    public StoredProcedureRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Integer uploadDocument(String title, String productCode, String edition,  // ✅ Changed return type from Long to Integer
                                  String publishDate, Integer noOfPages, String notes,
                                  String filePath, Long uploaderUserId,
                                  String tagNames, String classificationNames) {

        logger.info("Executing stored procedure: sp_UploadDocument");

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_UploadDocument");

        query.registerStoredProcedureParameter("title", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("productCode", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("edition", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("publishDate", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("noOfPages", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("notes", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("filePath", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("uploaderUserId", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("tagNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("classificationNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("newDocumentId", Integer.class, ParameterMode.OUT);

        query.setParameter("title", title);
        query.setParameter("productCode", productCode);
        query.setParameter("edition", edition);
        query.setParameter("publishDate", publishDate);
        query.setParameter("noOfPages", noOfPages);
        query.setParameter("notes", notes);
        query.setParameter("filePath", filePath);
        query.setParameter("uploaderUserId", uploaderUserId.intValue());
        query.setParameter("tagNames", tagNames);
        query.setParameter("classificationNames", classificationNames);

        logger.debug("SP Parameters - Title: {}, Tags: {}, Classifications: {}", title, tagNames, classificationNames);

        query.execute();

        Integer documentId = (Integer) query.getOutputParameterValue("newDocumentId");

        logger.info("Stored procedure executed successfully. Returned Document ID: {}", documentId);

        return documentId;  // ✅ Changed from documentId.longValue() to just documentId
    }

    public Map<String, Object> deleteDocument(Integer documentId) {  // ✅ Changed parameter from Long to Integer

        logger.info("Executing stored procedure: sp_DeleteDocument for document ID: {}", documentId);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_DeleteDocument");

        query.registerStoredProcedureParameter("documentId", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("filePath", String.class, ParameterMode.OUT);
        query.registerStoredProcedureParameter("deleted", Boolean.class, ParameterMode.OUT);

        query.setParameter("documentId", documentId);  // ✅ No need for .intValue() since it's already Integer

        query.execute();

        String filePath = (String) query.getOutputParameterValue("filePath");
        Boolean deleted = (Boolean) query.getOutputParameterValue("deleted");

        logger.info("Stored procedure executed. Document deleted: {}, File path: {}", deleted, filePath);

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted != null ? deleted : false);
        result.put("filePath", filePath);

        return result;
    }

    public boolean updateDocument(Integer documentId, String title, String productCode,  // ✅ Changed parameter from Long to Integer
                                  String edition, String publishDate, Integer noOfPages,
                                  String notes, String tagNames, String classificationNames) {

        logger.info("Executing stored procedure: sp_UpdateDocument for document ID: {}", documentId);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_UpdateDocument");

        query.registerStoredProcedureParameter("documentId", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("title", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("productCode", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("edition", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("publishDate", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("noOfPages", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("notes", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("tagNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("classificationNames", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("updated", Boolean.class, ParameterMode.OUT);

        query.setParameter("documentId", documentId);  // ✅ No need for .intValue() since it's already Integer
        query.setParameter("title", title);
        query.setParameter("productCode", productCode);
        query.setParameter("edition", edition);
        query.setParameter("publishDate", publishDate);
        query.setParameter("noOfPages", noOfPages);
        query.setParameter("notes", notes);
        query.setParameter("tagNames", tagNames);
        query.setParameter("classificationNames", classificationNames);

        logger.debug("SP Parameters - Title: {}, Tags: {}, Classifications: {}", title, tagNames, classificationNames);

        query.execute();

        Boolean updated = (Boolean) query.getOutputParameterValue("updated");

        logger.info("Stored procedure executed. Document updated: {}", updated);

        return updated != null && updated;
    }
}