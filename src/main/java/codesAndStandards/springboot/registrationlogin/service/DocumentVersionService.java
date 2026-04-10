package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentVersionDto;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.DocumentVersion;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVersionService {

    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<DocumentVersionDto> getVersionsByDocumentId(Long documentId) {
        return documentVersionRepository.findByDocumentId(documentId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentVersionDto getLatestVersion(Long documentId) {
        DocumentVersion version = documentVersionRepository.findLatestByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("No version found for document " + documentId));
        return mapToDto(version);
    }

    @Transactional(readOnly = true)
    public DocumentVersionDto getVersionById(Long versionId) {
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found with id: " + versionId));
        return mapToDto(version);
    }

    @Transactional
    public DocumentVersionDto createVersion(Long documentId, String versionNumber, String filePath) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + documentId));

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(versionNumber)
                .filePath(filePath)
                .build();

        DocumentVersion saved = documentVersionRepository.save(version);
        log.info("Created version {} for document {}", versionNumber, documentId);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteVersion(Long versionId) {
        documentVersionRepository.deleteById(versionId);
    }

    @Transactional(readOnly = true)
    public String getFilePath(Long versionId) {
        DocumentVersion version = documentVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return version.getFilePath();
    }

    @Transactional(readOnly = true)
    public List<String> getAllFilePathsForDocument(Long documentId) {
        return documentVersionRepository.findFilePathsByDocumentId(documentId);
    }

    private DocumentVersionDto mapToDto(DocumentVersion version) {
        return DocumentVersionDto.builder()
                .versionId(version.getVersionId())
                .documentId(version.getDocument().getDocumentId())
                .versionNumber(version.getVersionNumber())
                .filePath(version.getFilePath())
                .build();
    }
}
