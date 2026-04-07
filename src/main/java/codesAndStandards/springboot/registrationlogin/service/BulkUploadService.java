package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.BulkUploadValidationResult;
import codesAndStandards.springboot.registrationlogin.dto.BulkUploadResult;
import codesAndStandards.springboot.registrationlogin.dto.DocumentMetadata;
import codesAndStandards.springboot.registrationlogin.dto.ExtractedMultipartFile;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.Tag;
import codesAndStandards.springboot.registrationlogin.entity.Classification;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.TagRepository;
import codesAndStandards.springboot.registrationlogin.repository.ClassificationRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
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
import org.springframework.transaction.annotation.Propagation; // Import Propagation
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;

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

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassificationRepository classificationRepository;

    @Autowired
    private ApplicationSettingsService settingsService;

    @Value("${file.network-base-path:}")
    private String networkBasePath;

    @Value("${file.upload-dir}")
    private String uploadDir;


    /* =====================================================
       =============== VALIDATION HELPERS ==================
       ===================================================== */

    private String normalizeFilename(String name) {
        if (name == null) return null;
        return new File(name).getName().trim().toLowerCase();
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Validate ONE document and decide validity
     */
    private boolean validateSingleDocument(
            DocumentMetadata metadata,
            Set<String> pdfFilenames,
            BulkUploadValidationResult result,
            Set<String> invalidDocuments
    ) {
        String filename = normalizeFilename(metadata.getFilename());
        boolean hasError = false;

        // PDF existence
        if (!pdfFilenames.contains(filename)) {
            result.addError("Missing PDF File",
                    "Document '" + filename + "' PDF not found in upload");
            hasError = true;
        }

        // Mandatory fields → ERRORS
        if (isEmpty(metadata.getTitle())) {
            result.addError("Missing Title",
                    "Document '" + filename + "' is missing title");
            hasError = true;
        }

        if (isEmpty(metadata.getProductCode())) {
            result.addError("Missing Product Code",
                    "Document '" + filename + "' is missing product code");
            hasError = true;
        }

        if (isEmpty(metadata.getPublishYear())) {
            result.addError("Missing Publish Year",
                    "Document '" + filename + "' is missing publish year");
            hasError = true;
        }

        // Optional → WARNINGS ONLY
        if (metadata.getNoOfPages() == null || metadata.getNoOfPages() <= 0) {
            result.addWarning("Invalid Page Count",
                    "Document '" + filename + "' has invalid or missing page count");
        }

        if (hasError) {
            invalidDocuments.add(filename);
        }

        return !hasError;
    }

    /**
     * Generate Excel template from uploaded PDF filenames and page counts.
     */
    public ByteArrayOutputStream generateExcelTemplate(List<String> filenames, List<String> pageCounts) throws Exception {
        List<String> validFilenames = new ArrayList<>();
        List<String> validPageCounts = new ArrayList<>();
        
        if (filenames != null) {
            for (int i = 0; i < filenames.size(); i++) {
                String filename = filenames.get(i);
                if (filename != null && !filename.trim().isEmpty()) {
                    validFilenames.add(normalizeFilename(filename));
                    if (pageCounts != null && i < pageCounts.size()) {
                        validPageCounts.add(pageCounts.get(i));
                    } else {
                        validPageCounts.add("");
                    }
                }
            }
        }

        // Create a list of indices to sort by filename
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < validFilenames.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator.comparing(validFilenames::get));

        // Read maxTagsPerDocument from settings
        int maxTagsPerDoc = 10; // safe default
        try {
            Integer settingValue = settingsService.getMaxTagsPerDocument();
            if (settingValue != null && settingValue > 0) {
                maxTagsPerDoc = settingValue;
            }
        } catch (Exception e) {
            logger.warn("Could not read maxTagsPerDocument from settings, using default=10: {}", e.getMessage());
        }
        final int maxTagsForTemplate = maxTagsPerDoc;

        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();

        // ============= SHEET 1: DOCUMENT METADATA =============
        Sheet sheet = workbook.createSheet("Documents");

        // ====== Header styles ======
        CellStyle headerStyleRequired = workbook.createCellStyle();
        Font headerFontRequired = workbook.createFont();
        headerFontRequired.setBold(true);
        headerFontRequired.setFontHeightInPoints((short) 12);
        headerFontRequired.setColor(IndexedColors.WHITE.getIndex());
        headerStyleRequired.setFont(headerFontRequired);
        headerStyleRequired.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyleRequired.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyleRequired.setBorderBottom(BorderStyle.THIN);
        headerStyleRequired.setBorderTop(BorderStyle.THIN);
        headerStyleRequired.setBorderLeft(BorderStyle.THIN);
        headerStyleRequired.setBorderRight(BorderStyle.THIN);
        headerStyleRequired.setAlignment(HorizontalAlignment.CENTER);
        headerStyleRequired.setVerticalAlignment(VerticalAlignment.CENTER);

        // Optional header style
        CellStyle headerStyleOptional = workbook.createCellStyle();
        Font headerFontOptional = workbook.createFont();
        headerFontOptional.setBold(true);
        headerFontOptional.setFontHeightInPoints((short) 11);
        headerFontOptional.setColor(IndexedColors.WHITE.getIndex());
        headerStyleOptional.setFont(headerFontOptional);
        headerStyleOptional.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyleOptional.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyleOptional.setBorderBottom(BorderStyle.THIN);
        headerStyleOptional.setBorderTop(BorderStyle.THIN);
        headerStyleOptional.setBorderLeft(BorderStyle.THIN);
        headerStyleOptional.setBorderRight(BorderStyle.THIN);
        headerStyleOptional.setAlignment(HorizontalAlignment.CENTER);
        headerStyleOptional.setVerticalAlignment(VerticalAlignment.CENTER);

        String tagsHeader = "Tags (comma-separated, max " + maxTagsForTemplate + ")";

        // Column index → [header text, isRequired]
        Object[][] columnDefs = {
                {"Filename *",            true},   // 0
                {"Title *",               true},   // 1
                {"Product Code *",        true},   // 2
                {"Edition",               false},  // 3
                {"Publish Month",         false},  // 4
                {"Publish Year *",        true},   // 5
                {"No of Pages",           false},  // 6
                {"Notes",                 false},  // 7
                {tagsHeader,              false},  // 8
                {"Classifications (comma-separated)", false} // 9
        };

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(22);

        for (int i = 0; i < columnDefs.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue((String) columnDefs[i][0]);
            boolean required = (Boolean) columnDefs[i][1];
            cell.setCellStyle(required ? headerStyleRequired : headerStyleOptional);

            // Column widths
            if (i == 0 || i == 1) {
                sheet.setColumnWidth(i, 45 * 256);
            } else if (i == 7 || i == 8 || i == 9) {
                sheet.setColumnWidth(i, 38 * 256);
            } else {
                sheet.setColumnWidth(i, 22 * 256);
            }
        }

        // Add legend row
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
        legendCell.setCellValue("* = Required field   |   Dark blue = Required   |   Grey = Optional   |   Tags must be comma-separated (e.g. tag1,tag2,tag3)   |   Max " + maxTagsForTemplate + " tags per document");
        legendCell.setCellStyle(legendStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnDefs.length - 1));

        // Add example row
        Row exampleRow = sheet.createRow(2);
        CellStyle exampleStyle = workbook.createCellStyle();
        Font exampleFont = workbook.createFont();
        exampleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        exampleFont.setItalic(true);
        exampleStyle.setFont(exampleFont);

        String[] exampleData = {
                "document1.pdf",
                "Product Manual v2.1",
                "PM-001",
                "2.1",
                "06",
                "2024",
                "150",
                "Updated version",
                "manual,technical,v2",
                "Engineering,Safety"
        };
        for (int i = 0; i < exampleData.length; i++) {
            Cell cell = exampleRow.createCell(i);
            cell.setCellValue(exampleData[i]);
            cell.setCellStyle(exampleStyle);
        }

        // Dropdown for Publish Month column
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint monthConstraint = validationHelper.createExplicitListConstraint(
                new String[]{
                        "01 (Jan)", "02 (Feb)", "03 (Mar)", "04 (Apr)",
                        "05 (May)", "06 (Jun)", "07 (Jul)", "08 (Aug)",
                        "09 (Sep)", "10 (Oct)", "11 (Nov)", "12 (Dec)"
                }
        );
        CellRangeAddressList monthRange = new CellRangeAddressList(3, 1003, 4, 4);
        DataValidation monthValidation = validationHelper.createValidation(monthConstraint, monthRange);
        monthValidation.setShowErrorBox(true);
        sheet.addValidationData(monthValidation);

        // ====== Create data rows from row index 3 ======
        int rowNum = 3;

        for (int idx : indices) {
            String filename = validFilenames.get(idx);
            String pageCountStr = validPageCounts.get(idx);
            
            Row row = sheet.createRow(rowNum++);

            // Column 0: Filename (pre-filled)
            row.createCell(0).setCellValue(filename);

            // Columns 1-5: Empty
            for (int i = 1; i <= 5; i++) {
                row.createCell(i).setCellValue("");
            }

            // Column 6: Page count
            Cell pageCell = row.createCell(6);
            if (pageCountStr != null && !pageCountStr.trim().isEmpty() && !pageCountStr.equals("0")) {
                try {
                    pageCell.setCellValue(Integer.parseInt(pageCountStr));
                } catch (NumberFormatException e) {
                    pageCell.setCellValue("");
                }
            } else {
                pageCell.setCellValue("");
            }

            // Columns 7-9: Empty
            for (int i = 7; i <= 9; i++) {
                row.createCell(i).setCellValue("");
            }
        }

        // ============= SHEET 2: REFERENCE DATA =============
        Sheet referenceSheet = workbook.createSheet("Reference Data");

        Row refHeaderRow = referenceSheet.createRow(0);
        Cell tagsHeaderCell = refHeaderRow.createCell(0);
        tagsHeaderCell.setCellValue("Available Tags (max " + maxTagsForTemplate + " per document)");
        tagsHeaderCell.setCellStyle(headerStyleRequired);

        Cell classHeaderCell = refHeaderRow.createCell(2);
        classHeaderCell.setCellValue("Available Classifications");
        classHeaderCell.setCellStyle(headerStyleOptional);

        referenceSheet.setColumnWidth(0, 35 * 256);
        referenceSheet.setColumnWidth(2, 35 * 256);

        List<String> existingTags = tagRepository.findAll().stream()
                .map(Tag::getTagName)
                .filter(name -> name != null && !name.isEmpty())
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        List<String> existingClassifications = classificationRepository.findAll().stream()
                .map(Classification::getClassificationName)
                .filter(name -> name != null && !name.isEmpty())
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        int tagRowNum = 1;
        for (String tag : existingTags) {
            Row row = referenceSheet.getRow(tagRowNum);
            if (row == null) row = referenceSheet.createRow(tagRowNum);
            row.createCell(0).setCellValue(tag);
            tagRowNum++;
        }

        int classRowNum = 1;
        for (String classification : existingClassifications) {
            Row row = referenceSheet.getRow(classRowNum);
            if (row == null) row = referenceSheet.createRow(classRowNum);
            row.createCell(2).setCellValue(classification);
            classRowNum++;
        }

        int noteRowNum = Math.max(tagRowNum, classRowNum) + 2;
        Row noteRow = referenceSheet.createRow(noteRowNum);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("Note: Tags and Classifications in the Documents sheet must be comma-separated (e.g. tag1,tag2,tag3). " +
                "Max " + maxTagsForTemplate + " tags per document. New entries will be created automatically during upload.");

        CellStyle noteStyle = workbook.createCellStyle();
        Font noteFont = workbook.createFont();
        noteFont.setItalic(true);
        noteFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        noteStyle.setFont(noteFont);
        noteCell.setCellStyle(noteStyle);
        referenceSheet.addMergedRegion(new CellRangeAddress(noteRowNum, noteRowNum, 0, 2));

        // ============= SHEET 3: Tag Policies =============
        Sheet policiesSheet = workbook.createSheet("Tag Policies");
        Row policiesHeader = policiesSheet.createRow(0);
        Cell phCell = policiesHeader.createCell(0);
        phCell.setCellValue("Tag & Upload Policies");
        phCell.setCellStyle(headerStyleRequired);
        policiesSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        policiesSheet.setColumnWidth(0, 40 * 256);
        policiesSheet.setColumnWidth(1, 20 * 256);

        String[][] policies = {
                {"Max Tags Per Document", String.valueOf(maxTagsForTemplate)},
                {"Tags Format", "Comma-separated (e.g. tag1,tag2,tag3)"},
                {"Tags Case", "Lowercase only — tags are auto-converted to lowercase"},
                {"Classifications Format", "Comma-separated (e.g. Class1,Class2)"},
                {"Required Fields", "Filename, Title, Product Code, Publish Year"}
        };
        int pRow = 1;
        for (String[] pair : policies) {
            Row r = policiesSheet.createRow(pRow++);
            r.createCell(0).setCellValue(pair[0]);
            r.createCell(1).setCellValue(pair[1]);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream;
    }


    /* =====================================================
       =============== BULK VALIDATION =====================
       ===================================================== */

    public BulkUploadValidationResult validateBulkUpload(
            MultipartFile[] pdfFiles,
            List<String> pdfFilenamesList,
            MultipartFile excelFile,
            String selfValidationJson) throws Exception {

        BulkUploadValidationResult result = new BulkUploadValidationResult();

        List<DocumentMetadata> metadataList =
                (selfValidationJson != null && !selfValidationJson.isEmpty())
                        ? parseJsonToMetadataList(selfValidationJson)
                        : parseExcelFile(excelFile);

        Set<String> pdfFilenames = new HashSet<>();
        if (pdfFiles != null && pdfFiles.length > 0) {
            pdfFilenames.addAll(extractPdfFilenames(pdfFiles).stream()
                .map(this::normalizeFilename)
                .collect(Collectors.toSet()));
        }
        if (pdfFilenamesList != null && !pdfFilenamesList.isEmpty()) {
            pdfFilenames.addAll(pdfFilenamesList.stream()
                .map(this::normalizeFilename)
                .collect(Collectors.toSet()));
        }

        Set<String> invalidDocuments = new HashSet<>();

        for (DocumentMetadata metadata : metadataList) {
            validateSingleDocument(metadata, pdfFilenames, result, invalidDocuments);
        }

        Set<String> metadataFilenames = metadataList.stream()
                .map(m -> normalizeFilename(m.getFilename()))
                .collect(Collectors.toSet());

        for (String pdf : pdfFilenames) {
            if (!metadataFilenames.contains(pdf)) {
                result.addWarning("Extra PDF File",
                        "PDF '" + pdf + "' uploaded but not found in metadata");
            }
        }

        int total = metadataList.size();
        int invalid = invalidDocuments.size();
        int valid = total - invalid;

        result.setTotalDocuments(total);
        result.setValidDocuments(valid);

        logger.info(
                "Validation summary → Total={}, Valid={}, Invalid={}, Errors={}, Warnings={}",
                total, valid, invalid,
                result.getErrors() != null ? result.getErrors().size() : 0,
                result.getWarnings() != null ? result.getWarnings().size() : 0
        );

        return result;
    }

    /**
     * Process bulk upload
     */
    // Removed @Transactional from here to allow individual document transactions
    public BulkUploadResult processBulkUpload(
            MultipartFile[] pdfFiles,
            MultipartFile excelFile,
            String selfValidationJson,
            boolean uploadOnlyValid) throws Exception {

        BulkUploadResult result = new BulkUploadResult();

        List<DocumentMetadata> metadataList =
                (selfValidationJson != null && !selfValidationJson.isEmpty())
                        ? parseJsonToMetadataList(selfValidationJson)
                        : parseExcelFile(excelFile);

        if (uploadOnlyValid) {
            BulkUploadValidationResult validation =
                    validateBulkUpload(pdfFiles, null, excelFile, selfValidationJson);

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

        Map<String, MultipartFile> fileMap = createFileMap(pdfFiles);

        for (DocumentMetadata metadata : metadataList) {
            String filename = normalizeFilename(metadata.getFilename());
            MultipartFile pdfFile = fileMap.get(filename);

            if (pdfFile != null) {
                try {
                    // Each uploadDocument call will now run in its own transaction
                    uploadDocument(pdfFile, metadata);
                    result.addSuccess(filename, metadata.getTitle());
                } catch (Exception e) {
                    logger.error("Failed to upload document {}: {}", filename, e.getMessage());
                    result.addFailure(filename, e.getMessage());
                }
            } else {
                result.addFailure(filename, "PDF file not found");
            }
        }

        return result;
    }

    private List<DocumentMetadata> parseJsonToMetadataList(String jsonString) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<Map<String, Object>> jsonList = objectMapper.readValue(
                    jsonString,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            List<DocumentMetadata> metadataList = new ArrayList<>();

            for (Map<String, Object> jsonObj : jsonList) {
                DocumentMetadata metadata = new DocumentMetadata();

                metadata.setFilename(getStringValue(jsonObj, "fileName"));
                metadata.setTitle(getStringValue(jsonObj, "title"));
                metadata.setProductCode(getStringValue(jsonObj, "productCode"));
                metadata.setEdition(getStringValue(jsonObj, "edition"));

                String publishMonth = getStringValue(jsonObj, "publishMonth");
                if (publishMonth != null && !publishMonth.trim().isEmpty()) {
                    publishMonth = publishMonth.trim();
                    if (publishMonth.contains("(")) {
                        publishMonth = publishMonth.substring(0, publishMonth.indexOf("(")).trim();
                    }
                    if (publishMonth.length() == 1) {
                        publishMonth = "0" + publishMonth;
                    }
                }
                metadata.setPublishMonth(publishMonth);
                metadata.setPublishYear(getStringValue(jsonObj, "publishYear"));

                Object noOfPagesObj = jsonObj.get("noOfPages");
                if (noOfPagesObj != null) {
                    if (noOfPagesObj instanceof Number) {
                        metadata.setNoOfPages(((Number) noOfPagesObj).intValue());
                    } else if (noOfPagesObj instanceof String) {
                        String noOfPagesStr = (String) noOfPagesObj;
                        if (!noOfPagesStr.isEmpty()) {
                            try {
                                metadata.setNoOfPages(Integer.parseInt(noOfPagesStr));
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid page count for {}: {}", metadata.getFilename(), noOfPagesStr);
                            }
                        }
                    }
                }

                metadata.setNotes(getStringValue(jsonObj, "notes"));

                Object tagsObj = jsonObj.get("tags");
                if (tagsObj instanceof List) {
                    List<?> tagsList = (List<?>) tagsObj;
                    String tagsString = tagsList.stream()
                            .map(Object::toString)
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .map(this::normalizeTag)
                            .collect(Collectors.joining(","));
                    metadata.setTags(tagsString);
                } else if (tagsObj instanceof String) {
                    String tagsValue = (String) tagsObj;
                    String normalizedTags = Arrays.stream(tagsValue.split(","))
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .map(this::normalizeTag)
                            .collect(Collectors.joining(","));
                    metadata.setTags(normalizedTags);
                }

                Object classificationsObj = jsonObj.get("classifications");
                if (classificationsObj instanceof List) {
                    List<?> classificationsList = (List<?>) classificationsObj;
                    String classificationsString = classificationsList.stream()
                            .map(Object::toString)
                            .map(String::trim)
                            .filter(c -> !c.isEmpty())
                            .collect(Collectors.joining(","));
                    metadata.setClassifications(classificationsString);
                } else if (classificationsObj instanceof String) {
                    String classValue = (String) classificationsObj;
                    String normalizedClass = Arrays.stream(classValue.split(","))
                            .map(String::trim)
                            .filter(c -> !c.isEmpty())
                            .collect(Collectors.joining(","));
                    metadata.setClassifications(normalizedClass);
                }

                metadataList.add(metadata);
            }

            return metadataList;

        } catch (Exception e) {
            logger.error("Failed to parse JSON metadata", e);
            throw new Exception("Failed to parse edited metadata: " + e.getMessage(), e);
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString().trim();
    }

    private List<DocumentMetadata> parseExcelFile(MultipartFile excelFile) throws Exception {
        List<DocumentMetadata> metadataList = new ArrayList<>();

        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            int startRow = detectDataStartRow(sheet);

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                DocumentMetadata metadata = new DocumentMetadata();

                metadata.setFilename(getCellValueAsString(row.getCell(0)));
                metadata.setTitle(getCellValueAsString(row.getCell(1)));
                metadata.setProductCode(getCellValueAsString(row.getCell(2)));
                metadata.setEdition(getCellValueAsString(row.getCell(3)));

                String publishMonth = getCellValueAsString(row.getCell(4));
                if (publishMonth != null && !publishMonth.trim().isEmpty()) {
                    publishMonth = publishMonth.trim();
                    if (publishMonth.contains("(")) {
                        publishMonth = publishMonth.substring(0, publishMonth.indexOf("(")).trim();
                    }
                    if (publishMonth.length() == 1) {
                        publishMonth = "0" + publishMonth;
                    }
                }
                metadata.setPublishMonth(publishMonth);

                metadata.setPublishYear(getCellValueAsString(row.getCell(5)));
                metadata.setNoOfPages(getCellValueAsInteger(row.getCell(6)));
                metadata.setNotes(getCellValueAsString(row.getCell(7)));

                String tagsValue = getCellValueAsString(row.getCell(8));
                if (tagsValue != null && !tagsValue.isEmpty()) {
                    String normalizedTags = Arrays.stream(tagsValue.split(","))
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .map(this::normalizeTag)
                            .collect(Collectors.joining(","));
                    metadata.setTags(normalizedTags);
                } else {
                    metadata.setTags("");
                }

                String classValue = getCellValueAsString(row.getCell(9));
                if (classValue != null && !classValue.isEmpty()) {
                    String normalizedClass = Arrays.stream(classValue.split(","))
                            .map(String::trim)
                            .filter(c -> !c.isEmpty())
                            .collect(Collectors.joining(","));
                    metadata.setClassifications(normalizedClass);
                } else {
                    metadata.setClassifications(getCellValueAsString(row.getCell(9)));
                }

                if (metadata.getFilename() != null && !metadata.getFilename().isEmpty()) {
                    metadataList.add(metadata);
                }
            }
        }
        return metadataList;
    }

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

    private String normalizeTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return "";
        }
        return tag.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private Set<String> extractPdfFilenames(MultipartFile[] files) throws IOException {
        Set<String> filenames = new HashSet<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();

            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
                try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".pdf")) {
                            filenames.add(normalizeFilename(entry.getName()));
                        }
                        zis.closeEntry();
                    }
                }
            } else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
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
            } else if (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf")) {
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
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".pdf")) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW) // Changed to REQUIRES_NEW
    private void uploadDocument(MultipartFile file, DocumentMetadata metadata) throws Exception {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new Exception("User not authenticated");
        }

        String username = currentUser.getUsername();
        Path userUploadPath = Paths.get(uploadDir, username);
        if (!Files.exists(userUploadPath)) {
            Files.createDirectories(userUploadPath);
        }

        String originalFileName = new File(file.getOriginalFilename()).getName();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = Paths.get(uploadDir, uniqueFileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        String networkPath = filePath.toString();

        Integer pageCount = metadata.getNoOfPages();
        if (pageCount == null || pageCount == 0) {
            try {
                pageCount = detectPageCount(file);
                logger.info("Auto-detected page count for {}: {}", file.getOriginalFilename(), pageCount);
            } catch (Exception e) {
                logger.warn("Failed to auto-detect page count for {}: {}", file.getOriginalFilename(), e.getMessage());
                pageCount = null;
            }
        }

        Document document = new Document();
        document.setTitle(metadata.getTitle());
        document.setProductCode(metadata.getProductCode());
        document.setEdition(metadata.getEdition());
        document.setNoOfPages(pageCount);
        document.setNotes(metadata.getNotes());
        document.setFilePath(networkPath);
        document.setUploadedAt(LocalDateTime.now());
        document.setUploadedBy(currentUser);

        if (metadata.getPublishYear() != null && !metadata.getPublishYear().isEmpty()) {
            String year = metadata.getPublishYear();
            String month = metadata.getPublishMonth();
            if (month != null && !month.isEmpty()) {
                document.setPublishDate(year + "-" + month);
            } else {
                document.setPublishDate(year);
            }
        }

        document = documentRepository.save(document);

        if (metadata.getTags() != null && !metadata.getTags().isEmpty()) {
            Set<Tag> tags = new HashSet<>();
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
                tags.add(tag);
            }
            document.setTags(tags);
        }

        if (metadata.getClassifications() != null && !metadata.getClassifications().isEmpty()) {
            Set<Classification> classifications = new HashSet<>();
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
                classifications.add(classification);
            }
            document.setClassifications(classifications);
        }

        documentRepository.save(document);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName());
    }

    private Integer detectPageCount(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            throw new IOException("Failed to detect page count: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((int) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return Integer.parseInt(value);
            } else if (cell.getCellType() == CellType.BLANK) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}