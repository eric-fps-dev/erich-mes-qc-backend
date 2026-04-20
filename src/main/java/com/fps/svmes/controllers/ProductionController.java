//package com.fps.svmes.controllers;
//
//import com.fps.svmes.dto.dtos.production.ProductDTO;
//import com.fps.svmes.dto.dtos.production.ProductionWorkOrderDTO;
//import com.fps.svmes.dto.dtos.production.RawMaterialDTO;
//import com.fps.svmes.services.ProductionService;
//import com.fps.svmes.dto.responses.ResponseResult;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/production")
//@Tag(name = "Production Module API", description = "API for Production Module")
//public class ProductionController {
//
//    @Autowired
//    private ProductionService productionService;
//
//    private static final Logger logger = LoggerFactory.getLogger(ProductionController.class);
//
//    /**
//     * Get all active production work orders.
//     *
//     * @return List of ProductionWorkOrderDTO
//     */
//    @GetMapping("/workorders")
//    @Operation(summary = "Get all active production work orders", description = "Fetch all production work orders with status = 1")
//    public ResponseResult<List<ProductionWorkOrderDTO>> getAllProductionWorkOrders() {
//        try {
//            List<ProductionWorkOrderDTO> workOrders = productionService.getAllProductionWorkOrders();
//            return ResponseResult.success(workOrders);
//        } catch (Exception e) {
//            logger.error("Error retrieving all production work orders");
//            return ResponseResult.fail("Failed to retrieve all production work orders", e);
//        }
//    }
//
//    /**
//     * Get a specific production work order by ID.
//     *
//     * @param id Production work order ID
//     * @return ProductionWorkOrderDTO
//     */
//    @GetMapping("/workorders/{id}")
//    @Operation(summary = "Get a specific production work order by ID", description = "Fetch a production work order with status = 1 by its ID")
//    public ResponseResult<ProductionWorkOrderDTO> getProductionWorkOrderById(@PathVariable Integer id) {
//        try {
//            ProductionWorkOrderDTO workOrder = productionService.getProductionWorkOrderById(id);
//            return ResponseResult.success(workOrder);
//        } catch (Exception e) {
//            logger.error("Error retrieving work order");
//            return ResponseResult.fail("Failed to retrieve work order", e);
//        }
//    }
//
//    // Products
//
//    /**
//     * Get all active products.
//     *
//     * @return List of ProductDTO
//     */
//    @GetMapping("/products")
//    @Operation(summary = "Get all active products", description = "Fetch all products with status = 1")
//    public ResponseResult<List<ProductDTO>> getAllProducts() {
//        try {
//            List<ProductDTO> products = productionService.getAllProduct();
//            return ResponseResult.success(products);
//        } catch (Exception e) {
//            logger.error("Error retrieving all products");
//            return ResponseResult.fail("Failed to retrieve all products", e);
//        }
//
//    }
//
//    /**
//     * Get a specific product by ID.
//     *
//     * @param id Product ID
//     * @return ProductDTO
//     */
//    @GetMapping("/products/{id}")
//    @Operation(summary = "Get a specific product by ID", description = "Fetch a product with status = 1 by its ID")
//    public ResponseResult<ProductDTO> getProductById(@PathVariable Integer id) {
//        try {
//            ProductDTO product = productionService.getProductById(id);
//            return ResponseResult.success(product);
//        } catch (Exception e) {
//            logger.error("Error retrieving product");
//            return ResponseResult.fail("Failed to retrieve product", e);
//        }
//
//    }
//
//    // Raw Materials
//
//    /**
//     * Get all active raw materials.
//     *
//     * @return List of RawMaterialDTO
//     */
//    @GetMapping("/rawmaterials")
//    @Operation(summary = "Get all active raw materials", description = "Fetch all raw materials with status = 1")
//    public ResponseResult<List<RawMaterialDTO>> getAllRawMaterials() {
//        try {
//            List<RawMaterialDTO> rawMaterials = productionService.getAllRawMaterials();
//            return ResponseResult.success(rawMaterials);
//        } catch (Exception e) {
//            logger.error("Error retrieving all raw materials");
//            return ResponseResult.fail("Failed to retrieve all raw materials", e);
//        }
//
//    }
//
//    /**
//     * Get a specific raw material by ID.
//     *
//     * @param id Raw material ID
//     * @return RawMaterialDTO
//     */
//    @GetMapping("/rawmaterials/{id}")
//    @Operation(summary = "Get a specific raw material by ID", description = "Fetch a raw material with status = 1 by its ID")
//    public ResponseResult<RawMaterialDTO> getRawMaterialById(@PathVariable Integer id) {
//        try {
//            RawMaterialDTO rawMaterial = productionService.getRawMaterialById(id);
//            return ResponseResult.success(rawMaterial);
//        } catch (Exception e) {
//            logger.error("Error retrieving raw material");
//            return ResponseResult.fail("Failed to retrieve raw material", e);
//        }
//    }
//}
