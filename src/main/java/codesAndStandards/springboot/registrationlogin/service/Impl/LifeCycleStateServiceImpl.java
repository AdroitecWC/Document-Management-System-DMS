package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.LifeCycleStateDto;
import codesAndStandards.springboot.registrationlogin.entity.LifeCycleState;
import codesAndStandards.springboot.registrationlogin.repository.LifeCycleStateRepository;
import codesAndStandards.springboot.registrationlogin.service.LifeCycleStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LifeCycleStateServiceImpl implements LifeCycleStateService {

    @Autowired
    private LifeCycleStateRepository repo;

    private LifeCycleStateDto toDto(LifeCycleState e) {
        LifeCycleStateDto dto = new LifeCycleStateDto();
        dto.setStateId(e.getStateId());
        dto.setStateName(e.getStateName());
        dto.setDescription(e.getDescription());
        dto.setColor(e.getColor() != null ? e.getColor() : "#6b7280");
        dto.setUsedInTypesCount(repo.countUsageInDocTypes(e.getStateId()).intValue());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LifeCycleStateDto> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LifeCycleStateDto getById(Long id) {
        LifeCycleState e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lifecycle state not found: " + id));
        return toDto(e);
    }

    @Override
    @Transactional
    public LifeCycleStateDto create(LifeCycleStateDto dto) {
        if (dto.getStateName() == null || dto.getStateName().trim().isEmpty())
            throw new IllegalArgumentException("State name is required");
        if (repo.existsByStateName(dto.getStateName().trim()))
            throw new IllegalArgumentException("A state named '" + dto.getStateName() + "' already exists");

        LifeCycleState e = new LifeCycleState();
        e.setStateName(dto.getStateName().trim());
        e.setDescription(dto.getDescription());
        e.setColor(dto.getColor() != null ? dto.getColor() : "#6b7280");
        return toDto(repo.save(e));
    }

    @Override
    @Transactional
    public LifeCycleStateDto update(Long id, LifeCycleStateDto dto) {
        LifeCycleState e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lifecycle state not found: " + id));
        if (dto.getStateName() == null || dto.getStateName().trim().isEmpty())
            throw new IllegalArgumentException("State name is required");
        if (repo.existsByStateNameAndStateIdNot(dto.getStateName().trim(), id))
            throw new IllegalArgumentException("A state named '" + dto.getStateName() + "' already exists");

        e.setStateName(dto.getStateName().trim());
        e.setDescription(dto.getDescription());
        if (dto.getColor() != null) e.setColor(dto.getColor());
        return toDto(repo.save(e));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LifeCycleState e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lifecycle state not found: " + id));
        int usage = repo.countUsageInDocTypes(id).intValue();
        if (usage > 0)
            throw new IllegalStateException(
                    "Cannot delete: this state is used in " + usage + " document type(s). Remove it from those types first.");
        repo.delete(e);
    }
}