package com.fps.svmes.models.sql.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fps.svmes.models.sql.Common;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "qc_suggested_batch", schema = "quality_management")
@Data
@EqualsAndHashCode(callSuper = true)
public class SuggestedBatch extends Common {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @JsonProperty("id")
    private Long id;

    @Column(name = "code")
    @JsonProperty("code")
    private String code;

    @Column(name = "name", nullable = false)
    @JsonProperty("name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_suggested_product_id")
    @JsonProperty("suggested_product")
    private SuggestedProduct suggestedProduct;

    @Column(name = "description")
    @JsonProperty("description")
    private String description;
}