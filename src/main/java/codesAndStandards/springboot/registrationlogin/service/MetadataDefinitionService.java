package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.MetadataDefinitionDto;
import codesAndStandards.springboot.registrationlogin.entity.MetadataDefinition;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeMetadataRepository;
import codesAndStandards.springboot.registrationlogin.repository.MetadataDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataDefinitionService {

    private final MetadataDefinitionRepository metadataDefinitionRepository;
    private final DocumentTypeMetadataRepository documentTypeMetadataRepository;

    @Transactional(readOnly = true)
    public List<MetadataDefinitionDto> getAllMetadataDefinitions() {
        return metadataDefinitionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MetadataDefinitionDto getById(Long id) {
        MetadataDefinition md = metadataDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metadata definition not found with id: " + id));
        return mapToDto(md);
    }

    @Transactional(readOnly = true)
    public List<MetadataDefinitionDto> getByDocumentTypeId(Long docTypeId) {
        return metadataDefinitionRepository.findByDocumentTypeId(docTypeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MetadataDefinitionDto> getMandatoryByDocumentTypeId(Long docTypeId) {
        return metadataDefinitionRepository.findMandatoryByDocumentTypeId(docTypeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MetadataDefinitionDto create(MetadataDefinitionDto dto) {
        if (metadataDefinitionRepository.existsByFieldName(dto.getFieldName())) {
            throw new IllegalArgumentException("Metadata field '" + dto.getFieldName() + "' already exists");
        }

        MetadataDefinition md = MetadataDefinition.builder()
                .fieldName(dto.getFieldName())
                .fieldType(dto.getFieldType())
                .build();

        MetadataDefinition saved = metadataDefinitionRepository.save(md);
        return mapToDto(saved);
    }

    @Transactional
    public MetadataDefinitionDto update(Long id, MetadataDefinitionDto dto) {
        MetadataDefinition md = metadataDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metadata definition not found with id: " + id));

        if (!md.getFieldName().equals(dto.getFieldName()) &&
                metadataDefinitionRepository.existsByFieldName(dto.getFieldName())) {
            throw new IllegalArgumentException("Metadata field '" + dto.getFieldName() + "' already exists");
        }

        md.setFieldName(dto.getFieldName());
        md.setFieldType(dto.getFieldType());

        MetadataDefinition updated = metadataDefinitionRepository.save(md);
        return mapToDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        // Remove all type-metadata associations first
        documentTypeMetadataRepository.deleteByMetadataId(id);
        metadataDefinitionRepository.deleteById(id);
    }

    private MetadataDefinitionDto mapToDto(MetadataDefinition md) {
        return MetadataDefinitionDto.builder()
                .metadataId(md.getMetadataId())
                .fieldName(md.getFieldName())
                .fieldType(md.getFieldType())
                .build();
    }
}
