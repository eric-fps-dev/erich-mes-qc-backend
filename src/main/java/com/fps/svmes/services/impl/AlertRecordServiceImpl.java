package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.alert.*;
import com.fps.svmes.dto.dtos.production.SuggestedBatchDTO;
import com.fps.svmes.dto.dtos.production.SuggestedProductDTO;
import com.fps.svmes.dto.dtos.qcForm.QcFormTemplateDTO;
import com.fps.svmes.dto.dtos.user.UserDTO;
import com.fps.svmes.dto.requests.alert.AlertRecordFilterRequest;
import com.fps.svmes.models.sql.alert.*;
import com.fps.svmes.models.sql.production.SuggestedProduct;
import com.fps.svmes.models.sql.qcForm.QcFormTemplate;
import com.fps.svmes.repositories.jpaRepo.alert.AlertRecordLogRepository;
import com.fps.svmes.repositories.jpaRepo.alert.AlertRecordRepository;
import com.fps.svmes.repositories.jpaRepo.alert.AlertStatusRepository;
import com.fps.svmes.repositories.jpaRepo.alert.RiskLevelRepository;
import com.fps.svmes.repositories.jpaRepo.production.SuggestedBatchRepository;
import com.fps.svmes.repositories.jpaRepo.production.SuggestedProductRepository;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.repositories.jpaRepo.user.UserRepository;
import com.fps.svmes.services.AlertRecordService;
import com.fps.svmes.utils.AlertDiffBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordRepository alertRecordRepository;
    private final ModelMapper modelMapper;
    private final AlertRecordLogRepository alertRecordLogRepository;
    private final AlertStatusRepository alertStatusRepository;
    private final RiskLevelRepository riskLevelRepository;
    private final UserRepository userRepository;
    private final SuggestedProductRepository suggestedProductRepository;
    private final SuggestedBatchRepository suggestedBatchRepository;
    private final QcFormTemplateRepository qcFormTemplateRepository;

    @Override
    @Transactional
    public AlertRecordDTO create(AlertRecordDTO dto) {
        AlertRecord entity = modelMapper.map(dto, AlertRecord.class);
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setAlertTime(dto.getAlertTime());
        entity.setInspectionItemKey(dto.getInspectionItemKey());
        entity.setInspectionItemLabel(dto.getInspectionItemLabel());
        entity.setAlertType(dto.getAlertType());
        entity.setOptionItems(dto.getOptionItems());
        entity.setOptionLabels(dto.getOptionLabels());
        entity.setInvalidOptionItems(dto.getInvalidOptionItems());
        entity.setInvalidOptionLabels(dto.getInvalidOptionLabels());

        // 1. Map productIds to AlertProduct
        if (dto.getProductIds() != null) {
            List<AlertProduct> alertProducts = dto.getProductIds().stream()
                    .map(pid -> {
                        AlertProduct ap = new AlertProduct();
                        ap.setId(new AlertProductId(null, pid));
                        ap.setProductId(pid);
                        ap.setAlert(entity);
                        return ap;
                    }).toList();
            entity.setAlertProducts(alertProducts);
        }

        // 2. Map batchIds to AlertBatch
        if (dto.getBatchIds() != null) {
            List<AlertBatch> alertBatches = dto.getBatchIds().stream()
                    .map(bid -> {
                        AlertBatch ab = new AlertBatch();
                        ab.setId(new AlertBatchId(null, bid));
                        ab.setBatchId(bid);
                        ab.setAlert(entity);
                        return ab;
                    }).toList();
            entity.setAlertBatches(alertBatches);
        }

        // 3. Map inspectorIds to AlertInspector
        if (dto.getInspectorIds() != null) {
            List<AlertInspector> inspectors = dto.getInspectorIds().stream()
                    .map(iid -> {
                        AlertInspector ai = new AlertInspector();
                        ai.setId(new AlertInspectorId(null, iid));
                        ai.setInspectorId(iid);
                        ai.setAlert(entity);
                        return ai;
                    }).toList();
            entity.setAlertInspectors(inspectors);
        }

        // 4. Map reviewerIds to AlertReviewer
        if (dto.getReviewerIds() != null) {
            List<AlertReviewer> reviewers = dto.getReviewerIds().stream()
                    .map(rid -> {
                        AlertReviewer ar = new AlertReviewer();
                        ar.setId(new AlertReviewerId(null, rid));
                        ar.setReviewerId(rid);
                        ar.setAlert(entity);
                        return ar;
                    }).toList();
            entity.setAlertReviewers(reviewers);
        }

        AlertRecord saved = alertRecordRepository.save(entity);

        // Create full snapshot diff for create operation
        Map<String, List<String>> diff = new HashMap<>();
        if (dto.getRpn() != null) {
            diff.put("rpn", List.of("-", String.valueOf(dto.getRpn())));
        }
        if (dto.getAlertStatus() != null) {
            diff.put("alert_status", List.of("-", String.valueOf(dto.getAlertStatus())));
        }

        if (!diff.isEmpty()) {
            AlertRecordLog log = new AlertRecordLog();
            log.setAlertRecordId(saved.getId());
            log.setOperation("create");
            log.setDiff(diff);
            log.setCreatedBy(dto.getCreatedBy());
            log.setUpdatedBy(dto.getCreatedBy());
            log.setCreatedAt(dto.getCreatedAt());
            log.setUpdatedAt(dto.getUpdatedAt());
            alertRecordLogRepository.save(log);
        }

        return modelMapper.map(saved, AlertRecordDTO.class);
    }

    @Override
    public Page<DetailedAlertRecordDTO> getDetailedList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AlertRecord> entityPage = alertRecordRepository.findAll(pageable);

        List<AlertRecord> alertList = entityPage.getContent();

        // Collect IDs for batch fetching
        Set<Long> templateIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        Set<Long> batchIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        Set<Integer> statusIds = new HashSet<>();
        Set<Integer> riskLevelIds = new HashSet<>();

        for (AlertRecord alert : alertList) {
            templateIds.add(alert.getQcFormTemplateId());
            if (alert.getAlertStatus() != null) statusIds.add(alert.getAlertStatus());
            if (alert.getRiskLevelId() != null) riskLevelIds.add(alert.getRiskLevelId());

            alert.getAlertProducts().forEach(p -> productIds.add(p.getProductId()));
            alert.getAlertBatches().forEach(b -> batchIds.add(b.getBatchId()));
            alert.getAlertInspectors().forEach(i -> userIds.add(i.getInspectorId()));
            alert.getAlertReviewers().forEach(r -> userIds.add(r.getReviewerId()));
        }

        // Batch fetch all referenced data
        Map<Long, QcFormTemplate> templateMap = qcFormTemplateRepository.findAllById(templateIds)
                .stream().collect(Collectors.toMap(QcFormTemplate::getId, t -> t));

        Map<Long, SuggestedProductDTO> productMap = suggestedProductRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> modelMapper.map(p, SuggestedProductDTO.class)));

        Map<Long, SuggestedBatchDTO> batchMap = suggestedBatchRepository.findAllById(batchIds)
                .stream().collect(Collectors.toMap(
                        b -> b.getId(),
                        b -> modelMapper.map(b, SuggestedBatchDTO.class)));

        Map<Long, UserDTO> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(
                        u -> u.getId(),
                        u -> modelMapper.map(u, UserDTO.class)));

        Map<Integer, AlertStatusDTO> statusMap = alertStatusRepository.findAllById(statusIds)
                .stream().collect(Collectors.toMap(
                        s -> s.getId(),
                        s -> modelMapper.map(s, AlertStatusDTO.class)));

        Map<Integer, RiskLevelDTO> riskMap = riskLevelRepository.findAllById(riskLevelIds)
                .stream().collect(Collectors.toMap(
                        r -> r.getId(),
                        r -> modelMapper.map(r, RiskLevelDTO.class)));

        // Map results
        List<DetailedAlertRecordDTO> dtos = alertList.stream().map(alert -> {
            DetailedAlertRecordDTO dto = new DetailedAlertRecordDTO();

            dto.setId(alert.getId());
            dto.setAlertCode(alert.getAlertCode());
            dto.setAlertTime(alert.getAlertTime());
            dto.setInspectionValue(alert.getInspectionValue());
            dto.setRpn(alert.getRpn());
            dto.setCreatedAt(alert.getCreatedAt());
            dto.setStatus(alert.getStatus());
            dto.setUpperControlLimit(alert.getUpperControlLimit());
            dto.setLowerControlLimit(alert.getLowerControlLimit());

            dto.setAlertType(alert.getAlertType());
            dto.setOptionItems(alert.getOptionItems());
            dto.setOptionLabels(alert.getOptionLabels());
            dto.setInvalidOptionItems(alert.getInvalidOptionItems());
            dto.setInvalidOptionItemsLabels(alert.getInvalidOptionLabels());

            if ("options".equals(alert.getAlertType())) {
                dto.setControlRange(
                        alert.getOptionLabels() != null ? String.join(", ", alert.getOptionLabels()) : null
                );
            } else if ("number".equals(alert.getAlertType()) &&
                    alert.getLowerControlLimit() != null && alert.getUpperControlLimit() != null) {
                dto.setControlRange(alert.getLowerControlLimit() + " - " + alert.getUpperControlLimit());
            }

            // Template
            QcFormTemplate template = templateMap.get(alert.getQcFormTemplateId());
            if (template != null) {
                QcFormTemplateDTO formDto = new QcFormTemplateDTO();
                formDto.setId(template.getId());
                formDto.setName(template.getName());
                formDto.setFormTemplateJson(null);
                dto.setQcFormTemplate(formDto);
            }

            // Inspection item
            InspectionItemDTO item = new InspectionItemDTO();
            item.setKey(alert.getInspectionItemKey());
            item.setLabel(alert.getInspectionItemLabel());
            dto.setInspectionItem(item);

            // Products
            dto.setProducts(alert.getAlertProducts().stream()
                    .map(p -> productMap.get(p.getProductId()))
                    .filter(Objects::nonNull)
                    .toList());

            // Batches
            dto.setBatches(alert.getAlertBatches().stream()
                    .map(b -> batchMap.get(b.getBatchId()))
                    .filter(Objects::nonNull)
                    .toList());

            // Inspectors
            dto.setInspectors(alert.getAlertInspectors().stream()
                    .map(i -> userMap.get(i.getInspectorId()))
                    .filter(Objects::nonNull)
                    .toList());

            // Reviewers
            dto.setReviewers(alert.getAlertReviewers().stream()
                    .map(r -> userMap.get(r.getReviewerId()))
                    .filter(Objects::nonNull)
                    .toList());

            // Alert status
            if (alert.getAlertStatus() != null) {
                dto.setAlertStatus(statusMap.get(alert.getAlertStatus()));
            }

            // Risk level
            if (alert.getRiskLevelId() != null) {
                dto.setRiskLevel(riskMap.get(alert.getRiskLevelId()));
            }

            return dto;
        }).toList();

        return new PageImpl<>(dtos, pageable, entityPage.getTotalElements());
    }


    @Override
    @Transactional
    public AlertRecordDTO updateRecord(Long alertId, Integer newRpn, Long userId) {
        AlertRecord entity = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("alert record not exist"));

        AlertRecordDTO oldDto = modelMapper.map(entity, AlertRecordDTO.class);

        int newStatus = newRpn < 20 ? 2 : 1;

        entity.setRpn(newRpn);
        // Set risk level according to tooltip rules
        entity.setRiskLevelId(newRpn >= 200 ? 3 : newRpn >= 100 ? 2 : 1);
        entity.setAlertStatus(newStatus);
        entity.setUpdatedBy(Math.toIntExact(userId));
        entity.setUpdatedAt(OffsetDateTime.now());

        alertRecordRepository.save(entity);

        AlertRecordDTO newDto = modelMapper.map(entity, AlertRecordDTO.class);
        Map<String, List<String>> diff = AlertDiffBuilder.buildDiff(oldDto, newDto);

        if (!diff.isEmpty()) {
            AlertRecordLog log = new AlertRecordLog();
            log.setAlertRecordId(alertId);
            log.setOperation("update");
            log.setDiff(diff);
            log.setCreatedBy(Math.toIntExact(userId));
            log.setUpdatedBy(Math.toIntExact(userId));
            log.setCreatedAt(OffsetDateTime.now());
            log.setUpdatedAt(OffsetDateTime.now());
            alertRecordLogRepository.save(log);
        }

        return modelMapper.map(entity, AlertRecordDTO.class);
    }

    @Override
    @Transactional
    public AlertRecordDTO deleteRecord(Long alertId, Long userId) {
        AlertRecord entity = alertRecordRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("alert record not exist"));

        AlertRecordDTO oldDto = modelMapper.map(entity, AlertRecordDTO.class);

        // Logical deletion (archive)
        entity.setStatus(0);
        entity.setUpdatedBy(Math.toIntExact(userId));
        entity.setUpdatedAt(OffsetDateTime.now());

        alertRecordRepository.save(entity);

        // Logging
        Map<String, List<String>> diff = new HashMap<>();
        diff.put("status", List.of("1", "0")); // active → archived

        AlertRecordLog log = new AlertRecordLog();
        log.setAlertRecordId(alertId);
        log.setOperation("delete");
        log.setDiff(diff);
        log.setCreatedBy(Math.toIntExact(userId));
        log.setUpdatedBy(Math.toIntExact(userId));
        log.setCreatedAt(OffsetDateTime.now());
        log.setUpdatedAt(OffsetDateTime.now());

        alertRecordLogRepository.save(log);

        return modelMapper.map(entity, AlertRecordDTO.class);
    }

    @Override
    public AlertSummaryDTO getAlertSummary() {
//        List<AlertRecord> allRecords = alertRecordRepository.findAll();
        List<AlertRecord> allRecords = alertRecordRepository.findByStatus(1);

        // 1. 告警状态统计（按 alertStatus 外键聚合，展示名称）s
        Map<Integer, Long> alertStatusRaw = allRecords.stream()
                .filter(r -> r.getAlertStatus() != null)
                .collect(Collectors.groupingBy(AlertRecord::getAlertStatus, Collectors.counting()));

            // 加载 ID 对应的 AlertStatus 实体
        Map<Integer, AlertStatus> alertStatusMap = alertStatusRepository.findAllById(alertStatusRaw.keySet())
                .stream()
                .collect(Collectors.toMap(AlertStatus::getId, a -> a));

            // 映射为 <name, count>
        Map<String, Long> alertStatusCounts = alertStatusRaw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Optional.ofNullable(alertStatusMap.get(e.getKey()))
                                .map(AlertStatus::getName)
                                .orElse("未知"),
                        Map.Entry::getValue
                ));

        // 2. 风险等级统计（用 id 聚合，显示 label）
        Map<Integer, Long> riskRaw = allRecords.stream()
                .filter(r -> r.getRiskLevelId() != null)
                .collect(Collectors.groupingBy(AlertRecord::getRiskLevelId, Collectors.counting()));

        Map<Integer, RiskLevel> riskMap = riskLevelRepository.findAllById(riskRaw.keySet()).stream()
                .collect(Collectors.toMap(RiskLevel::getId, r -> r));

        Map<String, Long> riskLevelCounts = riskRaw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Optional.ofNullable(riskMap.get(e.getKey()))
                                .map(RiskLevel::getName)
                                .orElse("未知"),
                        Map.Entry::getValue
                ));

        // 3. 产品统计（用 productId 聚合，显示名称）
        Map<Long, Long> productRaw = allRecords.stream()
                .flatMap(r -> r.getAlertProducts().stream())
                .collect(Collectors.groupingBy(AlertProduct::getProductId, Collectors.counting()));

        Map<Long, SuggestedProduct> productMap = suggestedProductRepository.findAllById(productRaw.keySet()).stream()
                .collect(Collectors.toMap(SuggestedProduct::getId, p -> p));

        Map<String, Long> productCounts = productRaw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Optional.ofNullable(productMap.get(e.getKey()))
                                .map(SuggestedProduct::getName)
                                .orElse("未知"),
                        Map.Entry::getValue,
                        Long::sum // if the name of the products are the same then we combine the value
                ));

        // 4. 检测项统计（key 聚合，label 展示）
        Map<String, Long> inspectionRaw = allRecords.stream()
                .filter(r -> r.getInspectionItemKey() != null)
                .collect(Collectors.groupingBy(AlertRecord::getInspectionItemKey, Collectors.counting()));

        Map<String, String> keyToLabel = allRecords.stream()
                .filter(r -> r.getInspectionItemKey() != null && r.getInspectionItemLabel() != null)
                .collect(Collectors.toMap(AlertRecord::getInspectionItemKey, AlertRecord::getInspectionItemLabel, (a, b) -> a)); // 去重保留第一个

        Map<String, Long> inspectionItemCounts = inspectionRaw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Optional.ofNullable(keyToLabel.get(e.getKey())).orElse("[未知检测项]"),
                        Map.Entry::getValue,
                        Long::sum
                ));

        // 封装结果
        AlertSummaryDTO summary = new AlertSummaryDTO();
        summary.setInspectionItemCounts(inspectionItemCounts);
        summary.setRiskLevelCounts(riskLevelCounts);
        summary.setProductCounts(productCounts);
        summary.setAlertStatusCounts(alertStatusCounts);

        return summary;
    }

    @Override
    public Page<DetailedAlertRecordDTO> filterAlertRecords(AlertRecordFilterRequest request) {
        Pageable pageable;
        if (request.getSort() != null && StringUtils.hasText(request.getSort().getProp())) {
            Sort.Direction direction = "descending".equals(request.getSort().getOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(direction, request.getSort().getProp()));
        } else {
            pageable = PageRequest.of(request.getPage(), request.getSize());
        }

        Specification<AlertRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getFilters() != null) {
                String statusId = request.getFilters().get("alertStatusId");
                if (StringUtils.hasText(statusId)) {
                    predicates.add(cb.equal(root.get("alertStatus"), Integer.valueOf(statusId)));
                }

                String riskLevelId = request.getFilters().get("riskLevelId");
                if (StringUtils.hasText(riskLevelId)) {
                    predicates.add(cb.equal(root.get("riskLevelId"), Integer.valueOf(riskLevelId)));
                }

                String productId = request.getFilters().get("suggestedProductId");
                if (StringUtils.hasText(productId)) {
                    Join<AlertRecord, AlertProduct> productJoin = root.join("alertProducts", JoinType.LEFT);
                    predicates.add(cb.equal(productJoin.get("productId"), Long.valueOf(productId)));
                }

                String batchId = request.getFilters().get("suggestedBatchId");
                if (StringUtils.hasText(batchId)) {
                    Join<AlertRecord, AlertBatch> batchJoin = root.join("alertBatches", JoinType.LEFT);
                    predicates.add(cb.equal(batchJoin.get("batchId"), Long.valueOf(batchId)));
                }

                String general = request.getFilters().get("generalSearch");
                if (StringUtils.hasText(general)) {
                    predicates.add(cb.like(root.get("alertCode"), "%" + general + "%"));
                }

                String[] dateRange = request.getFilters().get("dateRange") != null
                        ? request.getFilters().get("dateRange").split(",") : null;

                if (dateRange != null && dateRange.length == 2) {
                    try {
                        OffsetDateTime start = OffsetDateTime.parse(dateRange[0]);
                        OffsetDateTime end = OffsetDateTime.parse(dateRange[1]);
                        predicates.add(cb.between(root.get("alertTime"), start.toLocalDateTime(), end.toLocalDateTime()));
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException("Invalid date format for UTC ISO string", e);
                    }
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AlertRecord> entityPage = alertRecordRepository.findAll(spec, pageable);
        List<AlertRecord> alertList = entityPage.getContent();

        // 以下逻辑复制自 getDetailedList() 没时间refactor
        Set<Long> templateIds = new HashSet<>();
        Set<Long> productIds = new HashSet<>();
        Set<Long> batchIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        Set<Integer> statusIds = new HashSet<>();
        Set<Integer> riskLevelIds = new HashSet<>();

        for (AlertRecord alert : alertList) {
            templateIds.add(alert.getQcFormTemplateId());
            if (alert.getAlertStatus() != null) statusIds.add(alert.getAlertStatus());
            if (alert.getRiskLevelId() != null) riskLevelIds.add(alert.getRiskLevelId());
            alert.getAlertProducts().forEach(p -> productIds.add(p.getProductId()));
            alert.getAlertBatches().forEach(b -> batchIds.add(b.getBatchId()));
            alert.getAlertInspectors().forEach(i -> userIds.add(i.getInspectorId()));
            alert.getAlertReviewers().forEach(r -> userIds.add(r.getReviewerId()));
        }

        Map<Long, QcFormTemplate> templateMap = qcFormTemplateRepository.findAllById(templateIds)
                .stream().collect(Collectors.toMap(QcFormTemplate::getId, t -> t));

        Map<Long, SuggestedProductDTO> productMap = suggestedProductRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(p -> p.getId(), p -> modelMapper.map(p, SuggestedProductDTO.class)));

        Map<Long, SuggestedBatchDTO> batchMap = suggestedBatchRepository.findAllById(batchIds)
                .stream().collect(Collectors.toMap(b -> b.getId(), b -> modelMapper.map(b, SuggestedBatchDTO.class)));

        Map<Long, UserDTO> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(u -> u.getId(), u -> modelMapper.map(u, UserDTO.class)));

        Map<Integer, AlertStatusDTO> statusMap = alertStatusRepository.findAllById(statusIds)
                .stream().collect(Collectors.toMap(s -> s.getId(), s -> modelMapper.map(s, AlertStatusDTO.class)));

        Map<Integer, RiskLevelDTO> riskMap = riskLevelRepository.findAllById(riskLevelIds)
                .stream().collect(Collectors.toMap(r -> r.getId(), r -> modelMapper.map(r, RiskLevelDTO.class)));

        List<DetailedAlertRecordDTO> dtos = alertList.stream().map(alert -> {
            DetailedAlertRecordDTO dto = new DetailedAlertRecordDTO();

            dto.setId(alert.getId());
            dto.setAlertCode(alert.getAlertCode());
            dto.setAlertTime(alert.getAlertTime());
            dto.setInspectionValue(alert.getInspectionValue());
            dto.setRpn(alert.getRpn());
            dto.setCreatedAt(alert.getCreatedAt());
            dto.setStatus(alert.getStatus());
            dto.setUpperControlLimit(alert.getUpperControlLimit());
            dto.setLowerControlLimit(alert.getLowerControlLimit());

            dto.setAlertType(alert.getAlertType());
            dto.setOptionItems(alert.getOptionItems());
            dto.setOptionLabels(alert.getOptionLabels());
            dto.setInvalidOptionItems(alert.getInvalidOptionItems());
            dto.setInvalidOptionItemsLabels(alert.getInvalidOptionLabels());
            dto.setInputOptionItems(alert.getInputOptionItems());
            dto.setInputOptionItemsLabels(alert.getInputOptionItemsLabels());
            dto.setSubmissionId(alert.getSubmissionId());

            if ("options".equals(alert.getAlertType()) && alert.getOptionLabels() != null) {
                List<String> validLabels = new ArrayList<>(alert.getOptionLabels());
                if (alert.getInvalidOptionLabels() != null) {
                    validLabels.removeAll(alert.getInvalidOptionLabels());
                }
                dto.setControlRange(String.join(", ", validLabels));
            } else if ("number".equals(alert.getAlertType()) &&
                    alert.getLowerControlLimit() != null &&
                    alert.getUpperControlLimit() != null) {
                dto.setControlRange(alert.getLowerControlLimit() + " - " + alert.getUpperControlLimit());
            }

            QcFormTemplate template = templateMap.get(alert.getQcFormTemplateId());
            if (template != null) {
                QcFormTemplateDTO formDto = new QcFormTemplateDTO();
                formDto.setId(template.getId());
                formDto.setName(template.getName());
                formDto.setFormTemplateJson(null);
                dto.setQcFormTemplate(formDto);
            }

            InspectionItemDTO item = new InspectionItemDTO();
            item.setKey(alert.getInspectionItemKey());
            item.setLabel(alert.getInspectionItemLabel());
            dto.setInspectionItem(item);

            dto.setProducts(alert.getAlertProducts().stream()
                    .map(p -> productMap.get(p.getProductId()))
                    .filter(Objects::nonNull).toList());

            dto.setBatches(alert.getAlertBatches().stream()
                    .map(b -> batchMap.get(b.getBatchId()))
                    .filter(Objects::nonNull).toList());

            dto.setInspectors(alert.getAlertInspectors().stream()
                    .map(i -> userMap.get(i.getInspectorId()))
                    .filter(Objects::nonNull).toList());

            dto.setReviewers(alert.getAlertReviewers().stream()
                    .map(r -> userMap.get(r.getReviewerId()))
                    .filter(Objects::nonNull).toList());

            if (alert.getAlertStatus() != null) {
                dto.setAlertStatus(statusMap.get(alert.getAlertStatus()));
            }

            if (alert.getRiskLevelId() != null) {
                dto.setRiskLevel(riskMap.get(alert.getRiskLevelId()));
            }

            return dto;
        }).toList();

        return new PageImpl<>(dtos, pageable, entityPage.getTotalElements());
    }


    // original
