package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.ClassificationDto;
import codesAndStandards.springboot.registrationlogin.entity.Classification;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.exception.ResourceNotFoundException;
import codesAndStandards.springboot.registrationlogin.repository.ClassificationRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentClassificationRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.service.ClassificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassificationServiceImpl implements ClassificationService {

    private final ClassificationRepository classificationRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentClassificationRepository documentClassificationRepository;

    @Override
    @Transactional
    public ClassificationDto createClassification(ClassificationDto classificationDto, Long userId) {
        if (classificationRepository.existsByClassificationName(classificationDto.getClassificationName())) {
            throw new IllegalArgumentException("Classification with name '" + classificationDto.getClassificationName() + "' already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Classification classification = new Classification();
        classification.setClassificationName(classificationDto.getClassificationName());
        classification.setCreatedBy(user);

        Classification savedClassification = classificationRepository.save(classification);
        return mapToDto(savedClassification);
    }

    @Override
    @Transactional
    public ClassificationDto updateClassification(Long classificationId, ClassificationDto classificationDto, Long userId) {
        Classification classification = classificationRepository.findByIdWithUsers(classificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Classification not found with id: " + classificationId));

        if (!classification.getClassificationName().equals(classificationDto.getClassificationName()) &&
                classificationRepository.existsByClassificationName(classificationDto.getClassificationName())) {
            throw new IllegalArgumentException("Classification with name '" + classificationDto.getClassificationName() + "' already exists");
        }

        User updatingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        classification.setClassificationName(classificationDto.getClassificationName());
        classification.setUpdatedBy(updatingUser);

        Classification updatedClassification = classificationRepository.save(classification);
        return mapToDto(updatedClassification);
    }

    @Override
    public List<Map<String, Object>> getDocumentsByClassificationId(Long classificationId) {
        if (!classificationRepository.existsById(classificationId)) {
            throw new RuntimeException("Classification not found with id: " + classificationId);
        }

        List<Document> documents = documentRepository.findByClassificationId(classificationId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : documents) {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", doc.getDocumentId());
            docMap.put("title", doc.getTitle());
            result.add(docMap);
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteClassification(Long classificationId, Long userId) {
        Classification classification = classificationRepository.findById(classificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Classification not found with id: " + classificationId));

        // Delete all document-classification associations first
        documentClassificationRepository.deleteByClassificationId(classificationId);

        classificationRepository.delete(classification);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassificationDto getClassificationById(Long classificationId) {
        Classification classification = classificationRepository.findByIdWithUsers(classificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Classification not found with id: " + classificationId));
        return mapToDto(classification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassificationDto> getAllClassifications() {
        return classificationRepository.findAllWithCreatorAndUpdater().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassificationDto> getClassificationsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return classificationRepository.findByCreatedBy(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassificationDto> getClassificationsEditedByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return classificationRepository.findByUpdatedBy(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ClassificationDto mapToDto(Classification classification) {
        ClassificationDto dto = new ClassificationDto();
        dto.setId(classification.getClassificationId());
        dto.setClassificationName(classification.getClassificationName());

        if (classification.getCreatedBy() != null) {
            dto.setCreatedBy(classification.getCreatedBy().getUserId());
            dto.setCreatedByUsername(classification.getCreatedBy().getUsername());
        } else {
            dto.setCreatedBy(null);
            dto.setCreatedByUsername("Unknown");
        }
        dto.setCreatedAt(classification.getCreatedAt());

        if (classification.getUpdatedBy() != null) {
            dto.setUpdatedBy(classification.getUpdatedBy().getUserId());
            dto.setUpdatedByUsername(classification.getUpdatedBy().getUsername());
        }
        dto.setUpdatedAt(classification.getUpdatedAt());

        // Count via join table
        long docCount = documentClassificationRepository.countByClassificationId(classification.getClassificationId());
        dto.setDocumentCount((int) docCount);

        return dto;
    }
}
