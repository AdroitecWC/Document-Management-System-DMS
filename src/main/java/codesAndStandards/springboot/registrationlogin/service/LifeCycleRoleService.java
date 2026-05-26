package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.LifeCycleRoleDto;
import java.util.List;

public interface LifeCycleRoleService {
    List<LifeCycleRoleDto> getAll();
    LifeCycleRoleDto getById(Long id);
    LifeCycleRoleDto create(LifeCycleRoleDto dto);
    LifeCycleRoleDto update(Long id, LifeCycleRoleDto dto);
    void delete(Long id);
}