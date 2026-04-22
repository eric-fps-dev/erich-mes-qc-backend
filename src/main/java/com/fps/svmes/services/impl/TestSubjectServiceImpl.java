//package com.fps.svmes.services.impl;
//
//import com.fps.svmes.dto.dtos.dispatch.TestSubjectDTO;
//import com.fps.svmes.models.sql.taskSchedule.TestSubject;
//import com.fps.svmes.repositories.jpaRepo.dispatch.TestSubjectRepository;
//import com.fps.svmes.services.TestSubjectService;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.time.OffsetDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class TestSubjectServiceImpl implements TestSubjectService {
//
//    @Autowired
//    private TestSubjectRepository repository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    public TestSubjectDTO createTestSubject(TestSubjectDTO testSubjectDTO) {
//        TestSubject testSubject = modelMapper.map(testSubjectDTO, TestSubject.class);
//        TestSubject savedTestSubject = repository.save(testSubject);
//        return modelMapper.map(savedTestSubject, TestSubjectDTO.class);
//    }
//
//    @Override
//    public TestSubjectDTO updateTestSubject(Long id, TestSubjectDTO testSubjectDTO) {
//        TestSubject testSubject = repository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Test Subject not found with ID: " + id)
//        );
//
//        if (testSubjectDTO.getName() != null) {
//            testSubject.setName(testSubjectDTO.getName());
//        }
//
//        if (testSubjectDTO.getDescription() != null) {
//            testSubject.setDescription(testSubjectDTO.getDescription());
//        }
//
//        testSubject.setUpdateDetails(testSubjectDTO.getUpdatedBy(), 1);
//        TestSubject updatedTestSubject = repository.save(testSubject);
//        return modelMapper.map(updatedTestSubject, TestSubjectDTO.class);
//    }
//
//    @Override
//    public TestSubjectDTO getTestSubjectById(Long id) {
//        TestSubject testSubject = repository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Test Subject not found with ID: " + id)
//        );
//        return modelMapper.map(testSubject, TestSubjectDTO.class);
//    }
//
//    @Override
//    public List<TestSubjectDTO> getAllActiveTestSubjects() {
//        List<TestSubject> activeTestSubjects = repository.findByStatus(1);
//        return activeTestSubjects.stream()
//                .map(subject -> modelMapper.map(subject, TestSubjectDTO.class))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public void deleteTestSubject(Long id, Integer userId) {
//        TestSubject testSubject = repository.findByIdAndStatus(id, 1).orElseThrow(
//                () -> new RuntimeException("Test Subject not found with ID: " + id)
//        );
//        testSubject.setUpdateDetails(userId, 0);
//        repository.save(testSubject);
//    }
//}
