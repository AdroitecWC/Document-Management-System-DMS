package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDto;
import codesAndStandards.springboot.registrationlogin.dto.MetadataDefinitionDto;
import codesAndStandards.springboot.registrationlogin.entity.DocumentType;
import codesAndStandards.springboot.registrationlogin.entity.DocumentTypeMetadata;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeMetadataRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTypeRepository;
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
        return mapToDto(saved);
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
