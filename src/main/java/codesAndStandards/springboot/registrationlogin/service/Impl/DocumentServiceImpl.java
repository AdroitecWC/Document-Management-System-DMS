package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.DocumentDto;
import codesAndStandards.springboot.registrationlogin.entity.*;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.StoredProcedureRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.repository.AccessControlLogicRepository;
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

    @Autowired
    private ApplicationSettingsService settingsService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               UserRepository userRepository,
                               StoredProcedureRepository storedProcedureRepository,
                               GroupService groupService,
                               AccessControlLogicRepository accessControlLogicRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storedProcedureRepository = storedProcedureRepository;
        this.groupService = groupService;
        this.accessControlLogicRepository = accessControlLogicRepository;
    }

    // ⭐ Helper: extract extension without the dot, in UPPERCASE (e.g. "PDF", "DOCX")
    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "UNKNOWN";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
    }

    // ⭐ Helper: strip any file extension from a filename to produce a clean title
    // e.g. "My Document.docx" → "My Document"
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

        // Get max file size from settings
        Integer maxFileSizeMB = settingsService.getMaxFileSizeMB();
        long maxSizeBytes = maxFileSizeMB * 1024L * 1024L;

        // Check file size against settings
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException(String.format("File size (%dMB) exceeds maximum allowed size of %dMB",
                    file.getSize() / (1024 * 1024), maxFileSizeMB));
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

        // ⭐ Extract file type (e.g. "PDF", "DOCX", "XLSX") for storage
        String fileType = extractFileType(originalFileName);

        // Check if format is allowed
        if (!settingsService.isFormatAllowed(fileType)) {
            throw new RuntimeException(String.format("File format '%s' is not allowed. Allowed formats: %s",
                    fileType, String.join(", ", settingsService.getAllowedFormats())));
        }

        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = Paths.get(uploadDir, uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // ⭐ If title was auto-generated from filename in bulk upload, strip any extension
        if (documentDto.getTitle() != null && documentDto.getTitle().isEmpty()) {
            documentDto.setTitle(stripExtension(originalFileName));
        }

        // Build publishDate from year/month
        if (documentDto.getPublishYear() != null && !documentDto.getPublishYear().isEmpty()) {
            if (documentDto.getPublishMonth() != null && !documentDto.getPublishMonth().isEmpty()) {
                documentDto.setPublishDate(documentDto.getPublishYear() + "-" + documentDto.getPublishMonth());
            } else {
                documentDto.setPublishDate(documentDto.getPublishYear());
            }
        } else {
            documentDto.setPublishDate(null);
        }

        String publishDate = (documentDto.getPublishDate() != null && !documentDto.getPublishDate().isEmpty())
                ? documentDto.getPublishDate()
                : null;

        logger.info("Saving Doc -> Title: {}, FileType: {}, PublishDate: {}, Tags: {}, Classifications: {}, Groups: {}",
                documentDto.getTitle(), fileType, publishDate, documentDto.getTagNames(),
                documentDto.getClassificationNames(), groupIds);

        Integer documentId = storedProcedureRepository.uploadDocument(
                documentDto.getTitle(),
                documentDto.getProductCode(),
                documentDto.getEdition(),
                publishDate,
                documentDto.getNoOfPages(),
                documentDto.getNotes(),
                filePath.toString(),
                user.getId(),
                documentDto.getTagNames(),
                documentDto.getClassificationNames()
        );

        if (documentId == null) {
            throw new RuntimeException("Stored procedure failed to return document ID");
        }

        // ⭐ Save fileType to the document entity after stored procedure creates it
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setFileType(fileType);
            documentRepository.save(doc);
            logger.info("✅ Saved fileType='{}' for document ID={}", fileType, documentId);
        });

        logger.info("✅ Document uploaded successfully. ID = {}", documentId);

        // Link uploaded document to selected groups (if any)
        if (groupIds != null && !groupIds.trim().isEmpty()) {
            List<Long> groupIdList = Arrays.stream(groupIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            if (!groupIdList.isEmpty()) {
                for (Long groupId : groupIdList) {
                    try {
                        groupService.addDocumentToGroup(groupId, documentId);
                        logger.info("✅ Linked document {} to group {}", documentId, groupId);
                    } catch (Exception e) {
                        logger.error("❌ Failed to link document {} to group {}: {}",
                                documentId, groupId, e.getMessage());
                    }
                }
                logger.info("✅ Successfully linked document {} to {} groups", documentId, groupIdList.size());
            }
        } else {
            logger.info("ℹ️ No groups selected for document {}", documentId);
        }
    }

    @Override
    @Transactional
    public void updateDocument(Integer id, DocumentDto documentDto, MultipartFile file, String username, String groupIds) throws Exception {
        logger.info("Updating document ID: {}", id);

        String fileType = null;

        // Handle file if provided
        if (file != null && !file.isEmpty()) {

            // Get max file size from settings
            Integer maxFileSizeMB = settingsService.getMaxFileSizeMB();
            long maxSizeBytes = maxFileSizeMB * 1024L * 1024L;

            // Check file size against settings
            if (file.getSize() > maxSizeBytes) {
                throw new RuntimeException(String.format("File size (%dMB) exceeds maximum allowed size of %dMB",
                        file.getSize() / (1024 * 1024), maxFileSizeMB));
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));

            // ⭐ Extract file type for the new file
            fileType = extractFileType(originalFileName);

            // Check if format is allowed
            if (!settingsService.isFormatAllowed(fileType)) {
                throw new RuntimeException(String.format("File format '%s' is not allowed. Allowed formats: %s",
                        fileType, String.join(", ", settingsService.getAllowedFormats())));
            }

            File uploadDirectory = new File(uploadDir);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(uploadDir, uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            documentDto.setFilePath(filePath.toString());
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        if (documentDto.getFilePath() != null && !documentDto.getFilePath().isEmpty()) {
            document.setFilePath(documentDto.getFilePath());
        }

        // ⭐ Update fileType on the entity if a new file was uploaded
        if (fileType != null) {
            document.setFileType(fileType);
            logger.info("✅ Updated fileType='{}' for document ID={}", fileType, id);
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (documentDto.getPublishYear() != null && !documentDto.getPublishYear().isEmpty()) {
            if (documentDto.getPublishMonth() != null && !documentDto.getPublishMonth().isEmpty()) {
                documentDto.setPublishDate(documentDto.getPublishYear() + "-" + documentDto.getPublishMonth());
            } else {
                documentDto.setPublishDate(documentDto.getPublishYear());
            }
        } else {
            documentDto.setPublishDate(null);
        }

        String publishDate = (documentDto.getPublishDate() != null && !documentDto.getPublishDate().isEmpty())
                ? documentDto.getPublishDate()
                : null;

        logger.info("Updating Doc -> ID: {}, Title: {}, FileType: {}, PublishDate: {}, Tags: {}, Classifications: {}, Groups: {}",
                id, documentDto.getTitle(), fileType, publishDate, documentDto.getTagNames(),
                documentDto.getClassificationNames(), groupIds);

        boolean updated = storedProcedureRepository.updateDocument(
                id,
                documentDto.getTitle(),
                documentDto.getProductCode(),
                documentDto.getEdition(),
                publishDate,
                documentDto.getNoOfPages(),
                documentDto.getNotes(),
                documentDto.getTagNames(),
                documentDto.getClassificationNames()
        );

        if (!updated) {
            throw new RuntimeException("Document not found or update failed");
        }

        // Save fileType update to DB (separate from stored procedure)
        if (fileType != null) {
            documentRepository.save(document);
        }

        logger.info("✅ Document metadata updated successfully: {}", id);

        // Update group associations — remove all existing first
        try {
            accessControlLogicRepository.deleteByDocumentId(id);
            logger.info("✅ Removed existing group associations for document {}", id);
        } catch (Exception e) {
            logger.warn("⚠️ No existing group associations to remove for document {}: {}", id, e.getMessage());
        }

        // Add new group associations
        if (groupIds != null && !groupIds.trim().isEmpty()) {
            List<Long> groupIdList = Arrays.stream(groupIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            if (!groupIdList.isEmpty()) {
                for (Long groupId : groupIdList) {
                    try {
                        groupService.addDocumentToGroup(groupId, id);
                        logger.info("✅ Linked document {} to group {}", id, groupId);
                    } catch (Exception e) {
                        logger.error("❌ Failed to link document {} to group {}: {}",
                                id, groupId, e.getMessage());
                    }
                }
                logger.info("✅ Successfully updated group associations for document {}", id);
            }
        } else {
            logger.info("ℹ️ No groups selected for document {} (all associations removed)", id);
        }
    }

    public String getGroupNamesForDocument(Integer documentId) {
        try {
            List<AccessControlLogic> accessControls =
                    accessControlLogicRepository.findByDocumentId(documentId);

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
        return documentRepository.findAll()
                .stream().map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto findDocumentById(Integer id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return convertToDto(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Integer id) {
        logger.info("Deleting document ID: {}", id);

        Map<String, Object> result = storedProcedureRepository.deleteDocument(id);
        Boolean deleted = (Boolean) result.get("deleted");
        String filePath = (String) result.get("filePath");

        if (deleted == null || !deleted) {
            throw new RuntimeException("Failed to delete document");
        }

        if (filePath != null && !filePath.isEmpty()) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
                logger.info("Deleted physical file: {}", filePath);
            } catch (Exception e) {
                logger.error("Error deleting file: {}", filePath, e);
            }
        }
    }

    @Override
    public String getFilePath(Integer id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return document.getFilePath();
    }

    private DocumentDto convertToDto(Document document) {
        DocumentDto dto = new DocumentDto();

        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setProductCode(document.getProductCode());
        dto.setEdition(document.getEdition());

        if (document.getPublishDate() != null) {
            dto.setPublishDate(document.getPublishDate());
            String[] parts = document.getPublishDate().split("-");
            dto.setPublishYear(parts[0]);
            if (parts.length > 1) {
                dto.setPublishMonth(parts[1]);
            }
        }

        dto.setNoOfPages(document.getNoOfPages());
        dto.setNotes(document.getNotes());
        dto.setFilePath(document.getFilePath());

        // ⭐ Map fileType to DTO — fallback to "PDF" for legacy documents that predate this field
        dto.setFileType(document.getFileType() != null ? document.getFileType() : "PDF");

        if (document.getUploadedAt() != null) {
            dto.setUploadedAt(document.getUploadedAt()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        if (document.getUploadedBy() != null) {
            dto.setUploadedByUsername(document.getUploadedBy().getUsername());
        }

        dto.setTagNames(document.getTags().stream()
                .map(Tag::getTagName)
                .collect(Collectors.joining(",")));

        dto.setClassificationNames(document.getClassifications().stream()
                .map(Classification::getClassificationName)
                .collect(Collectors.joining(",")));

        return dto;
    }

    @Override
    public List<DocumentDto> findDocumentsAccessibleByUser(Long userId) {
        List<Document> docs = documentRepository.findDocumentsAccessibleByUser(userId);
        return docs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getAccessibleDocumentIds(Long userId) {
        try {
            logger.info("Getting accessible document IDs for user: {}", userId);
            List<Document> accessibleDocs = documentRepository.findDocumentsAccessibleByUser(userId);
            List<Integer> documentIds = accessibleDocs.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
            logger.info("User {} has access to {} documents", userId, documentIds.size());
            return documentIds;
        } catch (Exception e) {
            logger.error("Error getting accessible documents for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasUserAccessToDocument(Long userId, int documentId) {
        try {
            logger.info("=== ACCESS CHECK (Using Document Library Logic) ===");
            logger.info("User ID: {}, Document ID: {}", userId, documentId);
            List<Integer> accessibleDocIds = getAccessibleDocumentIds(userId);
            boolean hasAccess = accessibleDocIds.contains(documentId);
            if (hasAccess) {
                logger.info("✅ User {} HAS access to document {}", userId, documentId);
            } else {
                logger.warn("❌ User {} DOES NOT have access to document {}", userId, documentId);
            }
            return hasAccess;
        } catch (Exception e) {
            logger.error("❌ ERROR checking document access for user {} and document {}: {}",
                    userId, documentId, e.getMessage(), e);
            return false;
        }
    }
}