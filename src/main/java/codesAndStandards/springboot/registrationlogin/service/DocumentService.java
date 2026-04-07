package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentDto;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface DocumentService {
    // ✅ UPDATED: Added groupIds parameter
    void saveDocument(DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception;

    List<DocumentDto> findAllDocuments();

    DocumentDto findDocumentById(Integer id);

    // ✅ UPDATED: Added groupIds parameter
    void updateDocument(Integer id, DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception;

    void deleteDocument(Integer id);

    String getFilePath(Integer id);

    List<DocumentDto> findDocumentsAccessibleByUser(Long userId);

    String getGroupNamesForDocument(Integer id);
    /**
     * ⭐ NEW - Check if user has access to document
     */
    boolean hasUserAccessToDocument(Long userId, int documentId);
    /**
     * ⭐ NEW - Get list of accessible document IDs for a user
     * This uses the SAME logic as the document library
     */
    List<Integer> getAccessibleDocumentIds(Long userId);
}