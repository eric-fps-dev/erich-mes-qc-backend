package com.fps.svmes.services;

import com.fps.svmes.dto.PagedResultDTO;
import com.fps.svmes.dto.dtos.approval.ApprovalInstanceListItemDTO;
import com.fps.svmes.models.sql.production.MaterialQcRequest;

public interface MaterialQcRequestService {
    MaterialQcRequest create(MaterialQcRequest request);
    MaterialQcRequest update(Long id, MaterialQcRequest request);

    /** All filter params are optional — pass null to leave a filter off. */
    PagedResultDTO<MaterialQcRequest> findAll(Long materialId, String materialLotNumber, Long formTemplateId, int page, int size);
    MaterialQcRequest findById(Long id);

    /** DRAFT -> PENDING: resolves and snapshots the suggested product/batch for this material + lot. */
    MaterialQcRequest confirm(Long id);

    /** PENDING -> PASSED | FAILED. */
    MaterialQcRequest setResult(Long id, boolean passed);

    /**
     * QC form records related to this request's material/lot/template, resolving
     * suggested product/batch on the fly (works even before confirm, for preview).
     */
    PagedResultDTO<ApprovalInstanceListItemDTO> getRelatedRecords(Long id, int page, int size);
}
