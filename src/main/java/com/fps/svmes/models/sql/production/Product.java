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
//@Table(name = "product", schema = "inventory_management")
//public class Product extends BaseEntity {
//
//    @Id
//    @Column(name = "id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @Column(name = "name")
//    private String name;
//
//    @Column(name = "description")
//    private String description;
//
//    @Column(name = "product_code")
//    private String productCode;
//
//    @Column(name = "personnel_class")
//    private String personnelClass;
//
//    @Column(name = "comments")
//    private String comments;
//
//    @Column(name = "image_paths")
//    private String imagePaths;
//
//    @Column(name = "product_class_id")
//    private Integer productClassId;
//
//    @Column(name = "qs_code")
//    private String qsCode;
//
//    @Column(name = "unit_resource_cost")
//    private BigDecimal unitResourceCost;
//
//    @Column(name = "unit_sales_price")
//    private BigDecimal unitSalesPrice;
//
//    @Column(name = "produced_quantity")
//    private Integer producedQuantity;
//
//    @Column(name = "produced_quantity_unit_id")
//    private Integer producedQuantityUnitId;
//}
