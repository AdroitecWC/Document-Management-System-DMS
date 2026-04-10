package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.GroupListDTO;
import codesAndStandards.springboot.registrationlogin.dto.UserDto;
import codesAndStandards.springboot.registrationlogin.entity.Group;
import codesAndStandards.springboot.registrationlogin.entity.GroupUser;
import codesAndStandards.springboot.registrationlogin.entity.Role;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.GroupRepository;
import codesAndStandards.springboot.registrationlogin.repository.GroupUserRepository;
import codesAndStandards.springboot.registrationlogin.repository.RoleRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           GroupUserRepository groupUserRepository,
                           GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.groupUserRepository = groupUserRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    @Transactional
    public void saveUserWithStoredProcedure(UserDto userDto) throws RuntimeException {
        logger.info("Starting saveUserWithStoredProcedure for user: {}", userDto.getUsername());

        try {
            String firstName = userDto.getFirstName();
            String lastName = userDto.getLastName();
            String username = userDto.getUsername();
            String email = userDto.getEmail();
            String encodedPassword = passwordEncoder.encode(userDto.getPassword());
            Integer roleId = userDto.getRoleId();

            Long createdById = null;
            if (userDto.getCreatedByUsername() != null && !userDto.getCreatedByUsername().isEmpty()) {
                User createdByUser = userRepository.findByUsername(userDto.getCreatedByUsername());
                if (createdByUser != null) {
                    createdById = createdByUser.getUserId();
                }
            }

            StoredProcedureQuery sp = entityManager.createStoredProcedureQuery("AddUser");

            sp.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(6, Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(7, Long.class, ParameterMode.IN);

            sp.setParameter(1, firstName);
            sp.setParameter(2, lastName);
            sp.setParameter(3, username);
            sp.setParameter(4, email);
            sp.setParameter(5, encodedPassword);
            sp.setParameter(6, roleId);
            sp.setParameter(7, createdById);

            sp.execute();

            User createdUser = userRepository.findByUsername(username);
            if (createdUser == null) {
                throw new RuntimeException("User created but cannot fetch userId.");
            }
            Long userId = createdUser.getUserId();

            groupUserRepository.deleteByUserId(userId);

            if (userDto.getGroupIds() != null && !userDto.getGroupIds().isEmpty()) {
                for (Long groupId : userDto.getGroupIds()) {
                    groupUserRepository.insertUserGroup(userId, groupId, createdById);
                }
            }

        } catch (Exception e) {
            logger.error("Error in saveUserWithStoredProcedure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create/update user");
        }
    }

    @Override
    @Transactional
    public void editUserByAdminWithStoredProcedure(String username, UserDto userDto) throws RuntimeException {
        logger.info("Starting editUserByAdminWithStoredProcedure for user: {}", username);

        try {
            StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("EditUserByAdmin");

            storedProcedure.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(6, Integer.class, ParameterMode.IN);

            storedProcedure.setParameter(1, username);
            storedProcedure.setParameter(2,
                    (userDto.getFirstName() != null && !userDto.getFirstName().isEmpty()) ? userDto.getFirstName() : null);
            storedProcedure.setParameter(3,
                    (userDto.getLastName() != null && !userDto.getLastName().isEmpty()) ? userDto.getLastName() : null);
            storedProcedure.setParameter(4,
                    (userDto.getEmail() != null && !userDto.getEmail().isEmpty()) ? userDto.getEmail() : null);
            storedProcedure.setParameter(5,
                    (userDto.getPassword() != null && !userDto.getPassword().isEmpty())
                            ? passwordEncoder.encode(userDto.getPassword()) : null);
            storedProcedure.setParameter(6, userDto.getRoleId());

            storedProcedure.execute();
            logger.info("User updated by admin successfully: {}", username);

        } catch (Exception e) {
            logger.error("Error in editUserByAdmin: {}", username, e);
            handleStoredProcedureException(e, "Failed to update user");
        }
    }

    @Override
    @Transactional
    public void editUserProfileWithStoredProcedure(String username, UserDto userDto) throws RuntimeException {
        logger.info("Starting editUserProfileWithStoredProcedure for user: {}", username);

        try {
            StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("EditUserProfile");

            storedProcedure.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(4, String.class, ParameterMode.IN);

            storedProcedure.setParameter(1, username);
            storedProcedure.setParameter(2,
                    (userDto.getFirstName() != null && !userDto.getFirstName().isEmpty()) ? userDto.getFirstName() : null);
            storedProcedure.setParameter(3,
                    (userDto.getLastName() != null && !userDto.getLastName().isEmpty()) ? userDto.getLastName() : null);
            storedProcedure.setParameter(4,
                    (userDto.getPassword() != null && !userDto.getPassword().isEmpty())
                            ? passwordEncoder.encode(userDto.getPassword()) : null);

            storedProcedure.execute();
            logger.info("User profile updated successfully: {}", username);

        } catch (Exception e) {
            logger.error("Error in editUserProfile: {}", username, e);
            handleStoredProcedureException(e, "Failed to update profile");
        }
    }

    @Override
    @Transactional
    public void deleteUserWithStoredProcedure(String username) throws RuntimeException {
        logger.info("Starting deleteUserWithStoredProcedure for user: {}", username);

        try {
            StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("DeleteUser");
            storedProcedure.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            storedProcedure.setParameter(1, username);
            storedProcedure.execute();
            logger.info("User deleted successfully: {}", username);
        } catch (Exception e) {
            logger.error("Error deleting user: {}", username, e);
            handleStoredProcedureException(e, "Failed to delete user");
        }
    }

    private void handleStoredProcedureException(Exception e, String defaultMessage) {
        String errorMessage = e.getMessage();
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        String rootMessage = rootCause.getMessage();

        if ((errorMessage != null && errorMessage.contains("User does not exist")) ||
                (rootMessage != null && rootMessage.contains("User does not exist"))) {
            throw new RuntimeException("User does not exist.");
        } else if ((errorMessage != null && errorMessage.contains("Username already exists")) ||
                (rootMessage != null && rootMessage.contains("Username already exists"))) {
            throw new RuntimeException("Username already exists.");
        } else if ((errorMessage != null && errorMessage.contains("Email already exists")) ||
                (rootMessage != null && rootMessage.contains("Email already exists"))) {
            throw new RuntimeException("Email already exists.");
        } else {
            throw new RuntimeException(defaultMessage + ": " +
                    (rootMessage != null ? rootMessage : errorMessage != null ? errorMessage : "Unknown error"));
        }
    }

    public void saveUserFallback(UserDto userDto) throws RuntimeException {
        User existingUser = userRepository.findByUsername(userDto.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("Username already exists.");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already exists.");
        }

        User user = new User();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        Role role = roleRepository.findById(userDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        if (userDto.getCreatedByUsername() != null && !userDto.getCreatedByUsername().isEmpty()) {
            User createdByUser = userRepository.findByUsername(userDto.getCreatedByUsername());
            if (createdByUser != null) {
                user.setCreatedBy(createdByUser);
            }
        }

        userRepository.save(user);
    }

    @Override
    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        Role role = roleRepository.findById(userDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        if (userDto.getCreatedByUsername() != null && !userDto.getCreatedByUsername().isEmpty()) {
            User createdByUser = userRepository.findByUsername(userDto.getCreatedByUsername());
            if (createdByUser != null) {
                user.setCreatedBy(createdByUser);
            }
        }

        userRepository.save(user);
    }

    @Override
    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public boolean doesUserExist(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    @Override
    public UserDto findUserById(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        UserDto dto = new UserDto();
        dto.setId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getRoleId());
            dto.setRoleName(user.getRole().getRoleName());
        }

        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        if (user.getCreatedBy() != null) {
            dto.setCreatedByUsername(user.getCreatedBy().getUsername());
        } else {
            dto.setCreatedByUsername("System");
        }

        // Load groups via GroupUserRepository (no collection on entity)
        List<GroupUser> groupUsers = groupUserRepository.findByUserId(user.getUserId());
        if (groupUsers != null && !groupUsers.isEmpty()) {
            List<GroupListDTO> groupDtos = groupUsers.stream()
                    .map(GroupUser::getGroup)
                    .filter(g -> g != null)
                    .map(g -> GroupListDTO.builder()
                            .id(g.getGroupId())
                            .groupName(g.getGroupName())
                            .build())
                    .toList();
            dto.setGroups(groupDtos);
        }

        return dto;
    }

    @Override
    public void editUser(UserDto updatedUserDto, Long userId) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        existingUser.setFirstName(updatedUserDto.getFirstName());
        existingUser.setLastName(updatedUserDto.getLastName());
        existingUser.setUsername(updatedUserDto.getUsername());
        existingUser.setEmail(updatedUserDto.getEmail());

        if (updatedUserDto.getPassword() != null &&
                !updatedUserDto.getPassword().isEmpty() &&
                !updatedUserDto.getPassword().equals(existingUser.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDto.getPassword()));
        }

        Role role = roleRepository.findById(updatedUserDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        existingUser.setRole(role);

        userRepository.save(existingUser);
    }

    @Override
    public List<UserDto> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPassword("");
        dto.setRoleId(user.getRole() != null ? user.getRole().getRoleId() : null);
        dto.setRoleName(user.getRole() != null ? user.getRole().getRoleName() : null);

        if (user.getCreatedBy() != null) {
            dto.setCreatedByUsername(user.getCreatedBy().getUsername());
        }

        if (user.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            dto.setCreatedAt(user.getCreatedAt().format(formatter));
        }

        return dto;
    }

    @Override
    public boolean existsByUsername(String username) {
        Optional<User> user = userRepository.findOptionalByUsername(username);
        return user.isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Long getLoggedInUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = findByUsername(username);
        return user.map(User::getUserId).orElse(null);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findOptionalByUsername(username);
    }

    @Transactional
    public void saveUserGroupAssociations(Long userId, List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        User currentUser = getCurrentUser();
        Long createdById = currentUser != null ? currentUser.getUserId() : null;

        for (Long groupId : groupIds) {
            try {
                Group group = groupRepository.findById(groupId)
                        .orElseThrow(() -> new RuntimeException("Group not found with ID: " + groupId));

                if (!groupUserRepository.existsByUserIdAndGroupId(userId, groupId)) {
                    groupUserRepository.insertUserGroup(userId, groupId, createdById);
                }
            } catch (Exception e) {
                logger.error("Failed to assign user {} to group {}: {}", userId, groupId, e.getMessage());
            }
        }
    }

    @Transactional
    public void updateUserGroupAssociations(Long userId, List<Long> groupIds) {
        try {
            groupUserRepository.deleteByUserId(userId);

            if (groupIds != null && !groupIds.isEmpty()) {
                saveUserGroupAssociations(userId, groupIds);
            }
        } catch (Exception e) {
            logger.error("Error updating group associations for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update group associations: " + e.getMessage());
        }
    }

    @Transactional
    public List<GroupListDTO> getUserGroups(Long userId) {
        List<GroupUser> groupUsers = groupUserRepository.findByUserId(userId);

        return groupUsers.stream()
                .map(gu -> {
                    Group group = gu.getGroup();
                    return GroupListDTO.builder()
                            .id(group.getGroupId())
                            .groupName(group.getGroupName())
                            .description(group.getDescription())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("Current user not found");
        }
        return user;
    }
}
