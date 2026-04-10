package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.BulkUploadValidationResult;
import codesAndStandards.springboot.registrationlogin.dto.BulkUploadResult;
import codesAndStandards.springboot.registrationlogin.dto.DocumentMetadata;
import codesAndStandards.springboot.registrationlogin.dto.ExtractedMultipartFile;
import codesAndStandards.springboot.registrationlogin.entity.*;
import codesAndStandards.springboot.registrationlogin.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BulkUploadService {

    private static final Logger logger = LoggerFactory.getLogger(BulkUploadService.class);

    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentVersionRepository documentVersionRepository;
    @Autowired private DocumentTypeRepository documentTypeRepository;
    @Autowired private DocumentTagRepository documentTagRepository;
    @Autowired private DocumentClassificationRepository documentClassificationRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ClassificationRepository classificationRepository;
    @Autowired private ApplicationSettingsService settingsService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private String normalizeFilename(String name) {
        if (name == null) return null;
        return new File(name).getName().trim().toLowerCase();
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Validate a single document metadata entry
     */
    private boolean validateSingleDocument(
            DocumentMetadata metadata,
            Set<String> fileFilenames,
            BulkUploadValidationResult result,
            Set<String> invalidDocuments) {

        String filename = normalizeFilename(metadata.getFilename());
        boolean hasError = false;

        if (!fileFilenames.contains(filename)) {
            result.addError("Missing File", "Document '" + filename + "' file not found in upload");
            hasError = true;
        }

        if (isEmpty(metadata.getTitle())) {
            result.addError("Missing Title", "Document '" + filename + "' is missing title");
            hasError = true;
        }

        if (isEmpty(metadata.getDocTypeName())) {
            result.addError("Missing Document Type", "Document '" + filename + "' is missing document type");
            hasError = true;
        } else {
            // Verify doc type exists
            if (documentTypeRepository.findByDocTypeName(metadata.getDocTypeName().trim()).isEmpty()) {
                result.addError("Invalid Document Type",
                        "Document '" + filename + "' has unknown document type: " + metadata.getDocTypeName());
                hasError = true;
            }
        }

        if (hasError) {
            invalidDocuments.add(filename);
        }

        return !hasError;
    }

    /**
     * Generate Excel template from uploaded filenames
     */
    public ByteArrayOutputStream generateExcelTemplate(List<String> filenames, List<String> pageCounts) throws Exception {
        List<String> validFilenames = new ArrayList<>();

        if (filenames != null) {
            for (String filename : filenames) {
                if (filename != null && !filename.trim().isEmpty()) {
                    validFilenames.add(normalizeFilename(filename));
                }
            }
        }
        Collections.sort(validFilenames);

        int maxTagsPerDoc = 10;
        try {
            Integer settingValue = settingsService.getMaxTagsPerDocument();
            if (settingValue != null && settingValue > 0) maxTagsPerDoc = settingValue;
        } catch (Exception e) {
            logger.warn("Could not read maxTagsPerDocument, using default=10");
        }
        final int maxTagsForTemplate = maxTagsPerDoc;

        Workbook workbook = new XSSFWorkbook();

        // ============= SHEET 1: DOCUMENT METADATA =============
        Sheet sheet = workbook.createSheet("Documents");

        CellStyle headerStyleRequired = createHeaderStyle(workbook, IndexedColors.DARK_BLUE, true);
        CellStyle headerStyleOptional = createHeaderStyle(workbook, IndexedColors.GREY_50_PERCENT, false);

        // Fetch existing document types for reference
        List<String> docTypeNames = documentTypeRepository.findAll().stream()
                .map(DocumentType::getDocTypeName)
                .sorted()
                .collect(Collectors.toList());

        String tagsHeader = "Tags (comma-separated, max " + maxTagsForTemplate + ")";

        Object[][] columnDefs = {
                {"Filename *",            true},   // 0
                {"Title *",               true},   // 1
                {"Document Type *",       true},   // 2
                {"Version Number",        false},  // 3
                {tagsHeader,              false},  // 4
                {"Classifications (comma-separated)", false} // 5
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(22);

        for (int i = 0; i < columnDefs.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue((String) columnDefs[i][0]);
            boolean required = (Boolean) columnDefs[i][1];
            cell.setCellStyle(required ? headerStyleRequired : headerStyleOptional);
            sheet.setColumnWidth(i, (i <= 2) ? 40 * 256 : 35 * 256);
        }

        // Legend row
        Row legendRow = sheet.createRow(1);
        CellStyle legendStyle = workbook.createCellStyle();
        Font legendFont = workbook.createFont();
        legendFont.setItalic(true);
        legendFont.setColor(IndexedColors.DARK_RED.getIndex());
        legendFont.setFontHeightInPoints((short) 10);
        legendStyle.setFont(legendFont);
        legendStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        legendStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Cell legendCell = legendRow.createCell(0);
        legendCell.setCellValue("* = Required   |   Document Type must match an existing type   |   Max " + maxTagsForTemplate + " tags per document");
        legendCell.setCellStyle(legendStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnDefs.length - 1));

        // Example row
        Row exampleRow = sheet.createRow(2);
        CellStyle exampleStyle = workbook.createCellStyle();
        Font exampleFont = workbook.createFont();
        exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        exampleFont.setItalic(true);
        exampleStyle.setFont(exampleFont);

        String[] exampleData = {"document1.pdf", "Product Manual v2.1",
                docTypeNames.isEmpty() ? "Standard" : docTypeNames.get(0),
                "1.0", "manual,technical", "Engineering,Safety"};
        for (int i = 0; i < exampleData.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(exampleData[i]);
            cell.setCellStyle(exampleStyle);
        }

        // Data rows
        int rowNum = 3;
        for (String filename : validFilenames) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(filename);
            for (int i = 1; i < columnDefs.length; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        // ============= SHEET 2: REFERENCE DATA =============
        Sheet refSheet = workbook.createSheet("Reference Data");

        Row refHeader = refSheet.createRow(0);
        refHeader.createCell(0).setCellValue("Document Types");
        refHeader.getCell(0).setCellStyle(headerStyleRequired);
        refHeader.createCell(2).setCellValue("Available Tags");
        refHeader.getCell(2).setCellStyle(headerStyleOptional);
        refHeader.createCell(4).setCellValue("Available Classifications");
        refHeader.getCell(4).setCellStyle(headerStyleOptional);
        refSheet.setColumnWidth(0, 30 * 256);
        refSheet.setColumnWidth(2, 30 * 256);
        refSheet.setColumnWidth(4, 30 * 256);

        int r = 1;
        for (String dt : docTypeNames) {
            Row row = refSheet.getRow(r);
            if (row == null) row = refSheet.createRow(r);
            row.createCell(0).setCellValue(dt);
            r++;
        }

        List<String> existingTags = tagRepository.findAll().stream()
                .map(Tag::getTagName).filter(n -> n != null && !n.isEmpty()).sorted().distinct().collect(Collectors.toList());
        r = 1;
        for (String tag : existingTags) {
            Row row = refSheet.getRow(r);
            if (row == null) row = refSheet.createRow(r);
            row.createCell(2).setCellValue(tag);
            r++;
        }

        List<String> existingClassifications = classificationRepository.findAll().stream()
                .map(Classification::getClassificationName).filter(n -> n != null && !n.isEmpty()).sorted().distinct().collect(Collectors.toList());
        r = 1;
        for (String c : existingClassifications) {
            Row row = refSheet.getRow(r);
            if (row == null) row = refSheet.createRow(r);
            row.createCell(4).setCellValue(c);
            r++;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream;
    }

    private CellStyle createHeaderStyle(Workbook workbook, IndexedColors bgColor, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints((short) (bold ? 12 : 11));
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * Validate bulk upload
     */
    public BulkUploadValidationResult validateBulkUpload(
            MultipartFile[] files, List<String> fileFilenamesList,
            MultipartFile excelFile, String selfValidationJson) throws Exception {

        BulkUploadValidationResult result = new BulkUploadValidationResult();

        List<DocumentMetadata> metadataList =
                (selfValidationJson != null && !selfValidationJson.isEmpty())
                        ? parseJsonToMetadataList(selfValidationJson)
                        : parseExcelFile(excelFile);

        Set<String> fileFilenames = new HashSet<>();
        if (files != null && files.length > 0) {
            fileFilenames.addAll(extractFilenames(files));
        }
        if (fileFilenamesList != null && !fileFilenamesList.isEmpty()) {
            fileFilenames.addAll(fileFilenamesList.stream().map(this::normalizeFilename).collect(Collectors.toSet()));
        }

        Set<String> invalidDocuments = new HashSet<>();
        for (DocumentMetadata metadata : metadataList) {
            validateSingleDocument(metadata, fileFilenames, result, invalidDocuments);
        }

        Set<String> metadataFilenames = metadataList.stream()
                .map(m -> normalizeFilename(m.getFilename()))
                .collect(Collectors.toSet());

        for (String f : fileFilenames) {
            if (!metadataFilenames.contains(f)) {
                result.addWarning("Extra File", "File '" + f + "' uploaded but not in metadata");
            }
        }

        result.setTotalDocuments(metadataList.size());
        result.setValidDocuments(metadataList.size() - invalidDocuments.size());

        return result;
    }

    /**
     * Process bulk upload
     */
    public BulkUploadResult processBulkUpload(
            MultipartFile[] files, MultipartFile excelFile,
            String selfValidationJson, boolean uploadOnlyValid) throws Exception {

        BulkUploadResult result = new BulkUploadResult();

        List<DocumentMetadata> metadataList =
                (selfValidationJson != null && !selfValidationJson.isEmpty())
                        ? parseJsonToMetadataList(selfValidationJson)
                        : parseExcelFile(excelFile);

        if (uploadOnlyValid) {
            BulkUploadValidationResult validation = validateBulkUpload(files, null, excelFile, selfValidationJson);
            Set<String> invalidFiles = validation.getErrors().stream()
                    .map(Object::toString)
                    .filter(s -> s.contains("'"))
                    .map(s -> s.substring(s.indexOf("'") + 1, s.lastIndexOf("'")))
                    .map(this::normalizeFilename)
                    .collect(Collectors.toSet());

            metadataList = metadataList.stream()
                    .filter(m -> !invalidFiles.contains(normalizeFilename(m.getFilename())))
                    .collect(Collectors.toList());
        }

        Map<String, MultipartFile> fileMap = createFileMap(files);

        for (DocumentMetadata metadata : metadataList) {
            String filename = normalizeFilename(metadata.getFilename());
            MultipartFile file = fileMap.get(filename);

            if (file != null) {
                try {
                    uploadDocument(file, metadata);
                    result.addSuccess(filename, metadata.getTitle());
                } catch (Exception e) {
                    logger.error("Failed to upload document {}: {}", filename, e.getMessage());
                    result.addFailure(filename, e.getMessage());
                }
            } else {
                result.addFailure(filename, "File not found");
            }
        }

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void uploadDocument(MultipartFile file, DocumentMetadata metadata) throws Exception {
        User currentUser = getCurrentUser();
        if (currentUser == null) throw new Exception("User not authenticated");

        String originalFileName = new File(file.getOriginalFilename()).getName();
        String fileExtension = extractFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;
        Path filePath = Paths.get(uploadDir, uniqueFileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Resolve document type
        DocumentType docType = documentTypeRepository.findByDocTypeName(metadata.getDocTypeName().trim())
                .orElseThrow(() -> new Exception("Document type not found: " + metadata.getDocTypeName()));

        // Create Document
        Document document = Document.builder()
                .title(metadata.getTitle())
                .documentType(docType)
                .uploaderUser(currentUser)
                .createdAt(LocalDateTime.now())
                .fileExtension(fileExtension)
                .build();
        document = documentRepository.save(document);

        // Create DocumentVersion
        String versionNumber = metadata.getVersionNumber();
        if (versionNumber == null || versionNumber.isEmpty()) versionNumber = "1.0";

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(versionNumber)
                .filePath(filePath.toString())
                .build();
        documentVersionRepository.save(version);

        // Tags
        if (metadata.getTags() != null && !metadata.getTags().isEmpty()) {
            for (String tagStr : metadata.getTags().split(",")) {
                String tagName = tagStr.trim().toLowerCase();
                if (tagName.isEmpty()) continue;

                Tag tag = tagRepository.findByTagName(tagName)
                        .orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setTagName(tagName);
                            newTag.setCreatedBy(currentUser);
                            newTag.setCreatedAt(LocalDateTime.now());
                            return tagRepository.save(newTag);
                        });

                DocumentTag.DocumentTagId dtId = new DocumentTag.DocumentTagId(document.getDocumentId(), tag.getTagId());
                DocumentTag dt = DocumentTag.builder()
                        .id(dtId)
                        .document(document)
                        .tag(tag)
                        .build();
                documentTagRepository.save(dt);
            }
        }

        // Classifications
        if (metadata.getClassifications() != null && !metadata.getClassifications().isEmpty()) {
            for (String classStr : metadata.getClassifications().split(",")) {
                String className = classStr.trim();
                if (className.isEmpty()) continue;

                Classification classification = classificationRepository.findByClassificationName(className)
                        .orElseGet(() -> {
                            Classification newClass = new Classification();
                            newClass.setClassificationName(className);
                            newClass.setCreatedBy(currentUser);
                            newClass.setCreatedAt(LocalDateTime.now());
                            return classificationRepository.save(newClass);
                        });

                DocumentClassification.DocumentClassificationId dcId =
                        new DocumentClassification.DocumentClassificationId(document.getDocumentId(), classification.getClassificationId());
                DocumentClassification dc = DocumentClassification.builder()
                        .id(dcId)
                        .document(document)
                        .classification(classification)
                        .build();
                documentClassificationRepository.save(dc);
            }
        }
    }

    // ============= PARSING =============

    private List<DocumentMetadata> parseExcelFile(MultipartFile excelFile) throws Exception {
        List<DocumentMetadata> metadataList = new ArrayList<>();

        try (InputStream is = excelFile.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int startRow = detectDataStartRow(sheet);

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                DocumentMetadata metadata = new DocumentMetadata();
                metadata.setFilename(getCellValueAsString(row.getCell(0)));
                metadata.setTitle(getCellValueAsString(row.getCell(1)));
                metadata.setDocTypeName(getCellValueAsString(row.getCell(2)));
                metadata.setVersionNumber(getCellValueAsString(row.getCell(3)));

                String tagsValue = getCellValueAsString(row.getCell(4));
                if (tagsValue != null && !tagsValue.isEmpty()) {
                    metadata.setTags(Arrays.stream(tagsValue.split(","))
                            .map(String::trim).filter(t -> !t.isEmpty())
                            .map(t -> t.replaceAll("\\s+", "").toLowerCase())
                            .collect(Collectors.joining(",")));
                } else {
                    metadata.setTags("");
                }

                String classValue = getCellValueAsString(row.getCell(5));
                if (classValue != null && !classValue.isEmpty()) {
                    metadata.setClassifications(Arrays.stream(classValue.split(","))
                            .map(String::trim).filter(c -> !c.isEmpty())
                            .collect(Collectors.joining(",")));
                } else {
                    metadata.setClassifications("");
                }

                if (metadata.getFilename() != null && !metadata.getFilename().isEmpty()) {
                    metadataList.add(metadata);
                }
            }
        }
        return metadataList;
    }

    private List<DocumentMetadata> parseJsonToMetadataList(String jsonString) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> jsonList = objectMapper.readValue(
                    jsonString, new TypeReference<List<Map<String, Object>>>() {});

            List<DocumentMetadata> metadataList = new ArrayList<>();

            for (Map<String, Object> jsonObj : jsonList) {
                DocumentMetadata metadata = new DocumentMetadata();
                metadata.setFilename(getStringValue(jsonObj, "fileName"));
                metadata.setTitle(getStringValue(jsonObj, "title"));
                metadata.setDocTypeName(getStringValue(jsonObj, "docTypeName"));
                metadata.setVersionNumber(getStringValue(jsonObj, "versionNumber"));

                Object tagsObj = jsonObj.get("tags");
                if (tagsObj instanceof List) {
                    metadata.setTags(((List<?>) tagsObj).stream()
                            .map(Object::toString).map(String::trim).filter(t -> !t.isEmpty())
                            .map(t -> t.replaceAll("\\s+", "").toLowerCase())
                            .collect(Collectors.joining(",")));
                } else if (tagsObj instanceof String) {
                    metadata.setTags(Arrays.stream(((String) tagsObj).split(","))
                            .map(String::trim).filter(t -> !t.isEmpty())
                            .map(t -> t.replaceAll("\\s+", "").toLowerCase())
                            .collect(Collectors.joining(",")));
                }

                Object classObj = jsonObj.get("classifications");
                if (classObj instanceof List) {
                    metadata.setClassifications(((List<?>) classObj).stream()
                            .map(Object::toString).map(String::trim).filter(c -> !c.isEmpty())
                            .collect(Collectors.joining(",")));
                } else if (classObj instanceof String) {
                    metadata.setClassifications(Arrays.stream(((String) classObj).split(","))
                            .map(String::trim).filter(c -> !c.isEmpty())
                            .collect(Collectors.joining(",")));
                }

                metadataList.add(metadata);
            }

            return metadataList;

        } catch (Exception e) {
            logger.error("Failed to parse JSON metadata", e);
            throw new Exception("Failed to parse edited metadata: " + e.getMessage(), e);
        }
    }

    // ============= HELPERS =============

    private int detectDataStartRow(Sheet sheet) {
        Row row1 = sheet.getRow(1);
        if (row1 != null) {
            Cell cell = row1.getCell(0);
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue();
                if (val != null && (val.startsWith("*") || val.contains("= Required"))) {
                    return 3;
                }
            }
        }
        return 1;
    }

    private Set<String> extractFilenames(MultipartFile[] files) throws IOException {
        Set<String> filenames = new HashSet<>();
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
                try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            filenames.add(normalizeFilename(entry.getName()));
                        }
                        zis.closeEntry();
                    }
                }
            } else if (originalFilename != null) {
                filenames.add(normalizeFilename(originalFilename));
            }
        }
        return filenames;
    }

    private Map<String, MultipartFile> createFileMap(MultipartFile[] files) throws IOException {
        Map<String, MultipartFile> fileMap = new HashMap<>();
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
                extractZipFiles(file, fileMap);
            } else if (originalFilename != null) {
                fileMap.put(normalizeFilename(originalFilename), file);
            }
        }
        return fileMap;
    }

    private void extractZipFiles(MultipartFile zipFile, Map<String, MultipartFile> fileMap) throws IOException {
        Path tempDir = Files.createTempDirectory("bulk-upload-");
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String filename = normalizeFilename(entry.getName());
                    Path tempFile = tempDir.resolve(filename);
                    Files.copy(zis, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    ExtractedMultipartFile extractedFile = new ExtractedMultipartFile(tempFile.toFile(), filename);
                    fileMap.put(filename, extractedFile);
                }
                zis.closeEntry();
            }
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName());
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString().trim();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                double numValue = cell.getNumericCellValue();
                return (numValue == Math.floor(numValue)) ? String.valueOf((int) numValue) : String.valueOf(numValue);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue().trim(); }
                catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
            case BLANK: return null;
            default: return null;
        }
    }
}
