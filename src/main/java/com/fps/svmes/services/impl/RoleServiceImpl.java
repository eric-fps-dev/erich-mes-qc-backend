//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.user.RoleDTO;
//import com.fps.svmes.models.sql.user.Role;
//import com.fps.svmes.repositories.jpaRepo.user.RoleRepository;
//import com.fps.svmes.services.RoleService;
//import lombok.RequiredArgsConstructor;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class RoleServiceImpl implements RoleService {
//
//    @Autowired
//    private RoleRepository roleRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<RoleDTO> getAllRoles() {
//        return roleRepository.findAll()
//                .stream()
//                .map(role -> modelMapper.map(role, RoleDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public RoleDTO createRole(RoleDTO roleDTO) {
//        Role role = modelMapper.map(roleDTO, Role.class);
//        Role savedRole = roleRepository.save(role);
//        return modelMapper.map(savedRole, RoleDTO.class);
//    }
//
//    @Override
//    @Transactional
//    public RoleDTO updateRole(Short id, RoleDTO roleDTO) {
//        Optional<Role> optionalRole = roleRepository.findById(id);
//
//        if (optionalRole.isPresent()) {
//            Role existingRole = optionalRole.get();
//
//            // Update only the fields that are provided
//            if (roleDTO.getName() != null) {
//                existingRole.setName(roleDTO.getName());
//            }
//            if (roleDTO.getDescription() != null) {
//                existingRole.setDescription(roleDTO.getDescription());
//            }
//            if (roleDTO.getElTagType() != null) {
//                existingRole.setElTagType(roleDTO.getElTagType());
//            }
//
//            Role updatedRole = roleRepository.save(existingRole);
//            return modelMapper.map(updatedRole, RoleDTO.class);
//        } else {
//            throw new RuntimeException("Role with ID " + id + " not found");
//        }
//    }
//
//    @Override
//    @Transactional
//    public void deleteRole(Short id) {
//        if (!roleRepository.existsById(id)) {
//            throw new RuntimeException("Role with ID " + id + " not found");
//        }
//        roleRepository.deleteById(id);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public RoleDTO getRoleById(Short id) {
//        return roleRepository.findById(id)
//                .map(role -> modelMapper.map(role, RoleDTO.class))
//                .orElseThrow(() -> new RuntimeException("Role with ID " + id + " not found"));
//    }
//}
