package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.MetadataEnumDto;
import codesAndStandards.springboot.registrationlogin.entity.MetadataDefinition;
import codesAndStandards.springboot.registrationlogin.entity.MetadataEnum;
import codesAndStandards.springboot.registrationlogin.repository.MetadataDefinitionRepository;
import codesAndStandards.springboot.registrationlogin.repository.MetadataEnumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataEnumService {

    private final MetadataEnumRepository metadataEnumRepository;
    private final MetadataDefinitionRepository metadataDefinitionRepository;

    /**
     * Get all enum values for a metadata definition
     */
    public List<MetadataEnumDto> getEnumValuesByMetadataId(Long metadataId) {
        return metadataEnumRepository.findByMetadataDefinitionMetadataId(metadataId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a single enum value by ID
     */
    public MetadataEnumDto getById(Long id) {
        MetadataEnum entity = metadataEnumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metadata enum value not found with ID: " + id));
        return toDto(entity);
    }

    /**
     * Add an enum value to a metadata definition
     */
    @Transactional
    public MetadataEnumDto addEnumValue(MetadataEnumDto dto) {
        if (dto.getMetadataId() == null) {
            throw new IllegalArgumentException("Metadata ID is required");
        }
        if (dto.getValue() == null || dto.getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Value is required");
        }

        MetadataDefinition definition = metadataDefinitionRepository.findById(dto.getMetadataId())
                .orElseThrow(() -> new RuntimeException("Metadata definition not found with ID: " + dto.getMetadataId()));

        // Check for duplicate value
        if (metadataEnumRepository.existsByMetadataDefinitionMetadataIdAndValue(dto.getMetadataId(), dto.getValue().trim())) {
            throw new IllegalArgumentException("Value '" + dto.getValue().trim() + "' already exists for this metadata field");
        }

        MetadataEnum entity = MetadataEnum.builder()
                .metadataDefinition(definition)
                .value(dto.getValue().trim())
                .build();

        MetadataEnum saved = metadataEnumRepository.save(entity);
        log.info("Added enum value '{}' to metadata '{}'", saved.getValue(), definition.getFieldName());
        return toDto(saved);
    }

    /**
     * Update an enum value
     */
    @Transactional
    public MetadataEnumDto updateEnumValue(Long id, MetadataEnumDto dto) {
        MetadataEnum entity = metadataEnumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Metadata enum value not found with ID: " + id));

        if (dto.getValue() == null || dto.getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("Value is required");
        }

        // Check for duplicate (excluding self)
        Long metadataId = entity.getMetadataDefinition().getMetadataId();
        if (metadataEnumRepository.existsByMetadataDefinitionMetadataIdAndValue(metadataId, dto.getValue().trim())) {
            MetadataEnum existing = metadataEnumRepository.findByMetadataDefinitionMetadataId(metadataId)
                    .stream()
                    .filter(e -> e.getValue().equals(dto.getValue().trim()) && !e.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                throw new IllegalArgumentException("Value '" + dto.getValue().trim() + "' already exists");
            }
        }

        entity.setValue(dto.getValue().trim());
        MetadataEnum saved = metadataEnumRepository.save(entity);
        log.info("Updated enum value ID {} to '{}'", id, saved.getValue());
        return toDto(saved);
    }

    /**
     * Delete an enum value
     */
    @Transactional
    public void deleteEnumValue(Long id) {
        if (!metadataEnumRepository.existsById(id)) {
            throw new RuntimeException("Metadata enum value not found with ID: " + id);
        }
        metadataEnumRepository.deleteById(id);
        log.info("Deleted enum value ID {}", id);
    }

    /**
     * Delete all enum values for a metadata definition
     */
    @Transactional
    public void deleteAllByMetadataId(Long metadataId) {
        metadataEnumRepository.deleteByMetadataDefinitionMetadataId(metadataId);
        log.info("Deleted all enum values for metadata ID {}", metadataId);
    }

    private MetadataEnumDto toDto(MetadataEnum entity) {
        return MetadataEnumDto.builder()
                .id(entity.getId())
                .metadataId(entity.getMetadataDefinition().getMetadataId())
                .metadataFieldName(entity.getMetadataDefinition().getFieldName())
                .value(entity.getValue())
                .build();
    }
}
