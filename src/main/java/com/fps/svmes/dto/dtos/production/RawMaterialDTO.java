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
//public class RawMaterialDTO extends CommonDTO {
//
//    private Integer id;
//
//    private String name;
//
//    private String type;
//
//    private String description;
//
//    private String code;
//
//    private String spec;
//
//    @JsonProperty("raw_material_class_id")
//    private Integer rawMaterialClassId;
//
//    @JsonProperty("quantity_uom")
//    private String quantityUom;
//
//    @JsonProperty("unit_price")
//    private BigDecimal unitPrice;
//
//    @JsonProperty("minimum_inventory")
//    private BigDecimal minimumInventory;
//
//    @JsonProperty("image_path")
//    private String imagePath;
//
//    @JsonProperty("vendor_id")
//    private Integer vendorId;
//
//    @JsonProperty("held_stock")
//    private BigDecimal heldStock;
//
//    @JsonProperty("available_stock")
//    private BigDecimal availableStock;
//
//    @JsonProperty("current_stock")
//    private BigDecimal currentStock;
//
//    private BigDecimal increment;
//
//    @JsonProperty("price_uom")
//    private String priceUom;
//}
