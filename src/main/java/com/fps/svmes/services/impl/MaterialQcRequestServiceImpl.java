package com.fps.svmes.services.impl;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceListItemDTO;
import com.fps.svmes.dto.requests.ApprovalInstanceQueryRequest;
import com.fps.svmes.enums.production.MaterialQcRequestState;
import com.fps.svmes.exceptions.MaterialQcRequestNotFoundException;
import com.fps.svmes.models.sql.production.MaterialLotSuggestedBatchMapping;
import com.fps.svmes.models.sql.production.MaterialQcRequest;
import com.fps.svmes.models.sql.production.MaterialSuggestedProductMapping;
import com.fps.svmes.repositories.jpaRepo.production.MaterialLotSuggestedBatchMappingRepository;
import com.fps.svmes.repositories.jpaRepo.production.MaterialQcRequestRepository;
import com.fps.svmes.repositories.jpaRepo.production.MaterialSuggestedProductMappingRepository;
import com.fps.svmes.services.MaterialQcRequestService;
import com.fps.svmes.services.QcFormDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialQcRequestServiceImpl implements MaterialQcRequestService {

    private final MaterialQcRequestRepository repository;
    private final MaterialSuggestedProductMappingRepository suggestedProductMappingRepository;
    private final MaterialLotSuggestedBatchMappingRepository suggestedBatchMappingRepository;
    private final QcFormDataService qcFormDataService;

    @Override
    public MaterialQcRequest create(MaterialQcRequest request) {
        // Ignore any client-supplied id — otherwise repository.save() treats this as an
        // update and can silently overwrite an unrelated existing row.
        request.setId(null);
        request.setState(MaterialQcRequestState.DRAFT.dbValue());
        request.setCreationDetails(request.getCreatedBy(), 1);
        return repository.save(request);
    }

    @Override
    public MaterialQcRequest update(Long id, MaterialQcRequest request) {
        MaterialQcRequest existing = requireById(id);
        if (!MaterialQcRequestState.DRAFT.matches(existing.getState())) {
            throw new IllegalStateException("Only draft requests can be edited");
        }
        if (request.getMaterialId() != null) {
            existing.setMaterialId(request.getMaterialId());
        }
        if (request.getMaterialName() != null) {
            existing.setMaterialName(request.getMaterialName());
        }
        if (request.getMaterialLotNumber() != null) {
            existing.setMaterialLotNumber(request.getMaterialLotNumber());
        }
        if (request.getFormTemplateId() != null) {
            existing.setFormTemplateId(request.getFormTemplateId());
        }
        if (request.getFormTemplateName() != null) {
            existing.setFormTemplateName(request.getFormTemplateName());
        }
        existing.setUpdateDetails(request.getUpdatedBy(), existing.getStatus());
        return repository.save(existing);
    }

    @Override
    public PagedResultDTO<MaterialQcRequest> findAll(Long materialId, String materialLotNumber, Long formTemplateId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        Page<MaterialQcRequest> result = repository.findByFilters(
                materialId, materialLotNumber, formTemplateId, PageRequest.of(safePage, safeSize));
        return new PagedResultDTO<>(result.getContent(), result.getTotalElements(), result.getTotalPages(), safePage, safeSize);
    }

    @Override
    public MaterialQcRequest findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public MaterialQcRequest confirm(Long id) {
        MaterialQcRequest request = requireById(id);
        if (!MaterialQcRequestState.DRAFT.matches(request.getState())) {
            throw new IllegalStateException("Only draft requests can be confirmed");
        }
        if (request.getFormTemplateId() == null) {
            throw new IllegalArgumentException("A form template must be set before the request can be confirmed.");
        }

        Long suggestedProductId = resolveSuggestedProductId(request.getMaterialId());
        if (suggestedProductId == null) {
            throw new IllegalArgumentException(
                    "No suggested product is mapped to material id " + request.getMaterialId()
                            + " — link the material first.");
        }
        Long suggestedBatchId = resolveSuggestedBatchId(request.getMaterialLotNumber());
        if (suggestedBatchId == null) {
            throw new IllegalArgumentException(
                    "No suggested batch is mapped to material lot " + request.getMaterialLotNumber()
                            + " — link the material lot first.");
        }

        request.setResolvedSuggestedProductId(suggestedProductId);
        request.setResolvedSuggestedBatchId(suggestedBatchId);
        request.setState(MaterialQcRequestState.PENDING.dbValue());
        request.setUpdatedAt(OffsetDateTime.now());
        return repository.save(request);
    }

    @Override
    public MaterialQcRequest setResult(Long id, boolean passed) {
        MaterialQcRequest request = requireById(id);
        if (!MaterialQcRequestState.PENDING.matches(request.getState())) {
            throw new IllegalStateException("Only pending requests can be marked pass/fail");
        }
        request.setState((passed ? MaterialQcRequestState.PASSED : MaterialQcRequestState.FAILED).dbValue());
        request.setUpdatedAt(OffsetDateTime.now());
        return repository.save(request);
    }

    @Override
    public PagedResultDTO<ApprovalInstanceListItemDTO> getRelatedRecords(Long id, int page, int size) {
        MaterialQcRequest request = requireById(id);

        Long suggestedProductId = request.getResolvedSuggestedProductId() != null
                ? request.getResolvedSuggestedProductId()
                : resolveSuggestedProductId(request.getMaterialId());
        Long suggestedBatchId = request.getResolvedSuggestedBatchId() != null
                ? request.getResolvedSuggestedBatchId()
                : resolveSuggestedBatchId(request.getMaterialLotNumber());

        if (suggestedProductId == null || suggestedBatchId == null) {
            return new PagedResultDTO<>(List.of(), 0, 0, page, size);
        }

        ApprovalInstanceQueryRequest query = new ApprovalInstanceQueryRequest();
        query.setPage(page);
        query.setSize(size);
        query.setFormTemplateId(request.getFormTemplateId());
        query.setSuggestedProductId(suggestedProductId);
        query.setSuggestedBatchId(suggestedBatchId);
        return qcFormDataService.getApprovalInstances(query);
    }

    private Long resolveSuggestedProductId(Long materialId) {
        if (materialId == null) {
            return null;
        }
        return suggestedProductMappingRepository.findByMaterialIdAndStatus(materialId, 1)
                .map(MaterialSuggestedProductMapping::getSuggestedProductId)
                .orElse(null);
    }

    private Long resolveSuggestedBatchId(String materialLotNumber) {
        if (materialLotNumber == null || materialLotNumber.isBlank()) {
            return null;
        }
        return suggestedBatchMappingRepository.findByMaterialLotNumberAndStatus(materialLotNumber, 1)
                .map(MaterialLotSuggestedBatchMapping::getSuggestedBatchId)
                .orElse(null);
    }

    private MaterialQcRequest requireById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new MaterialQcRequestNotFoundException("Material QC request not found: " + id));
    }
}