//    public Page<DetailedAlertRecordDTO> getDetailedList(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//        Page<AlertRecord> entityPage = alertRecordRepository.findAll((org.springframework.data.domain.Pageable) pageable);
//
//        return entityPage.map(alert -> {
//            DetailedAlertRecordDTO dto = new DetailedAlertRecordDTO();
//
//            dto.setId(alert.getId());
//            dto.setAlertCode(alert.getAlertCode());
//            dto.setAlertTime(alert.getAlertTime());
//            dto.setInspectionValue(alert.getInspectionValue());
//            dto.setRpn(alert.getRpn());
//            dto.setCreatedAt(alert.getCreatedAt());
//            dto.setStatus(alert.getStatus());
//
//            // control_range
//            if (alert.getLowerControlLimit() != null && alert.getUpperControlLimit() != null) {
//                dto.setControlRange(alert.getLowerControlLimit() + " - " + alert.getUpperControlLimit());
//            }
//
//            // qc_form_template
//            QcFormTemplateDTO formDto = new QcFormTemplateDTO();
//            formDto.setId(alert.getQcFormTemplateId());
//            formDto.setFormTemplateJson(null); // exclude this field for frontend
//            // Load name from template
//            qcFormTemplateRepository.findById(alert.getQcFormTemplateId())
//                    .ifPresent(template -> formDto.setName(template.getName()));
//            dto.setQcFormTemplate(formDto);
//
//            // inspection_item (key only for now)
//            InspectionItemDTO item = new InspectionItemDTO();
//            item.setKey(String.valueOf(alert.getInspectionItemKey()));
//            item.setLabel(alert.getInspectionItemLabel()); // ✅ from DB
//            dto.setInspectionItem(item);
//
//            // products
//            dto.setProducts(alert.getAlertProducts().stream()
//                    .map(p -> {
//                        Long pid = p.getProductId();
//                        return suggestedProductRepository.findById(pid)
//                                .map(product -> modelMapper.map(product, SuggestedProductDTO.class))
//                                .orElse(null);
//                    })
//                    .filter(p -> p != null)
//                    .toList());
//
//            // batches
//            dto.setBatches(alert.getAlertBatches().stream()
//                    .map(b -> {
//                        Long bid = b.getBatchId();
//                        return suggestedBatchRepository.findById(bid)
//                                .map(batch -> modelMapper.map(batch, SuggestedBatchDTO.class))
//                                .orElse(null);
//                    })
//                    .filter(b -> b != null)
//                    .toList());
//
//            // inspectors
//            dto.setInspectors(alert.getAlertInspectors().stream()
//                    .map(i -> userRepository.findById(Math.toIntExact(i.getInspectorId()))
//                            .map(user -> modelMapper.map(user, UserDTO.class))
//                            .orElse(null))
//                    .filter(u -> u != null)
//                    .toList());
//
//            // reviewers
//            dto.setReviewers(alert.getAlertReviewers().stream()
//                    .map(r -> userRepository.findById(Math.toIntExact(r.getReviewerId()))
//                            .map(user -> modelMapper.map(user, UserDTO.class))
//                            .orElse(null))
//                    .filter(u -> u != null)
//                    .toList());
//
//            // alert_status
//            if (alert.getAlertStatus() != null) {
//                alertStatusRepository.findById(alert.getAlertStatus())
//                        .ifPresent(status -> dto.setAlertStatus(modelMapper.map(status, AlertStatusDTO.class)));
//            }
//
//            // risk_level
//            if (alert.getRiskLevelId() != null) {
//                riskLevelRepository.findById(alert.getRiskLevelId())
//                        .ifPresent(level -> dto.setRiskLevel(modelMapper.map(level, RiskLevelDTO.class)));
//            }
//
    @Override
    @Transactional
    public void deleteBySubmissionIds(List<String> submissionIds) {
        if (submissionIds != null && !submissionIds.isEmpty()) {
            alertRecordRepository.deleteBySubmissionIdIn(submissionIds);
        }
    }
}
