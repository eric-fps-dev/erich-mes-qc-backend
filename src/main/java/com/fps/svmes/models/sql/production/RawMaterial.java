//package com.fps.svmes.models.sql.production;
//
//import com.fps.shared.entity.BaseEntity;
//import com.fps.svmes.models.sql.Common;
//import jakarta.persistence.*;
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//
//@Data
//@NoArgsConstructor
//@EqualsAndHashCode(callSuper = true)
//@Entity
//@Table(name = "raw_material", schema = "inventory_management")
//public class RawMaterial extends BaseEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
//    @Column(name = "raw_material_class_id")
//    private Integer rawMaterialClassId;
//
//    @Column(name = "quantity_uom")
//    private String quantityUom;
//
//    @Column(name = "unit_price")
//    private BigDecimal unitPrice;
//
//    @Column(name = "minimum_inventory")
//    private BigDecimal minimumInventory;
//
//    @Column(name = "image_path")
//    private String imagePath;
//
//    @Column(name = "vendor_id")
//    private Integer vendorId;
//
//    @Column(name = "held_stock")
//    private BigDecimal heldStock;
//
//    @Column(name = "available_stock")
//    private BigDecimal availableStock;
//
//    @Column(name = "current_stock")
//    private BigDecimal currentStock;
//
//    private BigDecimal increment;
//
//    @Column(name = "price_uom")
//    private String priceUom;
//
//}
