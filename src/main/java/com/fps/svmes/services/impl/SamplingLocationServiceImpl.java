//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.dispatch.SamplingLocationDTO;
//import com.fps.svmes.models.sql.taskSchedule.SamplingLocation;
//import com.fps.svmes.repositories.jpaRepo.dispatch.SamplingLocationRepository;
//import com.fps.svmes.services.SamplingLocationService;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.OffsetDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class SamplingLocationServiceImpl implements SamplingLocationService {
//
//    @Autowired
//    private SamplingLocationRepository repository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    public SamplingLocationDTO createSamplingLocation(SamplingLocationDTO samplingLocationDTO) {
//        SamplingLocation samplingLocation = modelMapper.map(samplingLocationDTO, SamplingLocation.class);
//        SamplingLocation savedSamplingLocation = repository.save(samplingLocation);
//        return modelMapper.map(savedSamplingLocation, SamplingLocationDTO.class);
//    }
//
//    @Override
//    public SamplingLocationDTO updateSamplingLocation(Long id, SamplingLocationDTO samplingLocationDTO) {
//        SamplingLocation samplingLocation = repository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Sampling Location not found with ID: " + id)
//        );
//
//        if (samplingLocationDTO.getName() != null) {
//            samplingLocation.setName(samplingLocationDTO.getName());
//        }
//
//        if (samplingLocation.getDescription() != null) {
//            samplingLocation.setDescription(samplingLocationDTO.getDescription());
//        }
//
//
//        samplingLocation.setUpdateDetails(samplingLocationDTO.getUpdatedBy(), samplingLocationDTO.getStatus());
//        SamplingLocation updatedSamplingLocation = repository.save(samplingLocation);
//        return modelMapper.map(updatedSamplingLocation, SamplingLocationDTO.class);
//    }
//
//    @Override
//    public SamplingLocationDTO getSamplingLocationById(Long id) {
//        SamplingLocation samplingLocation = repository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Sampling Location not found with ID: " + id)
//        );
//        return modelMapper.map(samplingLocation, SamplingLocationDTO.class);
//    }
//
//    @Override
//    public List<SamplingLocationDTO> getAllActiveSamplingLocations() {
//        List<SamplingLocation> activeLocations = repository.findByStatus(1);
//        return activeLocations.stream()
//                .map(location -> modelMapper.map(location, SamplingLocationDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public void deleteSamplingLocation(Long id, Integer userId) {
//        SamplingLocation samplingLocation = repository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Sampling Location not found with ID: " + id)
//        );
//        samplingLocation.setUpdateDetails(userId, 0); // Mark as inactive
//        repository.save(samplingLocation);
//    }
//}
//
