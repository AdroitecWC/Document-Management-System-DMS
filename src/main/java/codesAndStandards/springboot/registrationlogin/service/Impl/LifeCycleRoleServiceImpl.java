package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.LifeCycleRoleDto;
import codesAndStandards.springboot.registrationlogin.entity.LifeCycleRole;
import codesAndStandards.springboot.registrationlogin.repository.LifeCycleRoleRepository;
import codesAndStandards.springboot.registrationlogin.service.LifeCycleRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LifeCycleRoleServiceImpl implements LifeCycleRoleService {

    @Autowired
    private LifeCycleRoleRepository repo;

    private LifeCycleRoleDto toDto(LifeCycleRole e) {
        LifeCycleRoleDto dto = new LifeCycleRoleDto();
        dto.setLcRoleId(e.getLcRoleId());
        dto.setRoleName(e.getRoleName());
        dto.setDescription(e.getDescription());
        dto.setUsedInTypesCount(repo.countUsageInDocTypes(e.getLcRoleId()).intValue());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LifeCycleRoleDto> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LifeCycleRoleDto getById(Long id) {
        return toDto(repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lifecycle role not found: " + id)));
    }

    @Override
    @Transactional
    public LifeCycleRoleDto create(LifeCycleRoleDto dto) {
        if (dto.getRoleName() == null || dto.getRoleName().trim().isEmpty())
            throw new IllegalArgumentException("Role name is required");
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty())
            throw new IllegalArgumentException("Description is required");
        if (repo.existsByRoleName(dto.getRoleName().trim()))
            throw new IllegalArgumentException("A role named '" + dto.getRoleName() + "' already exists");

        LifeCycleRole e = new LifeCycleRole();
        e.setRoleName(dto.getRoleName().trim());
        e.setDescription(dto.getDescription().trim());
        return toDto(repo.save(e));
    }

    @Override
    @Transactional
    public LifeCycleRoleDto update(Long id, LifeCycleRoleDto dto) {
        LifeCycleRole e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lifecycle role not found: " + id));
        if (dto.getRoleName() == null || dto.getRoleName().trim().isEmpty())
            throw new IllegalArgumentException("Role name is required");
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty())
            throw new IllegalArgumentException("Description is required");
        if (repo.existsByRoleNameAndLcRoleIdNot(dto.getRoleName().trim(), id))
            throw new IllegalArgumentException("A role named '" + dto.getRoleName() + "' already exists");

        e.setRoleName(dto.getRoleName().trim());
        e.setDescription(dto.getDescription().trim());
        return toDto(repo.save(e));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LifeCycleRole e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lifecycle role not found: " + id));
        int usage = repo.countUsageInDocTypes(id).intValue();
        if (usage > 0)
            throw new IllegalStateException(
                    "Cannot delete: this role is used in " + usage + " document type(s). Remove it from those types first.");
        repo.delete(e);
    }
}