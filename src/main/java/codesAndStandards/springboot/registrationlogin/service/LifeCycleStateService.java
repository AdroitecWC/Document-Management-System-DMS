package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.LifeCycleStateDto;
import java.util.List;

public interface LifeCycleStateService {
    List<LifeCycleStateDto> getAll();
    LifeCycleStateDto getById(Long id);
    LifeCycleStateDto create(LifeCycleStateDto dto);
    LifeCycleStateDto update(Long id, LifeCycleStateDto dto);
    void delete(Long id);
}