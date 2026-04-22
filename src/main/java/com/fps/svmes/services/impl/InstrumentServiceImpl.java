//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.dispatch.InstrumentDTO;
//import com.fps.svmes.models.sql.taskSchedule.Instrument;
//import com.fps.svmes.repositories.jpaRepo.dispatch.InstrumentRepository;
//import com.fps.svmes.services.InstrumentService;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class InstrumentServiceImpl implements InstrumentService {
//
//    @Autowired
//    private InstrumentRepository instrumentRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    public InstrumentDTO createInstrument(InstrumentDTO instrumentRequest) {
//        Instrument instrument = modelMapper.map(instrumentRequest, Instrument.class);
//        return modelMapper.map(instrumentRepository.save(instrument), InstrumentDTO.class);
//    }
//
//    @Override
//    public InstrumentDTO getInstrumentById(Long id) {
//        Instrument instrument = instrumentRepository.findByIdAndStatus(id, 1).orElseThrow(
//                ()-> new RuntimeException("Instrument not found with ID: " + id)
//        );
//
//        return modelMapper.map(instrument, InstrumentDTO.class); // Only fetch active instruments
//    }
//
//    @Override
//    public List<InstrumentDTO> getAllInstruments() {
//        List<Instrument> activeInstruments = instrumentRepository.findByStatus(1);
//        return activeInstruments.stream()
//                .map(instrument -> modelMapper.map(instrument, InstrumentDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public InstrumentDTO updateInstrument(Long id, InstrumentDTO instrumentDTO) {
//        Instrument existingInstrument =  instrumentRepository.findById(id).orElseThrow(
//                () -> new RuntimeException("Instrument not found with ID: " + id)
//        );
//
//        if (instrumentDTO.getType() != null) {
//            existingInstrument.setType(instrumentDTO.getType());
//        }
//        if (instrumentDTO.getName() != null) {
//            existingInstrument.setName(instrumentDTO.getName());
//        }
//        if (instrumentDTO.getDescription() != null) {
//            existingInstrument.setDescription(instrumentDTO.getDescription());
//        }
//        if (instrumentDTO.getManufacturer() != null) {
//            existingInstrument.setManufacturer(instrumentDTO.getManufacturer());
//        }
//        if (instrumentDTO.getModelNumber() != null) {
//            existingInstrument.setModelNumber(instrumentDTO.getModelNumber());
//        }
//        if (instrumentDTO.getType() != null) {
//            existingInstrument.setType(instrumentDTO.getType());
//        }
//        existingInstrument.setUpdateDetails(instrumentDTO.getUpdatedBy(), instrumentDTO.getStatus());
//
//        return modelMapper.map(instrumentRepository.save(existingInstrument), InstrumentDTO.class);
//    }
//
//    @Override
//    public void deleteInstrument(Long id, Integer userId) {
//        Instrument existingInstrument =  instrumentRepository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Instrument not found with ID: " + id)
//        );
//        existingInstrument.setUpdateDetails(userId, 0);
//        instrumentRepository.save(existingInstrument);
//    }
//}
