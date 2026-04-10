package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    void saveDocument(DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception;

    List<DocumentDto> findAllDocuments();

    DocumentDto findDocumentById(Long id);

    void updateDocument(Long id, DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception;

    void deleteDocument(Long id);

    String getFilePath(Long id);

    List<DocumentDto> findDocumentsAccessibleByUser(Long userId);

    String getGroupNamesForDocument(Long id);

    boolean hasUserAccessToDocument(Long userId, Long documentId);

    List<Long> getAccessibleDocumentIds(Long userId);
}
