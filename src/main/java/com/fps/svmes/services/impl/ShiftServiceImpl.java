//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.user.ShiftDTO;
//import com.fps.svmes.models.sql.user.Shift;
//import com.fps.svmes.repositories.jpaRepo.user.ShiftRepository;
//import com.fps.svmes.services.ShiftService;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class ShiftServiceImpl implements ShiftService {
//
//    @Autowired
//    private ShiftRepository shiftRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    public ShiftDTO createShift(ShiftDTO shiftDTO) {
//        Shift shift = modelMapper.map(shiftDTO, Shift.class);
//        return modelMapper.map(shiftRepository.save(shift), ShiftDTO.class);
//    }
//
//    @Override
//    public ShiftDTO getShiftById(Integer shiftId) {
//        Shift shift = shiftRepository.findByIdAndStatus(shiftId, 1)
//                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + shiftId));
//        return modelMapper.map(shift, ShiftDTO.class);
//    }
//
//    @Override
//    public List<ShiftDTO> getAllShifts() {
//        List<Shift> activeShifts = shiftRepository.findByStatus(1);
//        return activeShifts.stream()
//                .map(shift -> modelMapper.map(shift, ShiftDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public ShiftDTO updateShift(Integer shiftId, ShiftDTO shiftDTO) {
//        Shift existingShift = shiftRepository.findById(shiftId)
//                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + shiftId));
//
//        if (shiftDTO.getName() != null) {
//            existingShift.setName(shiftDTO.getName());
//        }
//        if (shiftDTO.getDescription() != null) {
//            existingShift.setDescription(shiftDTO.getDescription());
//        }
//        if (shiftDTO.getStartTime() != null) {
//            existingShift.setStartTime(shiftDTO.getStartTime());
//        }
//        if (shiftDTO.getEndTime() != null) {
//            existingShift.setEndTime(shiftDTO.getEndTime());
//        }
//        if (shiftDTO.getGraceMinute() != null) {
//            existingShift.setGraceMinute(shiftDTO.getGraceMinute());
//        }
//
//        existingShift.setUpdateDetails(shiftDTO.getUpdatedBy(), shiftDTO.getStatus());
//        return modelMapper.map(shiftRepository.save(existingShift), ShiftDTO.class);
//    }
//
//    @Override
//    public void deleteShift(Integer shiftId, Integer userId) {
//        Shift shift = shiftRepository.findByIdAndStatus(shiftId, 1)
//                .orElseThrow(() -> new RuntimeException("Shift not found with ID: " + shiftId));
//        shift.setUpdateDetails(userId, 0); // soft delete
//        shiftRepository.save(shift);
//    }
//}
