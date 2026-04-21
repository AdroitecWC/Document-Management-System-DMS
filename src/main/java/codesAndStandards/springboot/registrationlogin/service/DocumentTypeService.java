package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDto;
import codesAndStandards.springboot.registrationlogin.entity.MetadataDefinition;
import codesAndStandards.springboot.registrationlogin.dto.MetadataDefinitionDto;
import codesAndStandards.springboot.registrationlogin.entity.DocumentType;
import codesAndStandards.springboot.registrationlogin.entity.DocumentTypeMetadata;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeMetadataRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeRepository;
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
public class DocumentTypeService {

    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentTypeMetadataRepository documentTypeMetadataRepository;
    private final DocumentRepository documentRepository;
    private final MetadataDefinitionRepository metadataDefinitionRepository;

    @Transactional(readOnly = true)
    public List<DocumentTypeDto> getAllDocumentTypes() {
        return documentTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentTypeDto getDocumentTypeById(Long id) {
        DocumentType docType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document type not found with id: " + id));
        return mapToDto(docType);
    }

    @Transactional
    public DocumentTypeDto createDocumentType(DocumentTypeDto dto) {
        if (documentTypeRepository.existsByDocTypeName(dto.getDocTypeName())) {
            throw new IllegalArgumentException("Document type '" + dto.getDocTypeName() + "' already exists");
        }

        DocumentType docType = DocumentType.builder()
                .docTypeName(dto.getDocTypeName())
                .description(dto.getDescription())
                .build();

        DocumentType saved = documentTypeRepository.save(docType);

        // Save metadata field associations
        saveMetadataAssociations(saved, dto.getMetadataFieldIds(), dto.getMandatoryFieldIds());

        return mapToDto(saved);
    }
    /**
     * Save metadata field associations for a document type.
     * Inserts rows into DocumentTypeMetadata join table with mandatory flag.
     */
    private void saveMetadataAssociations(DocumentType docType, List<Long> fieldIds, List<Long> mandatoryIds) {
        if (fieldIds == null || fieldIds.isEmpty()) return;

        List<Long> mandatorySet = mandatoryIds != null ? mandatoryIds : List.of();

        for (Long metadataId : fieldIds) {
            MetadataDefinition metaDef = metadataDefinitionRepository.findById(metadataId)
                    .orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + metadataId));

            DocumentTypeMetadata.DocumentTypeMetadataId pk =
                    new DocumentTypeMetadata.DocumentTypeMetadataId(docType.getDocTypeId(), metadataId);

            DocumentTypeMetadata link = DocumentTypeMetadata.builder()
                    .id(pk)
                    .documentType(docType)
                    .metadataDefinition(metaDef)
                    .mandatory(mandatorySet.contains(metadataId))
                    .build();

            documentTypeMetadataRepository.save(link);
        }
    }

    @Transactional
    public DocumentTypeDto updateDocumentType(Long id, DocumentTypeDto dto) {
        DocumentType docType = documentTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document type not found with id: " + id));

        if (!docType.getDocTypeName().equals(dto.getDocTypeName()) &&
                documentTypeRepository.existsByDocTypeName(dto.getDocTypeName())) {
            throw new IllegalArgumentException("Document type '" + dto.getDocTypeName() + "' already exists");
        }

        docType.setDocTypeName(dto.getDocTypeName());
        docType.setDescription(dto.getDescription());

        DocumentType updated = documentTypeRepository.save(docType);

        // Delete existing associations and save new ones
        documentTypeMetadataRepository.deleteByDocTypeId(id);
        saveMetadataAssociations(updated, dto.getMetadataFieldIds(), dto.getMandatoryFieldIds());

        return mapToDto(updated);
    }

    @Transactional
    public void deleteDocumentType(Long id) {
        long docCount = documentRepository.countByDocumentType(id);
        if (docCount > 0) {
            throw new IllegalStateException("Cannot delete document type — it is used by " + docCount + " document(s)");
        }

        documentTypeMetadataRepository.deleteByDocTypeId(id);
        documentTypeRepository.deleteById(id);
    }

    private DocumentTypeDto mapToDto(DocumentType docType) {
        long docCount = documentRepository.countByDocumentType(docType.getDocTypeId());

        List<DocumentTypeMetadata> metadataMappings = documentTypeMetadataRepository.findByDocTypeId(docType.getDocTypeId());

        List<MetadataDefinitionDto> metaDtos = metadataMappings.stream()
                .map(dtm -> MetadataDefinitionDto.builder()
                        .metadataId(dtm.getMetadataDefinition().getMetadataId())
                        .fieldName(dtm.getMetadataDefinition().getFieldName())
                        .fieldType(dtm.getMetadataDefinition().getFieldType())
                        .mandatory(dtm.getMandatory())
                        .build())
                .collect(Collectors.toList());

        return DocumentTypeDto.builder()
                .docTypeId(docType.getDocTypeId())
                .docTypeName(docType.getDocTypeName())
                .description(docType.getDescription())
                .documentCount((int) docCount)
                .metadataDefinitions(metaDtos)
                .build();
    }
}
