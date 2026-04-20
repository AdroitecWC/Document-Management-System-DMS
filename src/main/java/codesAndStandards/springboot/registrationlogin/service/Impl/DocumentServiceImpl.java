package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.ClassificationDto;
import codesAndStandards.springboot.registrationlogin.dto.DocumentDto;
import codesAndStandards.springboot.registrationlogin.dto.DocumentVersionDto;
import codesAndStandards.springboot.registrationlogin.dto.TagDto;
import codesAndStandards.springboot.registrationlogin.entity.*;
import codesAndStandards.springboot.registrationlogin.repository.*;
import codesAndStandards.springboot.registrationlogin.service.ApplicationSettingsService;
import codesAndStandards.springboot.registrationlogin.service.DocumentService;
import codesAndStandards.springboot.registrationlogin.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StoredProcedureRepository storedProcedureRepository;
    private final GroupService groupService;
    private final AccessControlLogicRepository accessControlLogicRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentTagRepository documentTagRepository;
    private final DocumentClassificationRepository documentClassificationRepository;

    @Autowired
    private ApplicationSettingsService settingsService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               UserRepository userRepository,
                               StoredProcedureRepository storedProcedureRepository,
                               GroupService groupService,
                               AccessControlLogicRepository accessControlLogicRepository,
                               DocumentVersionRepository documentVersionRepository,
                               DocumentTypeRepository documentTypeRepository,
                               DocumentTagRepository documentTagRepository,
                               DocumentClassificationRepository documentClassificationRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storedProcedureRepository = storedProcedureRepository;
        this.groupService = groupService;
        this.accessControlLogicRepository = accessControlLogicRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.documentTagRepository = documentTagRepository;
        this.documentClassificationRepository = documentClassificationRepository;
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "UNKNOWN";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
    }

    private String stripExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    @Override
    @Transactional
    public void saveDocument(DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception {

        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        Integer maxFileSizeMB = settingsService.getMaxFileSizeMB();
        long maxSizeBytes = maxFileSizeMB * 1024L * 1024L;

        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException(String.format("File size (%dMB) exceeds maximum allowed size of %dMB",
                    file.getSize() / (1024 * 1024), maxFileSizeMB));
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = extractFileExtension(originalFileName);
        String fileType = extractFileType(originalFileName);

        if (!settingsService.isFormatAllowed(fileType)) {
            throw new RuntimeException(String.format("File format '%s' is not allowed. Allowed formats: %s",
                    fileType, String.join(", ", settingsService.getAllowedFormats())));
        }

        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        Path filePath = Paths.get(uploadDir, uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (documentDto.getTitle() == null || documentDto.getTitle().isEmpty()) {
            documentDto.setTitle(stripExtension(originalFileName));
        }

        String versionNumber = documentDto.getVersionNumber();
        if (versionNumber == null || versionNumber.isEmpty()) {
            versionNumber = "1.0";
        }

        logger.info("Saving Doc -> Title: {}, DocTypeId: {}, FileExt: {}, Tags: {}, Classifications: {}, Groups: {}",
                documentDto.getTitle(), documentDto.getDocTypeId(), fileExtension,
                documentDto.getTagNames(), documentDto.getClassificationNames(), groupIds);

        Long documentId = storedProcedureRepository.uploadDocument(
                documentDto.getTitle(),
                documentDto.getDocTypeId(),
                fileExtension,
                filePath.toString(),
                versionNumber,
                user.getUserId(),
                documentDto.getTagNames(),
                documentDto.getClassificationNames()
        );

        if (documentId == null) {
            throw new RuntimeException("Stored procedure failed to return document ID");
        }

        logger.info("Document uploaded successfully. ID = {}", documentId);

        // Link to groups
        if (groupIds != null && !groupIds.trim().isEmpty()) {
            List<Long> groupIdList = Arrays.stream(groupIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            for (Long groupId : groupIdList) {
                try {
                    groupService.addDocumentToGroup(groupId, documentId);
                    logger.info("Linked document {} to group {}", documentId, groupId);
                } catch (Exception e) {
                    logger.error("Failed to link document {} to group {}: {}", documentId, groupId, e.getMessage());
                }
            }
        }
    }

    @Override
    @Transactional
    public void updateDocument(Long id, DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception {
        logger.info("Updating document ID: {}", id);

        // Handle new file upload as a new version
        if (file != null && !file.isEmpty()) {
            Integer maxFileSizeMB = settingsService.getMaxFileSizeMB();
            long maxSizeBytes = maxFileSizeMB * 1024L * 1024L;

            if (file.getSize() > maxSizeBytes) {
                throw new RuntimeException(String.format("File size (%dMB) exceeds maximum allowed size of %dMB",
                        file.getSize() / (1024 * 1024), maxFileSizeMB));
            }


            String originalFileName = file.getOriginalFilename();
            String fileExtension = extractFileExtension(originalFileName);
            String fileType = extractFileType(originalFileName);

            if (!settingsService.isFormatAllowed(fileType)) {
                throw new RuntimeException(String.format("File format '%s' is not allowed. Allowed formats: %s",
                        fileType, String.join(", ", settingsService.getAllowedFormats())));
            }

            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
            Path filePath = Paths.get(uploadDir, uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Create a new version entry
            Document document = documentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

            // Update file extension on document if changed
            document.setFileExtension(fileExtension);
            documentRepository.save(document);

            String versionNumber = documentDto.getVersionNumber();
            if (versionNumber == null || versionNumber.isEmpty()) {
                long versionCount = documentVersionRepository.countByDocumentId(id);
                versionNumber = String.valueOf(versionCount + 1) + ".0";
            }

            DocumentVersion newVersion = DocumentVersion.builder()
                    .document(document)
                    .versionNumber(versionNumber)
                    .filePath(filePath.toString())
                    .build();
            documentVersionRepository.save(newVersion);

            logger.info("New version {} created for document {}", versionNumber, id);
        }
        // Update version number on existing latest version if no new file uploaded
        if (file == null || file.isEmpty()) {
            String versionNumber = documentDto.getVersionNumber();
            if (versionNumber != null && !versionNumber.trim().isEmpty()) {
                documentVersionRepository.findLatestByDocumentId(id).ifPresent(latestVersion -> {
                    latestVersion.setVersionNumber(versionNumber.trim());
                    documentVersionRepository.save(latestVersion);
                    logger.info("Updated version number to '{}' for document {}", versionNumber, id);
                });
            }
        }

        // Update metadata via stored procedure
        boolean updated = storedProcedureRepository.updateDocument(
                id,
                documentDto.getTitle(),
                documentDto.getDocTypeId(),
                documentDto.getTagNames(),
                documentDto.getClassificationNames()
        );

        if (!updated) {
            throw new RuntimeException("Document not found or update failed");
        }

        logger.info("Document metadata updated successfully: {}", id);

        // Update group associations
        try {
            accessControlLogicRepository.deleteByDocumentId(id);
        } catch (Exception e) {
            logger.warn("No existing group associations to remove for document {}: {}", id, e.getMessage());
        }

        if (groupIds != null && !groupIds.trim().isEmpty()) {
            List<Long> groupIdList = Arrays.stream(groupIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            for (Long groupId : groupIdList) {
                try {
                    groupService.addDocumentToGroup(groupId, id);
                    logger.info("Linked document {} to group {}", id, groupId);
                } catch (Exception e) {
                    logger.error("Failed to link document {} to group {}: {}", id, groupId, e.getMessage());
                }
            }
        }
    }

    @Override
    public String getGroupNamesForDocument(Long documentId) {
        try {
            List<AccessControlLogic> accessControls = accessControlLogicRepository.findByDocumentId(documentId);
            if (accessControls == null || accessControls.isEmpty()) {
                return "";
            }
            return accessControls.stream()
                    .map(ac -> ac.getGroup().getGroupName())
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            logger.warn("Failed to fetch groups for document {}: {}", documentId, e.getMessage());
            return "";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> findAllDocuments() {
        return documentRepository.findAllWithDocumentTypeAndUploader()
                .stream().map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto findDocumentById(Long id) {
        Document document = documentRepository.findByIdWithAll(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return convertToDto(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        logger.info("Deleting document ID: {}", id);

        // Get all file paths before deletion
        List<String> filePaths = documentVersionRepository.findFilePathsByDocumentId(id);

        Map<String, Object> result = storedProcedureRepository.deleteDocument(id);
        Boolean deleted = (Boolean) result.get("deleted");

        if (deleted == null || !deleted) {
            throw new RuntimeException("Failed to delete document");
        }

        // Delete physical files for all versions
        for (String filePath : filePaths) {
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    Files.deleteIfExists(Paths.get(filePath));
                    logger.info("Deleted physical file: {}", filePath);
                } catch (Exception e) {
                    logger.error("Error deleting file: {}", filePath, e);
                }
            }
        }
    }

    @Override
    public String getFilePath(Long id) {
        // Return the latest version's file path
        DocumentVersion latestVersion = documentVersionRepository.findLatestByDocumentId(id)
                .orElseThrow(() -> new RuntimeException("No version found for document " + id));
        return latestVersion.getFilePath();
    }

    private DocumentDto convertToDto(Document document) {
        DocumentDto dto = new DocumentDto();

        dto.setId(document.getDocumentId());
        dto.setTitle(document.getTitle());
        dto.setFileExtension(document.getFileExtension());

        // Document Type
        if (document.getDocumentType() != null) {
            dto.setDocTypeId(document.getDocumentType().getDocTypeId());
            dto.setDocTypeName(document.getDocumentType().getDocTypeName());
        }

        // Uploader info
        if (document.getCreatedAt() != null) {
            dto.setUploadedAt(document.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (document.getUploaderUser() != null) {
            dto.setUploadedByUsername(document.getUploaderUser().getUsername());
            dto.setUploadedByUserId(document.getUploaderUser().getUserId());
        }

        // Get latest version file path
        documentVersionRepository.findLatestByDocumentId(document.getDocumentId())
                .ifPresent(version -> {
                    dto.setFilePath(version.getFilePath());
                    dto.setVersionNumber(version.getVersionNumber());
                    dto.setCurrentVersionId(version.getVersionId());
                });

        // Get all versions
        List<DocumentVersion> versions = documentVersionRepository.findByDocumentId(document.getDocumentId());
        dto.setVersions(versions.stream().map(v -> DocumentVersionDto.builder()
                .versionId(v.getVersionId())
                .documentId(document.getDocumentId())
                .versionNumber(v.getVersionNumber())
                .filePath(v.getFilePath())
                .build()).collect(Collectors.toList()));

        // Tags (via join table)
        List<DocumentTag> docTags = documentTagRepository.findByDocumentId(document.getDocumentId());
        dto.setTagNames(docTags.stream()
                .map(dt -> dt.getTag().getTagName())
                .collect(Collectors.joining(",")));
        dto.setTags(docTags.stream().map(dt -> {
            TagDto tagDto = new TagDto();
            tagDto.setId(dt.getTag().getTagId());
            tagDto.setTagName(dt.getTag().getTagName());
            return tagDto;
        }).collect(Collectors.toList()));

        // Classifications (via join table)
        List<DocumentClassification> docClassifications = documentClassificationRepository.findByDocumentId(document.getDocumentId());
        dto.setClassificationNames(docClassifications.stream()
                .map(dc -> dc.getClassification().getClassificationName())
                .collect(Collectors.joining(",")));
        dto.setClassifications(docClassifications.stream().map(dc -> {
            ClassificationDto classDto = new ClassificationDto();
            classDto.setId(dc.getClassification().getClassificationId());
            classDto.setClassificationName(dc.getClassification().getClassificationName());
            return classDto;
        }).collect(Collectors.toList()));

        return dto;
    }
    @Transactional
    @Override
    public List<DocumentDto> findDocumentsAccessibleByUser(Long userId) {
        List<Document> docs = documentRepository.findDocumentsAccessibleByUser(userId);
        return docs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getAccessibleDocumentIds(Long userId) {
        try {
            logger.info("Getting accessible document IDs for user: {}", userId);
            List<Document> accessibleDocs = documentRepository.findDocumentsAccessibleByUser(userId);
            List<Long> documentIds = accessibleDocs.stream()
                    .map(Document::getDocumentId)
                    .collect(Collectors.toList());
            logger.info("User {} has access to {} documents", userId, documentIds.size());
            return documentIds;
        } catch (Exception e) {
            logger.error("Error getting accessible documents for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasUserAccessToDocument(Long userId, Long documentId) {
        try {
            List<Long> accessibleDocIds = getAccessibleDocumentIds(userId);
            return accessibleDocIds.contains(documentId);
        } catch (Exception e) {
            logger.error("Error checking document access for user {} and document {}: {}",
                    userId, documentId, e.getMessage(), e);
            return false;
        }
    }
}
