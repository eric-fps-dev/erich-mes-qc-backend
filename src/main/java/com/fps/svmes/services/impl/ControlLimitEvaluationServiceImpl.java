package com.fps.svmes.services.impl;

import com.fps.svmes.dto.dtos.alert.AlertRecordDTO;
import com.fps.svmes.dto.dtos.alert.ExceededFieldInfoDTO;
import com.fps.svmes.repositories.jpaRepo.qcForm.QcFormTemplateRepository;
import com.fps.svmes.services.AlertRecordService;
import com.fps.svmes.services.ControlLimitEvaluationService;
import com.fps.svmes.services.QcFormTemplateService;
import com.fps.svmes.services.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ControlLimitEvaluationServiceImpl implements ControlLimitEvaluationService {

    private final RecipeService recipeService;
    private final AlertRecordService alertRecordService;
    private final QcFormTemplateService qcFormTemplateService;
    private final QcFormTemplateRepository qcFormTemplateRepository;

    @Override
    public void evaluateAndTriggerAlerts(Long templateId, Long userId, Map<String, Object> formData, String submissionId) {
        var setting = recipeService.getByQcFormTemplateId(templateId);
        if (setting == null || setting.getControlLimits() == null) return;

        setting.getControlLimits().forEach((fieldKey, limit) -> {
            Object valueObj = formData.get(fieldKey);
            if (valueObj == null) return;

            boolean shouldAlert = false;
            BigDecimal numericValue = null;

            try {
                // Case 1: 数值类型字段
                if (limit.getUpperControlLimit() != null || limit.getLowerControlLimit() != null) {
                    double value = Double.parseDouble(valueObj.toString());
                    Double upper = limit.getUpperControlLimit();
                    Double lower = limit.getLowerControlLimit();
                    boolean isAbove = upper != null && value > upper;
                    boolean isBelow = lower != null && value < lower;
                    if (isAbove || isBelow) {
                        shouldAlert = true;
                        numericValue = BigDecimal.valueOf(value);
                    }
                }

                // Case 2: option valid_keys 字段（支持单选或多选）
                if (limit.getValidKeys() != null && !limit.getValidKeys().isEmpty()) {
                    List<String> selectedValues;
                    if (valueObj instanceof List<?>) {
                        selectedValues = ((List<?>) valueObj).stream().map(Object::toString).toList();
                    } else {
                        selectedValues = List.of(valueObj.toString());
                    }

                    for (String val : selectedValues) {
                        if (!limit.getValidKeys().contains(val)) {
                            shouldAlert = true;
                            break;
                        }
                    }
                }

                // 触发告警
                if (shouldAlert) {
                    AlertRecordDTO alert = new AlertRecordDTO();

                    alert.setQcFormTemplateId(templateId);
                    alert.setInspectionItemKey(fieldKey);
                    alert.setInspectionItemLabel(limit.getLabel());
                    alert.setAlertTime(OffsetDateTime.now(ZoneOffset.UTC));
                    alert.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    alert.setCreatedBy(userId.intValue());
                    alert.setAlertStatus(1);
                    alert.setStatus(1);
                    alert.setRpn(50);
                    alert.setRiskLevelId(1);
                    alert.setAlertCode("AL" + OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
                    alert.setSubmissionId(submissionId);

                    // 设置数值型报警信息
                    if (numericValue != null) {
                        alert.setAlertType("number");
                        alert.setInspectionValue(numericValue);
                        alert.setUpperControlLimit(limit.getUpperControlLimit() != null ? BigDecimal.valueOf(limit.getUpperControlLimit()) : null);
                        alert.setLowerControlLimit(limit.getLowerControlLimit() != null ? BigDecimal.valueOf(limit.getLowerControlLimit()) : null);
                    }

                    // 设置选项型报警信息
                    if (limit.getValidKeys() != null && !limit.getValidKeys().isEmpty()) {
                        alert.setAlertType("options");
                        List<String> selectedValues = valueObj instanceof List<?> list
                                ? list.stream().map(Object::toString).toList()
                                : List.of(valueObj.toString());

                        // 1. All option values; option_labels stores only the VALID ones
                        //    so that control_range can be derived directly by joining option_labels
                        List<String> allOptionValues = limit.getOptionItems() != null
                                ? limit.getOptionItems().stream().map(item -> item.getValue()).toList()
                                : List.of();
                        List<String> validOptionLabels = limit.getOptionItems() != null
                                ? limit.getOptionItems().stream()
                                        .filter(item -> limit.getValidKeys().contains(item.getValue()))
                                        .map(item -> item.getLabel())
                                        .toList()
                                : List.of();

                        alert.setOptionItems(allOptionValues);
                        alert.setOptionLabels(validOptionLabels);

                        // 2. 从用户输入中识别无效项
                        List<String> invalidValues = selectedValues.stream()
                                .filter(val -> !limit.getValidKeys().contains(val))
                                .toList();
                        alert.setInvalidOptionItems(invalidValues);
                        alert.setInvalidOptionLabels(
                                invalidValues.stream()
                                        .map(invalidVal ->
                                                limit.getOptionItems().stream()
                                                        .filter(item -> item.getValue().equals(invalidVal))
                                                        .map(item -> item.getLabel())
                                                        .findFirst()
                                                        .orElse(invalidVal)
                                        )
                                        .toList()
                        );

                        // 识别出不合格项
                        List<String> invalidOptions = selectedValues.stream()
                                .filter(val -> !limit.getValidKeys().contains(val))
                                .toList();
                        alert.setInvalidOptionItems(invalidOptions);
                        alert.setInvalidOptionLabels(
                                invalidOptions.stream().map(invalidVal ->
                                        limit.getOptionItems().stream()
                                                .filter(item -> item.getValue().equals(invalidVal))
                                                .map(item -> item.getLabel())
                                                .findFirst()
                                                .orElse(invalidVal)
                                ).toList()
                        );

                        // 3. 用户输入项（全部） → 存 inputOptionItems 和 inputOptionLabels
                        alert.setInputOptionItems(selectedValues);
                        alert.setInputOptionItemsLabels(
                                selectedValues.stream()
                                        .map(val -> limit.getOptionItems().stream()
                                                .filter(item -> item.getValue().equals(val))
                                                .map(item -> item.getLabel())
                                                .findFirst()
                                                .orElse(val))
                                        .toList()
                        );
                    }

                    // 人员字段
                    extractLongList(alert::setProductIds, formData.get("related_product_ids"));
                    extractLongList(alert::setBatchIds, formData.get("related_batch_ids"));
                    extractLongList(alert::setInspectorIds, formData.get("related_inspector_ids"));
                    extractLongList(alert::setReviewerIds, formData.get("related_reviewer_ids"));

                    alertRecordService.create(alert);
                }
            } catch (Exception e) {
                // skip on error
            }
        });
    }

    private void extractLongList(java.util.function.Consumer<List<Long>> setter, Object valueObj) {
        if (valueObj instanceof List<?> list && !list.isEmpty()) {
            setter.accept(list.stream().map(Object::toString).map(Long::valueOf).toList());
        }
    }

    @Override
    public Map<String, ExceededFieldInfoDTO> evaluateExceededInfo(Long templateId, Map<String, Object> formData) {
        Map<String, ExceededFieldInfoDTO> resultMap = new HashMap<>();
        var setting = recipeService.getByQcFormTemplateId(templateId);
        if (setting == null || setting.getControlLimits() == null) return resultMap;

        setting.getControlLimits().forEach((fieldKey, limit) -> {
            Object valueObj = formData.get(fieldKey);
            if (valueObj == null) return;

            try {
                ExceededFieldInfoDTO info = new ExceededFieldInfoDTO();
                info.setType(limit.getValidKeys() != null ? "options" : "number");
                info.setValue(valueObj);

                if ("number".equals(info.getType())) {
                    double val = Double.parseDouble(valueObj.toString());
                    info.setLowerLimit(limit.getLowerControlLimit() != null ? BigDecimal.valueOf(limit.getLowerControlLimit()) : null);
                    info.setUpperLimit(limit.getUpperControlLimit() != null ? BigDecimal.valueOf(limit.getUpperControlLimit()) : null);
                    if (info.getUpperLimit() != null && val > info.getUpperLimit().doubleValue()) {
                        info.setResult("high");
                    } else if (info.getLowerLimit() != null && val < info.getLowerLimit().doubleValue()) {
                        info.setResult("low");
                    }
                } else {
                    List<String> selected = valueObj instanceof List<?> list
                            ? list.stream().map(Object::toString).toList()
                            : List.of(valueObj.toString());
                    List<String> valid = limit.getValidKeys();
                    List<String> invalid = selected.stream().filter(s -> !valid.contains(s)).toList();
                    info.setValidOptions(valid);
                    info.setInvalidOptions(invalid);

                    List<String> validLabels = valid.stream()
                            .map(val -> limit.getOptionItems().stream()
                                    .filter(item -> item.getValue().equals(val))
                                    .map(item -> item.getLabel())
                                    .findFirst()
                                    .orElse(val)
                            ).toList();
                    info.setValidOptionLabels(validLabels);

                    List<String> invalidLabels = invalid.stream()
                            .map(val -> limit.getOptionItems().stream()
                                    .filter(item -> item.getValue().equals(val))
                                    .map(item -> item.getLabel())
                                    .findFirst()
                                    .orElse(val)
                            ).toList();
                    info.setInvalidOptionLabels(invalidLabels);

                    if (!invalid.isEmpty()) info.setResult("invalid");
                }

                resultMap.put(fieldKey, info);
            } catch (Exception e) {
                // log or skip
            }
        });

        return resultMap;
    }

}
