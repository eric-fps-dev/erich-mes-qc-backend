//package com.fps.svmes.dto.dtos.production;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fps.svmes.dto.dtos.CommonDTO;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//import java.math.BigDecimal;
//
//@Data
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@EqualsAndHashCode(callSuper = true)
//public class ProductDTO extends CommonDTO {
//
//    @JsonProperty("id")
//    private Long id;
//
//    @JsonProperty("name")
//    private String name;
//
//    @JsonProperty("description")
//    private String description;
//
//    @JsonProperty("product_code")
//    private String productCode;
//
//    @JsonProperty("personnel_class")
//    private String personnelClass;
//
//    @JsonProperty("comments")
//    private String comments;
//
//    @JsonProperty("image_paths")
//    private String imagePaths;
//
//    @JsonProperty("product_class_id")
//    private Integer productClassId;
//
//    @JsonProperty("qs_code")
//    private String qsCode;
//
//    @JsonProperty("unit_resource_cost")
//    private BigDecimal unitResourceCost;
//
//    @JsonProperty("unit_sales_price")
//    private BigDecimal unitSalesPrice;
//
//    @JsonProperty("produced_quantity")
//    private Integer producedQuantity;
//
//    @JsonProperty("produced_quantity_unit_id")
//    private Integer producedQuantityUnitId;
//}
