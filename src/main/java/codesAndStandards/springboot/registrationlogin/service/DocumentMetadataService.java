package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentMetadataDto;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.DocumentMetadata;
import codesAndStandards.springboot.registrationlogin.entity.MetadataDefinition;
import codesAndStandards.springboot.registrationlogin.repository.DocumentMetadataRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
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
public class DocumentMetadataService {

    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentRepository documentRepository;
    private final MetadataDefinitionRepository metadataDefinitionRepository;

    /**
     * Get all metadata values for a document
     */
    public List<DocumentMetadataDto> getMetadataByDocumentId(Long documentId) {
        return documentMetadataRepository.findByDocumentDocumentId(documentId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a single document metadata entry by ID
     */
    public DocumentMetadataDto getById(Long id) {
        DocumentMetadata entity = documentMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document metadata not found with ID: " + id));
        return toDto(entity);
    }

    /**
     * Save or update a metadata value for a document
     * If the document+metadata combination already exists, update the value
     * Otherwise, create a new entry
     */
    @Transactional
    public DocumentMetadataDto saveOrUpdate(DocumentMetadataDto dto) {
        if (dto.getDocumentId() == null) {
            throw new IllegalArgumentException("Document ID is required");
        }
        if (dto.getMetadataId() == null) {
            throw new IllegalArgumentException("Metadata ID is required");
        }

        Document document = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + dto.getDocumentId()));

        MetadataDefinition definition = metadataDefinitionRepository.findById(dto.getMetadataId())
                .orElseThrow(() -> new RuntimeException("Metadata definition not found with ID: " + dto.getMetadataId()));

        // Check if entry already exists — update if so
        DocumentMetadata entity = documentMetadataRepository
                .findByDocumentDocumentIdAndMetadataDefinitionMetadataId(dto.getDocumentId(), dto.getMetadataId())
                .orElse(null);

        if (entity != null) {
            entity.setValue(dto.getValue());
            log.info("Updated metadata '{}' for document ID {}", definition.getFieldName(), dto.getDocumentId());
        } else {
            entity = DocumentMetadata.builder()
                    .document(document)
                    .metadataDefinition(definition)
                    .value(dto.getValue())
                    .build();
            log.info("Created metadata '{}' for document ID {}", definition.getFieldName(), dto.getDocumentId());
        }

        DocumentMetadata saved = documentMetadataRepository.save(entity);
        return toDto(saved);
    }

    /**
     * Save multiple metadata values for a document at once
     */
    @Transactional
    public List<DocumentMetadataDto> saveAll(Long documentId, List<DocumentMetadataDto> metadataList) {
        return metadataList.stream()
                .peek(dto -> dto.setDocumentId(documentId))
                .map(this::saveOrUpdate)
                .collect(Collectors.toList());
    }

    /**
     * Delete a specific metadata entry
     */
    @Transactional
    public void delete(Long id) {
        if (!documentMetadataRepository.existsById(id)) {
            throw new RuntimeException("Document metadata not found with ID: " + id);
        }
        documentMetadataRepository.deleteById(id);
        log.info("Deleted document metadata ID {}", id);
    }

    /**
     * Delete a specific metadata field value for a document
     */
    @Transactional
    public void deleteByDocumentAndMetadata(Long documentId, Long metadataId) {
        documentMetadataRepository.deleteByDocumentDocumentIdAndMetadataDefinitionMetadataId(documentId, metadataId);
        log.info("Deleted metadata {} for document {}", metadataId, documentId);
    }

    /**
     * Delete all metadata for a document
     */
    @Transactional
    public void deleteAllByDocumentId(Long documentId) {
        documentMetadataRepository.deleteByDocumentDocumentId(documentId);
        log.info("Deleted all metadata for document {}", documentId);
    }

    private DocumentMetadataDto toDto(DocumentMetadata entity) {
        return DocumentMetadataDto.builder()
                .id(entity.getId())
                .documentId(entity.getDocument().getDocumentId())
                .metadataId(entity.getMetadataDefinition().getMetadataId())
                .fieldName(entity.getMetadataDefinition().getFieldName())
                .fieldType(entity.getMetadataDefinition().getFieldType())
                .value(entity.getValue())
                .build();
    }
}
