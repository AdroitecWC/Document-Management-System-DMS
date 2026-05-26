package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.DocumentTypeDetailDto;
import java.util.List;
import java.util.Map;

public interface DocTypeLifecycleService {
    List<DocumentTypeDetailDto> getAllSummaries();
    DocumentTypeDetailDto       getFullDetail(Long docTypeId);
    DocumentTypeDetailDto       saveFullDetail(Long docTypeId, DocumentTypeDetailDto dto, String username);
    List<Map<String, Object>>   getAvailableStates();
    List<Map<String, Object>>   getAvailableGroups();
    List<Map<String, Object>>   getTeamMembers(Long docTypeId);
    List<Map<String, Object>> getAvailablePeople();
}