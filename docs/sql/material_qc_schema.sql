-- Material QC integration — new tables for mes-qc-backend (schema: quality_management)
-- Matches models/sql/production/{MaterialSuggestedProductMapping,MaterialLotSuggestedBatchMapping,
-- MaterialFormTemplateLink,MaterialQcRequest}.java exactly. Run against the dev database.

CREATE TABLE quality_management.material_suggested_product_mapping (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(255),
    qc_suggested_product_id BIGINT NOT NULL REFERENCES quality_management.qc_suggested_product(id),
    created_at TIMESTAMPTZ,
    created_by INT,
    updated_at TIMESTAMPTZ,
    updated_by INT,
    status SMALLINT NOT NULL DEFAULT 1
);

-- Only one *active* mapping per material — delete() soft-deletes (status = 0), and a
-- plain UNIQUE constraint would keep blocking re-mapping the same material afterwards.
CREATE UNIQUE INDEX material_suggested_product_mapping_material_id_active_uidx
    ON quality_management.material_suggested_product_mapping (material_id)
    WHERE status = 1;

CREATE TABLE quality_management.material_lot_suggested_batch_mapping (
    id BIGSERIAL PRIMARY KEY,
    material_lot_number VARCHAR(100) NOT NULL,
    qc_suggested_batch_id BIGINT NOT NULL REFERENCES quality_management.qc_suggested_batch(id),
    created_at TIMESTAMPTZ,
    created_by INT,
    updated_at TIMESTAMPTZ,
    updated_by INT,
    status SMALLINT NOT NULL DEFAULT 1
);

-- See comment above: only one active mapping per lot number, enforced on active rows only.
CREATE UNIQUE INDEX material_lot_suggested_batch_mapping_lot_active_uidx
    ON quality_management.material_lot_suggested_batch_mapping (material_lot_number)
    WHERE status = 1;

CREATE TABLE quality_management.material_form_template_link (
    id BIGSERIAL PRIMARY KEY,
    scope VARCHAR(20) NOT NULL,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(255),
    qc_form_template_id BIGINT NOT NULL REFERENCES quality_management.qc_form_template(id),
    qc_form_template_name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ,
    created_by INT,
    updated_at TIMESTAMPTZ,
    updated_by INT,
    status SMALLINT NOT NULL DEFAULT 1
);

-- "One active link per (materialId, scope)", enforced on active rows only — see comment above.
CREATE UNIQUE INDEX material_form_template_link_material_scope_active_uidx
    ON quality_management.material_form_template_link (material_id, scope)
    WHERE status = 1;

CREATE TABLE quality_management.material_qc_request (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    material_name VARCHAR(255),
    material_lot_number VARCHAR(100) NOT NULL,
    qc_form_template_id BIGINT REFERENCES quality_management.qc_form_template(id),
    qc_form_template_name VARCHAR(255),
    state VARCHAR(20) NOT NULL DEFAULT 'draft',
    resolved_suggested_product_id BIGINT,
    resolved_suggested_batch_id BIGINT,
    created_at TIMESTAMPTZ,
    created_by INT,
    updated_at TIMESTAMPTZ,
    updated_by INT,
    status SMALLINT NOT NULL DEFAULT 1,
    -- Optimistic lock: guards state transitions (confirm/setResult) against lost updates
    -- from concurrent requests on the same row.
    version BIGINT NOT NULL DEFAULT 0
);
