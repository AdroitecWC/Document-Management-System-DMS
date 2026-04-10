package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.TagDto;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.Tag;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.exception.ResourceNotFoundException;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentTagRepository;
import codesAndStandards.springboot.registrationlogin.repository.TagRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentTagRepository documentTagRepository;

    @Override
    @Transactional
    public TagDto createTag(TagDto tagDto, Long userId) {
        if (tagRepository.existsByTagName(tagDto.getTagName())) {
            throw new IllegalArgumentException("Tag with name '" + tagDto.getTagName() + "' already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Tag tag = new Tag();
        tag.setTagName(tagDto.getTagName());
        tag.setCreatedBy(user);

        Tag savedTag = tagRepository.save(tag);
        return mapToDto(savedTag);
    }

    public void createTagIfNotExists(String tagName, Long userId) {
        if (!tagRepository.existsByTagName(tagName)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Tag tag = new Tag();
            tag.setTagName(tagName);
            tag.setCreatedBy(user);
            tagRepository.save(tag);
        }
    }

    @Override
    public List<Map<String, Object>> getDocumentsByTagId(Long tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new RuntimeException("Tag not found with id: " + tagId);
        }

        List<Document> documents = documentRepository.findByTagId(tagId);

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
    public TagDto updateTag(Long tagId, TagDto tagDto, Long userId) {
        Tag tag = tagRepository.findByIdWithUsers(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        if (!tag.getTagName().equals(tagDto.getTagName()) &&
                tagRepository.existsByTagName(tagDto.getTagName())) {
            throw new IllegalArgumentException("Tag with name '" + tagDto.getTagName() + "' already exists");
        }

        User updatingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        tag.setTagName(tagDto.getTagName());
        tag.setUpdatedBy(updatingUser);

        Tag updatedTag = tagRepository.save(tag);
        return mapToDto(updatedTag);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId, Long userId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        // Delete all document-tag associations first
        documentTagRepository.deleteByTagId(tagId);

        tagRepository.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public TagDto getTagById(Long tagId) {
        Tag tag = tagRepository.findByIdWithUsers(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));
        return mapToDto(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDto> getAllTags() {
        return tagRepository.findAllWithCreatorAndUpdater().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDto> getTagsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return tagRepository.findByCreatedBy(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDto> getTagsEditedByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return tagRepository.findByUpdatedBy(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TagDto mapToDto(Tag tag) {
        TagDto dto = new TagDto();
        dto.setId(tag.getTagId());
        dto.setTagName(tag.getTagName());

        if (tag.getCreatedBy() != null) {
            dto.setCreatedBy(tag.getCreatedBy().getUserId());
            dto.setCreatedByUsername(tag.getCreatedBy().getUsername());
        } else {
            dto.setCreatedBy(null);
            dto.setCreatedByUsername("Unknown");
        }
        dto.setCreatedAt(tag.getCreatedAt());

        if (tag.getUpdatedBy() != null) {
            dto.setUpdatedBy(tag.getUpdatedBy().getUserId());
            dto.setUpdatedByUsername(tag.getUpdatedBy().getUsername());
        }
        dto.setUpdatedAt(tag.getUpdatedAt());

        // Count via join table
        long docCount = documentTagRepository.countByTagId(tag.getTagId());
        dto.setDocumentCount((int) docCount);

        return dto;
    }

    @Transactional
    public Tag getOrCreateTag(String tagName, Long userId) {
        return tagRepository.findByTagName(tagName)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    Tag newTag = new Tag();
                    newTag.setTagName(tagName);
                    newTag.setCreatedBy(user);
                    return tagRepository.save(newTag);
                });
    }

    @Transactional
    public List<Tag> getOrCreateTags(List<String> tagNames, Long userId) {
        return tagNames.stream()
                .map(tagName -> getOrCreateTag(tagName, userId))
                .collect(Collectors.toList());
    }
}
