package com.fps.svmes.services;

import com.fps.svmes.models.sql.production.MaterialFormTemplateLink;

import java.util.List;

public interface MaterialFormTemplateLinkService {
    MaterialFormTemplateLink create(MaterialFormTemplateLink link);
    MaterialFormTemplateLink update(Long id, MaterialFormTemplateLink link);
    /** Active (status = 1) links only. */
    List<MaterialFormTemplateLink> find(String scope, Long materialId);
    MaterialFormTemplateLink findById(Long id);

    /** Soft delete: sets status = 0. */
    void delete(Long id);
}
