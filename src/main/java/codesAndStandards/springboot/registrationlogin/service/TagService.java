package codesAndStandards.springboot.registrationlogin.service;

//import codesAndStandards.springboot.registrationlogin.dto.TagDto;
import codesAndStandards.springboot.registrationlogin.dto.TagDto;
import codesAndStandards.springboot.registrationlogin.entity.Tag;

import java.util.List;
import java.util.Map;

public interface TagService {

    TagDto createTag(TagDto tagRequestDto, Long userId);

    TagDto updateTag(Long tagId, TagDto tagRequestDto, Long userId);

    void deleteTag(Long tagId, Long userId);

    TagDto getTagById(Long tagId);

    List<TagDto> getAllTags();

    List<TagDto> getTagsByUser(Long userId);

    List<TagDto> getTagsEditedByUser(Long userId);

    void createTagIfNotExists(String tagName, Long userId);
    Tag getOrCreateTag(String tagName, Long userId);
    List<Tag> getOrCreateTags(List<String> tagNames, Long userId);
    List<Map<String, Object>> getDocumentsByTagId(Long tagId);
}