//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.production.ProductDTO;
//import com.fps.svmes.dto.dtos.production.ProductionWorkOrderDTO;
//import com.fps.svmes.dto.dtos.production.RawMaterialDTO;
//import com.fps.svmes.models.sql.production.Product;
//import com.fps.svmes.models.sql.production.ProductionWorkOrder;
//import com.fps.svmes.models.sql.production.RawMaterial;
//import com.fps.svmes.repositories.jpaRepo.production.ProductRepository;
//import com.fps.svmes.repositories.jpaRepo.production.ProductionWorkOrderRepository;
//import com.fps.svmes.repositories.jpaRepo.production.RawMaterialRepository;
//import com.fps.svmes.services.ProductionService;
//import jakarta.persistence.EntityNotFoundException;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class ProductionServiceImpl implements ProductionService {
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private ProductionWorkOrderRepository productionWorkOrderRepository;
//
//    @Autowired
//    private RawMaterialRepository rawMaterialRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    /**
//     * Fetch all active production work orders (status = 1).
//     *
//     * @return List of ProductionWorkOrderDTO
//     */
//    @Transactional(readOnly = true)
//    public List<ProductionWorkOrderDTO> getAllProductionWorkOrders() {
//        return productionWorkOrderRepository.findByStatus(1)
//                .stream()
//                .map(productionWorkOrder -> modelMapper.map(productionWorkOrder, ProductionWorkOrderDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Fetch a specific production work order by ID (status = 1).
//     *
//     * @param id Production Work Order ID
//     * @return ProductionWorkOrderDTO
//     */
//    @Transactional(readOnly = true)
//    public ProductionWorkOrderDTO getProductionWorkOrderById(Integer id) {
//        try {
//            ProductionWorkOrder productionWorkOrder = productionWorkOrderRepository.findByIdAndStatus(id, 1);
//            return modelMapper.map(productionWorkOrder, ProductionWorkOrderDTO.class);
//        } catch (Exception e) {
//            throw new EntityNotFoundException("Production work order with ID " + id + " not found");
//        }
//    }
//
//    /**
//     * Fetch all active products (status = 1).
//     *
//     * @return List of ProductDTO
//     */
//    @Transactional(readOnly = true)
//    public List<ProductDTO> getAllProduct() {
//        return productRepository.findByStatus(1)
//                .stream()
//                .map(product -> modelMapper.map(product, ProductDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Fetch a specific product by ID (status = 1).
//     *
//     * @param id Product ID
//     * @return ProductDTO
//     */
//    @Transactional(readOnly = true)
//    public ProductDTO getProductById(Integer id) {
//        try {
//            Product product = productRepository.findByIdAndStatus(id, 1);
//            return modelMapper.map(product, ProductDTO.class);
//        } catch (Exception e) {
//            throw new EntityNotFoundException("Product with ID " + id + " not found");
//        }
//    }
//    /**
//     * Fetch all active raw materials (status = 1).
//     *
//     * @return List of RawMaterialDTO
//     */
//    @Transactional(readOnly = true)
//    public List<RawMaterialDTO> getAllRawMaterials() {
//        return rawMaterialRepository.findByStatus(1)
//                .stream()
//                .map(rawMaterial -> modelMapper.map(rawMaterial, RawMaterialDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Fetch a specific raw material by ID (status = 1).
//     *
//     * @param id Raw Material ID
//     * @return RawMaterialDTO
//     */
//    @Transactional(readOnly = true)
//    public RawMaterialDTO getRawMaterialById(Integer id) {
//        try {
//            RawMaterial rawMaterial = rawMaterialRepository.findByIdAndStatus(id, 1);
//            return modelMapper.map(rawMaterial, RawMaterialDTO.class);
//        } catch (Exception e) {
//            throw new EntityNotFoundException("Raw material with ID " + id + " not found");
//        }
//    }
//}
