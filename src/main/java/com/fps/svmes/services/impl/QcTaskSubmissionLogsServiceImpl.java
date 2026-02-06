package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.qcForm.QcTaskSubmissionLogsDTO;
import com.fps.svmes.models.sql.qcForm.QcTaskSubmissionLogs;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcTaskSubmissionLogsRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcApprovalAssignmentRepository;
import com.fps.svmes.services.AlertRecordService;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.QcTaskSubmissionLogsService;
import com.fps.svmes.services.QcSnapshotSubmissionService;
import com.itextpdf.text.Paragraph;

import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.mongodb.client.MongoCollection;
import jakarta.validation.constraints.Null;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Query;
import com.itextpdf.text.*;

import java.io.InputStream;
import java.time.*;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Collections;

import org.bson.Document;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QcTaskSubmissionLogsServiceImpl implements QcTaskSubmissionLogsService {
    @Autowired
    private QcTaskSubmissionLogsRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QcFormTemplateRepository qcFormTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    public static Logger logger = LoggerFactory.getLogger(QcTaskSubmissionLogsService.class);

    @Autowired
    private AlertRecordService alertRecordService;

    @Autowired
    private QcSnapshotSubmissionService qcSnapshotSubmissionService;

    @Autowired
    private QcApprovalAssignmentRepository qcApprovalAssignmentRepository;

    @Override
    public QcTaskSubmissionLogsDTO insertLog(QcTaskSubmissionLogsDTO dto) {
        // Map the DTO to the entity
        QcTaskSubmissionLogs log = modelMapper.map(dto, QcTaskSubmissionLogs.class);

        // Set timestamps to current time
        log.setCreatedAt(OffsetDateTime.now());

        // Save the entity
        QcTaskSubmissionLogs savedLog = repository.save(log);

        // Map the saved entity back to DTO
        return modelMapper.map(savedLog, QcTaskSubmissionLogsDTO.class);
    }

    @Override
    public List<QcTaskSubmissionLogsDTO> getAllByCreatedByAndTaskId(Integer createdBy, Long dispatchedTaskId) {
        List<QcTaskSubmissionLogs> logs = repository.findAllByCreatedByAndDispatchedTaskId(createdBy, dispatchedTaskId);
        return logs.stream()
                .map(log -> modelMapper.map(log, QcTaskSubmissionLogsDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public Document getDocumentBySubmissionId(String submissionId, Long formId, Integer createdBy, Optional<String> inputCollectionName) {
        try {
            // Log input parameters for debugging
            logger.info("Fetching document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy);

            // Validate submissionId
            if (!ObjectId.isValid(submissionId)) {
                logger.error("Invalid submissionId format: {}", submissionId);
                throw new IllegalArgumentException("Invalid submissionId format");
            }

            // Determine collection name
            String collectionName = inputCollectionName.orElseGet(() -> {
                String yearMonth = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                return "form_template_" + formId + "_" + yearMonth;
            });

            logger.info("Looking in collection: {}", collectionName);

            // Check if collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                logger.error("Collection does not exist: {}", collectionName);
                throw new RuntimeException("Collection not found: " + collectionName);
            }

            // Construct the MongoDB query
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(new ObjectId(submissionId)));

            logger.info("Constructed query: {}", query);

            // Execute the query and fetch the document
            Document document = mongoTemplate.findOne(query, Document.class, collectionName);

            // adjust the document to categorize the results according to the form template
            if (document == null) {
                logger.warn("No document found for submissionId: {}", submissionId);
                return null;
            }

            logger.info("Document retrieved successfully: {}", document);
            return formattedResult(document, formId);

        } catch (Exception e) {
            logger.error("Error retrieving document with submissionId: {}, formId: {}, createdBy: {}", submissionId, formId, createdBy, e);
            throw new RuntimeException("Error retrieving document from MongoDB: " + e.getMessage(), e);
        }
    }


    public HashMap<String, String> getFormTemplateKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId); // get only that single result for now

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, String> keyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractKeyValuePairs(widgetList, keyValueMap); // Start recursive extraction
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return keyValueMap;
    }

    private HashMap<String, Object> QcFormTemplateOptionItemsKeyValueMapping(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);

        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        HashMap<String, Object> optionItemsKeyValueMap = new HashMap<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");

            if (widgetList != null) {
                extractOptionItems(widgetList, optionItemsKeyValueMap); // Start recursive extraction
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return optionItemsKeyValueMap;
    }

    private void extractOptionItems(List<Document> widgetList, HashMap<String, Object> optionItemsKeyValueMap) {
        for (Document widget : widgetList) {
            // Extract options with optionItems
            Document options = (Document) widget.get("options");
            if (options != null) {
                String label = options.getString("label");
                List<Document> optionItems = (List<Document>) options.get("optionItems");
                if (label != null && optionItems != null) {
                    HashMap<String, String> valueToLabelMap = new HashMap<>();
                    for (Document option : optionItems) {
                        Object value = option.get("value");
                        String optionLabel = option.getString("label");
                        if (value != null && optionLabel != null) {
                            valueToLabelMap.put(value.toString(), optionLabel);
                        }
                    }
                    optionItemsKeyValueMap.put(label, valueToLabelMap);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractOptionItems(nestedWidgetList, optionItemsKeyValueMap);
            }

            // Check for widget lists inside grid columns
            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractOptionItems(colWidgetList, optionItemsKeyValueMap);
                    }
                }
            }
        }
    }

    private void extractKeyValuePairs(List<Document> widgetList, HashMap<String, String> keyValueMap) {
        for (Document widget : widgetList) {
            // Extract options and add name-label pairs to the map
            Document options = (Document) widget.get("options");
            if (options != null) {
                String name = options.getString("name");
                String label = options.getString("label");
                if (name != null && label != null) {
                    keyValueMap.put(name, label);
                }
            }

            // Recursively process nested widgetList
            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractKeyValuePairs(nestedWidgetList, keyValueMap);
            }

            // Check for widget lists inside grid columns
            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractKeyValuePairs(colWidgetList, keyValueMap);
                    }
                }
            }
        }
    }

    public Document formattedResult(Document document, Long formId) {
        // 获取表单的字段映射（字段 name -> label）
        HashMap<String, String> keyValueMap = getFormTemplateKeyValueMapping(formId);
        HashMap<String, Object> optionItemsKeyValueMap = QcFormTemplateOptionItemsKeyValueMapping(formId);

        // 解析表单模板，构建 divider 层级映射
        HashMap<String, String> fieldToDividerMap = new HashMap<>();
        List<Document> widgetList = getWidgetListFromTemplate(formId);

        // Step 1: 解析表单模板，构建字段归属的 `divider`
        String currentDivider = "uncategorized"; // 默认归类
        for (Document widget : widgetList) {
            String type = widget.getString("type");
            Document options = (Document) widget.get("options");

            if ("divider".equals(type) && options != null) {
                // **遇到新的 `divider`，更新当前分组名称**
                currentDivider = options.getString("label");
            } else if ("grid".equals(type)) {
                // **如果当前 `divider` 下的第一个元素是 `grid`，那么 `grid` 内部的 `cols` 也归属于该 `divider`**
                List<Document> cols = (List<Document>) widget.get("cols");
                if (cols != null) {
                    for (Document col : cols) {
                        List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                        if (colWidgetList != null) {
                            for (Document colWidget : colWidgetList) {
                                Document colOptions = (Document) colWidget.get("options");
                                if (colOptions != null && colOptions.containsKey("name")) {
                                    fieldToDividerMap.put(colOptions.getString("name"), currentDivider);
                                }
                            }
                        }
                    }
                }
            } else if (options != null && options.containsKey("name")) {
                // 记录字段属于哪个分组
                fieldToDividerMap.put(options.getString("name"), currentDivider);
            }
        }

        // Step 2: 重新格式化 MongoDB 取出的数据
        Document formattedDocument = new Document();
        Document groupedData = new Document();

        for (String key : document.keySet()) {
            Object value = document.get(key);

            // 获取格式化后的字段名
            String formattedKey = keyValueMap.getOrDefault(key, key);
            String dividerLabel = fieldToDividerMap.get(key); // 获取字段归属的 `divider`

            // 如果字段不属于任何 `divider`，默认归类到 `"uncategorized"`
            if (dividerLabel == null) {
                dividerLabel = "uncategorized";
            }

            // 处理 optionItems 转换
            if (optionItemsKeyValueMap.containsKey(formattedKey) && value instanceof List) {
                List<?> valueList = (List<?>) value;
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(formattedKey);
                List<String> resolvedLabels = valueList.stream()
                        .map(val -> valueToLabelMap.getOrDefault(val.toString(), val.toString()))
                        .collect(Collectors.toList());
                value = resolvedLabels;
            } else if (optionItemsKeyValueMap.containsKey(formattedKey) && (value instanceof String || value instanceof Integer)) {
                HashMap<String, String> valueToLabelMap = (HashMap<String, String>) optionItemsKeyValueMap.get(formattedKey);
                value = valueToLabelMap.getOrDefault(value.toString(), value.toString());
            }

            // 保留 `_id`, `created_at`, `created_by` 在根层级
            if (List.of("_id", "created_at", "created_by").contains(key)) {
                formattedDocument.put(formattedKey, value);
            } else {
                // **正确归类到 `divider`**
                groupedData.computeIfAbsent(dividerLabel, k -> new Document());
                ((Document) groupedData.get(dividerLabel)).put(formattedKey, value);
            }
        }

        // 🔧 Step 3: Remap exceeded_info keys from name → label
        if (document.containsKey("exceeded_info")) {
            Document exceededInfo = (Document) document.get("exceeded_info");
            Document labeledExceededInfo = new Document();

            for (String key : exceededInfo.keySet()) {
                Object infoValue = exceededInfo.get(key);
                String labeledKey = keyValueMap.getOrDefault(key, key); // key → label
                labeledExceededInfo.put(labeledKey, infoValue);
            }

            formattedDocument.put("exceeded_info", labeledExceededInfo);
        }


        // **合并分组数据到最终 JSON**
        formattedDocument.putAll(groupedData);
        return formattedDocument;
    }

    // **解析 widgetList**
    private List<Document> getWidgetListFromTemplate(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);
        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }
        Document formTemplate = Document.parse(formTemplateJson);
        return (List<Document>) formTemplate.get("widgetList");
    }

    @Override
    public List<Document> getDocumentsByQcFormTemplateIdAndCreatedBy(Long qcFormTemplateId, Integer createdBy) {
        try {
            // Generate the collection name dynamically
            String yearMonth = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String collectionName = "form_template_" + qcFormTemplateId + "_" + yearMonth;

            // Check if collection exists
            if (!mongoTemplate.collectionExists(collectionName)) {
                throw new RuntimeException("Collection not found: " + collectionName);
            }

            // Construct the MongoDB query
            Query query = new Query();
            query.addCriteria(Criteria.where("created_by").is(createdBy));

            // Execute the query and fetch the documents
            List<Document> documents = mongoTemplate.find(query, Document.class, collectionName);

            // Format each document
            List<Document> formattedDocuments = documents.stream()
                    .map(document -> formattedResult(document, qcFormTemplateId))
                    .collect(Collectors.toList());

            return formattedDocuments;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving documents from MongoDB: " + e.getMessage(), e);
        }
    }

    // the downloaded name should be the filename
    @Override
    public byte[] exportDocumentsToExcel(List<Document> documents) {
        try {
            // Create a new workbook
            Workbook workbook = new XSSFWorkbook();
            // name it to current datetime
            String fileName = "documents_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            Sheet sheet = workbook.createSheet(fileName);

            // Create header row
            Row headerRow = sheet.createRow(0);
            int cellNum = 0;
            for (String key : documents.get(0).keySet()) {
                Cell cell = headerRow.createCell(cellNum++);
                cell.setCellValue(key);
            }

            // Populate data rows
            int rowNum = 1;
            for (Document document : documents) {
                Row row = sheet.createRow(rowNum++);
                cellNum = 0;
                for (Object value : document.values()) {
                    Cell cell = row.createCell(cellNum++);
                    if (value == null) {
                        cell.setCellValue(""); // 如果 value 是 null，则填充空字符串
                    } else if (value instanceof List) {
                        cell.setCellValue(String.join(", ", (List<String>) value));
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // Write workbook to ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error exporting documents to Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportDocumentToPdf(Document mongoDocument) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Create a new PDF document (iText)
            com.itextpdf.text.Document pdfDocument = new com.itextpdf.text.Document();
            PdfWriter.getInstance(pdfDocument, outputStream);

            // Open the PDF document
            pdfDocument.open();

            // Load the font from resources instead of system path
            BaseFont baseFont;
            try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/simsun.ttc")) {
                if (fontStream == null) {
                    throw new RuntimeException("Font file not found in resources!");
                }
                byte[] fontBytes = fontStream.readAllBytes();
                baseFont = BaseFont.createFont("fonts/simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }

            Font chineseFont = new Font(baseFont, 12, Font.NORMAL);

            // Add the title "提交记录"
            pdfDocument.add(new Paragraph("提交记录", new Font(baseFont, 18, Font.BOLD, BaseColor.BLUE)));
            pdfDocument.add(Chunk.NEWLINE); // Add a blank line for spacing

            // Create a table to dynamically display the key-value pairs
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setWidths(new float[]{2f, 5f});

            // Dynamically iterate over the MongoDB document and handle specific keys
            for (Map.Entry<String, Object> entry : mongoDocument.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if ("_id".equals(key)) {
                    key = "提交单号";
                } else if ("created_at".equals(key)) {
                    key = "提交时间";
                    value = convertToLocalTime(value.toString());
                } else if ("created_by".equals(key)) {
                    key = "提交人ID";
                }

                PdfPCell keyCell = new PdfPCell(new Phrase(key, chineseFont));
                keyCell.setBorder(Rectangle.NO_BORDER);
                keyCell.setPadding(5);

                PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value.toString() : "", chineseFont));
                valueCell.setBorder(Rectangle.NO_BORDER);
                valueCell.setPadding(5);

                table.addCell(keyCell);
                table.addCell(valueCell);
            }

            pdfDocument.add(table);

            // Add the closing "提交时间" at the end
            pdfDocument.add(Chunk.NEWLINE);
            String createdAt = mongoDocument.getString("created_at");
            pdfDocument.add(new Paragraph(
                    "提交时间: " + (createdAt != null ? convertToLocalTime(createdAt) : "N/A"),
                    chineseFont
            ));

            String creatorName = userRepository.findNameById(Integer.parseInt(String.valueOf(mongoDocument.get("created_by"))));

            pdfDocument.add(new Paragraph(
                    "提交人: " + (creatorName != null ? creatorName : "未知用户"),
                    chineseFont
            ));

            // Close the PDF document
            pdfDocument.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error exporting document to PDF: " + e.getMessage(), e);
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional("transactionManager")
    public void deleteSubmissionLog(String submissionId, String collectionName) {
        // 1. Check if collection exists
        if (!mongoTemplate.collectionExists(collectionName)) {
            throw new RuntimeException("Collection not found: " + collectionName);
        }

        // 2. Find the document by _id
        ObjectId oid = new ObjectId(submissionId);
        Query idQuery = new Query(Criteria.where("_id").is(oid));
        Document document = mongoTemplate.findOne(idQuery, Document.class, collectionName);

        // 3. Check if it exists
        if (document == null) {
            throw new RuntimeException("Document not found: " + submissionId);
        }

        // 4. Check if it has version_group_id
        Object versionGroupId = document.get("version_group_id");

        if (versionGroupId != null) {
            // Find all submission IDs in this group to delete from snapshot service
            Query groupQuery = new Query(Criteria.where("version_group_id").is(versionGroupId));
            groupQuery.fields().include("_id");
            List<Document> docs = mongoTemplate.find(groupQuery, Document.class, collectionName);

            List<String> idsToDelete = docs.stream()
                    .map(d -> d.getObjectId("_id").toString())
                    .collect(Collectors.toList());

            // Delete from snapshot service
            qcSnapshotSubmissionService.deleteBySubmissionIds(idsToDelete);

            // Delete associated alert records
            alertRecordService.deleteBySubmissionIds(idsToDelete);

            // Delete associated approval assignments
            for (String id : idsToDelete) {
                qcApprovalAssignmentRepository.deleteBySubmissionId(id);
            }

            // Delete all documents with the same version_group_id
            Query deleteGroupQuery = new Query(Criteria.where("version_group_id").is(versionGroupId));
            mongoTemplate.remove(deleteGroupQuery, collectionName);
        } else {
            // Delete from snapshot service
            qcSnapshotSubmissionService.deleteBySubmissionId(submissionId);

            // Delete associated alert records
            alertRecordService.deleteBySubmissionIds(Collections.singletonList(submissionId));

            // Delete associated approval assignments
            qcApprovalAssignmentRepository.deleteBySubmissionId(submissionId);

            // Delete only this document
            mongoTemplate.remove(idQuery, collectionName);
        }
    }

    @Override
    public Document getRawDocumentBySubmissionId(String submissionId, String collectionName) {
        if (!ObjectId.isValid(submissionId)) {
            throw new IllegalArgumentException("Invalid submissionId format: " + submissionId);
        }

        if (!mongoTemplate.collectionExists(collectionName)) {
            throw new RuntimeException("Collection not found: " + collectionName);
        }

        Query query = new Query(Criteria.where("_id").is(new ObjectId(submissionId)));
        Document rawDocument = mongoTemplate.findOne(query, Document.class, collectionName);

        if (rawDocument == null) {
            return null;
        }

        // 🔥 清洗掉不需要的 key (keep related_* fields for form edit pre-population)
        Document cleanedDocument = new Document();
        for (Map.Entry<String, Object> entry : rawDocument.entrySet()) {
            String key = entry.getKey();
            if (key.equals("exceeded_info") || key.equals("e-signature") || key.equals("approval_info") || key.equals("_id") || key.equals("created_at") || key.equals("created_by")) {
                continue; // 跳过
            }
            cleanedDocument.put(key, entry.getValue());
        }

        return cleanedDocument;
    }



    private String convertToLocalTime(String utcTime) {
        try {
            // Parse the ISO-8601 UTC timestamp into an Instant
            utcTime = utcTime + 'Z';
            Instant instant = Instant.parse(utcTime);

            // Convert the Instant to the system's default timezone
            ZonedDateTime localZoned = instant.atZone(ZoneId.systemDefault());

            // Format the time as "YYYY-MM-DD HH:mm:ss"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return localZoned.format(formatter);

        } catch (Exception e) {
            // Fallback: return the original UTC time if parsing fails
            return utcTime;
        }
    }

    @Override
    public List<Map<String, Object>> getFormTemplateFieldList(Long formId) {
        String formTemplateJson = qcFormTemplateRepository.findFormTemplateJsonById(formId);
        if (formTemplateJson == null || formTemplateJson.isEmpty()) {
            throw new RuntimeException("Form template JSON not found for formId: " + formId);
        }

        List<Map<String, Object>> fieldList = new ArrayList<>();

        try {
            Document formTemplate = Document.parse(formTemplateJson);
            List<Document> widgetList = (List<Document>) formTemplate.get("widgetList");
            if (widgetList != null) {
                extractFieldDetailList(widgetList, fieldList); // 🔁 recursive helper
            }
        } catch (Exception e) {
            logger.error("Error parsing form template JSON for formId: {}", formId, e);
            throw new RuntimeException("Error parsing form template JSON", e);
        }

        return fieldList;
    }

    private void extractFieldDetailList(List<Document> widgetList, List<Map<String, Object>> fieldList) {
        for (Document widget : widgetList) {
            Document options = (Document) widget.get("options");
            if (options != null && options.containsKey("name") && options.containsKey("label")) {
                Map<String, Object> field = new HashMap<>();
                field.put("name", options.getString("name"));
                field.put("label", options.getString("label"));
                if (options.containsKey("optionItems")) {
                    field.put("optionItems", options.get("optionItems"));
                }
                fieldList.add(field);
            }

            List<Document> nestedWidgetList = (List<Document>) widget.get("widgetList");
            if (nestedWidgetList != null) {
                extractFieldDetailList(nestedWidgetList, fieldList);
            }

            List<Document> cols = (List<Document>) widget.get("cols");
            if (cols != null) {
                for (Document col : cols) {
                    List<Document> colWidgetList = (List<Document>) col.get("widgetList");
                    if (colWidgetList != null) {
                        extractFieldDetailList(colWidgetList, fieldList);
                    }
                }
            }
        }
    }


}
