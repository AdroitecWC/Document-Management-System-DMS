package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.*;
import codesAndStandards.springboot.registrationlogin.entity.*;
import codesAndStandards.springboot.registrationlogin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final AccessControlLogicRepository accessControlLogicRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final LicenseService licenseService;

    @Transactional(readOnly = true)
    public List<GroupListDTO> getAllGroups() {
        List<Group> groups = groupRepository.findAllWithCreator();
        return groups.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupResponseDTO getGroupById(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + id));
        return convertToResponseDTO(group);
    }

    @Transactional
    public GroupResponseDTO createGroup(GroupRequestDTO requestDTO) {
        if (!canCreateGroups()) {
            throw new IllegalStateException("Group creation is not allowed in Essential Edition (ED1).");
        }

        if (requestDTO.getGroupName() == null || requestDTO.getGroupName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }

        String groupName = requestDTO.getGroupName().trim();
        if (groupRepository.existsByGroupNameIgnoreCase(groupName)) {
            throw new IllegalArgumentException("Group with name '" + groupName + "' already exists");
        }

        User currentUser = getCurrentUser();

        Group group = new Group();
        group.setGroupName(groupName);
        group.setDescription(requestDTO.getDescription());
        group.setCreatedBy(currentUser);
        group.setCreatedAt(LocalDateTime.now());

        Group savedGroup = groupRepository.save(group);

        if (requestDTO.getDocumentIds() != null && !requestDTO.getDocumentIds().isEmpty()) {
            addDocumentsToGroup(savedGroup, requestDTO.getDocumentIds(), currentUser);
        }
        if (requestDTO.getUserIds() != null && !requestDTO.getUserIds().isEmpty()) {
            addUsersToGroup(savedGroup, requestDTO.getUserIds(), currentUser);
        }

        return getGroupById(savedGroup.getGroupId());
    }

    @Transactional
    public List<Long> getGroupIdsByDocumentId(Long documentId) {
        try {
            List<AccessControlLogic> aclList = accessControlLogicRepository.findByDocumentId(documentId);
            return aclList.stream()
                    .map(acl -> acl.getGroup().getGroupId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting group IDs for document {}: {}", documentId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public GroupResponseDTO updateGroup(Long id, GroupRequestDTO requestDTO) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + id));

        User currentUser = getCurrentUser();

        if (requestDTO.getGroupName() != null && !requestDTO.getGroupName().trim().isEmpty()) {
            String newGroupName = requestDTO.getGroupName().trim();
            if (!group.getGroupName().equalsIgnoreCase(newGroupName)) {
                if (groupRepository.existsByGroupNameIgnoreCase(newGroupName)) {
                    throw new IllegalArgumentException("Group with name '" + newGroupName + "' already exists");
                }
                group.setGroupName(newGroupName);
            }
        }

        if (requestDTO.getDescription() != null) {
            group.setDescription(requestDTO.getDescription());
        }

        groupRepository.save(group);
        updateGroupDocuments(group, requestDTO.getDocumentIds(), currentUser);
        updateGroupUsers(group, requestDTO.getUserIds(), currentUser);

        return getGroupById(id);
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + id));

        groupUserRepository.deleteByGroupId(id);
        accessControlLogicRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public List<GroupListDTO> getGroupsByDocumentId(Long documentId) {
        List<Group> groups = groupRepository.findGroupsByDocumentId(documentId);
        return groups.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupListDTO> getGroupsByUserId(Long userId) {
        List<Group> groups = groupRepository.findGroupsByUserId(userId);
        return groups.stream().map(this::convertToListDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccessCheckDTO checkUserAccessToDocument(Long userId, Long documentId) {
        boolean hasAccess = accessControlLogicRepository.hasUserAccessToDocument(userId, documentId);

        List<String> groupNames = List.of();
        if (hasAccess) {
            // Get groups the user belongs to, then check which ones contain the document
            List<Group> userGroups = groupRepository.findGroupsByUserId(userId);
            List<Long> docGroupIds = accessControlLogicRepository.findByDocumentId(documentId).stream()
                    .map(acl -> acl.getGroup().getGroupId())
                    .collect(Collectors.toList());

            groupNames = userGroups.stream()
                    .filter(g -> docGroupIds.contains(g.getGroupId()))
                    .map(Group::getGroupName)
                    .collect(Collectors.toList());
        }

        String message = hasAccess
                ? "User has access through groups: " + String.join(", ", groupNames)
                : "User does not have access to this document";

        return AccessCheckDTO.builder()
                .hasAccess(hasAccess)
                .groupNames(groupNames)
                .message(message)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Long> getAccessibleDocumentIds(Long userId) {
        return accessControlLogicRepository.findAccessibleDocumentIdsByUserId(userId);
    }

    @Transactional
    public void addDocumentToGroup(Long groupId, Long documentId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (accessControlLogicRepository.existsByDocumentIdAndGroupId(documentId, groupId)) {
            throw new IllegalArgumentException("Document already in group");
        }

        User currentUser = getCurrentUser();

        AccessControlLogic acl = AccessControlLogic.builder()
                .document(document)
                .group(group)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        accessControlLogicRepository.save(acl);
    }

    @Transactional
    public void removeDocumentFromGroup(Long groupId, Long documentId) {
        accessControlLogicRepository.deleteByDocumentIdAndGroupId(documentId, groupId);
    }

    @Transactional
    public void addUserToGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (groupUserRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new IllegalArgumentException("User already in group");
        }

        User currentUser = getCurrentUser();

        GroupUser groupUser = GroupUser.builder()
                .user(user)
                .group(group)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        groupUserRepository.save(groupUser);
    }

    @Transactional
    public void removeUserFromGroup(Long groupId, Long userId) {
        groupUserRepository.deleteByUserIdAndGroupId(userId, groupId);
    }

    @Transactional
    public void addDocumentsToGroup(Group group, List<Long> documentIds, User currentUser) {
        List<Document> documents = documentRepository.findAllById(documentIds);

        for (Document document : documents) {
            if (!accessControlLogicRepository.existsByDocumentIdAndGroupId(document.getDocumentId(), group.getGroupId())) {
                AccessControlLogic acl = AccessControlLogic.builder()
                        .document(document)
                        .group(group)
                        .createdBy(currentUser)
                        .createdAt(LocalDateTime.now())
                        .build();
                accessControlLogicRepository.save(acl);
            }
        }
    }

    private void addUsersToGroup(Group group, List<Long> userIds, User currentUser) {
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            if (!groupUserRepository.existsByUserIdAndGroupId(user.getUserId(), group.getGroupId())) {
                GroupUser groupUser = GroupUser.builder()
                        .user(user)
                        .group(group)
                        .createdBy(currentUser)
                        .createdAt(LocalDateTime.now())
                        .build();
                groupUserRepository.save(groupUser);
            }
        }
    }

    private void updateGroupDocuments(Group group, List<Long> documentIds, User currentUser) {
        accessControlLogicRepository.deleteByGroupId(group.getGroupId());
        if (documentIds != null && !documentIds.isEmpty()) {
            addDocumentsToGroup(group, documentIds, currentUser);
        }
    }

    private void updateGroupUsers(Group group, List<Long> userIds, User currentUser) {
        groupUserRepository.deleteByGroupId(group.getGroupId());
        if (userIds != null && !userIds.isEmpty()) {
            addUsersToGroup(group, userIds, currentUser);
        }
    }

    private GroupListDTO convertToListDTO(Group group) {
        long documentCount = accessControlLogicRepository.countByGroupId(group.getGroupId());
        long userCount = groupUserRepository.countByGroupId(group.getGroupId());

        return GroupListDTO.builder()
                .id(group.getGroupId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .createdByUsername(group.getCreatedBy() != null ? group.getCreatedBy().getUsername() : "Unknown")
                .createdAt(group.getCreatedAt())
                .documentCount((int) documentCount)
                .userCount((int) userCount)
                .build();
    }

    private GroupResponseDTO convertToResponseDTO(Group group) {
        List<Long> documentIds = accessControlLogicRepository.findDocumentIdsByGroupId(group.getGroupId());
        List<Long> userIds = groupUserRepository.findUserIdsByGroupId(group.getGroupId());

        // Fetch documents info via repo
        List<DocumentInfoDTO> documents = new ArrayList<>();
        if (!documentIds.isEmpty()) {
            List<Document> docs = documentRepository.findAllById(documentIds);
            documents = docs.stream()
                    .map(doc -> DocumentInfoDTO.builder()
                            .id(doc.getDocumentId())
                            .title(doc.getTitle())
                            .docTypeName(doc.getDocumentType() != null ? doc.getDocumentType().getDocTypeName() : null)
                            .fileExtension(doc.getFileExtension())
                            .build())
                    .collect(Collectors.toList());
        }

        // Fetch users info via repo
        List<UserInfoDTO> users = new ArrayList<>();
        if (!userIds.isEmpty()) {
            List<User> userList = userRepository.findAllById(userIds);
            users = userList.stream()
                    .map(u -> UserInfoDTO.builder()
                            .id(u.getUserId())
                            .username(u.getUsername())
                            .email(u.getEmail())
                            .role(u.getRole() != null ? u.getRole().getRoleName() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        return GroupResponseDTO.builder()
                .id(group.getGroupId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .createdById(group.getCreatedBy() != null ? group.getCreatedBy().getUserId() : null)
                .createdByUsername(group.getCreatedBy() != null ? group.getCreatedBy().getUsername() : "Unknown")
                .createdAt(group.getCreatedAt())
                .documentCount(documentIds.size())
                .userCount(userIds.size())
                .documentIds(documentIds)
                .userIds(userIds)
                .documents(documents)
                .users(users)
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return userRepository.findByUsername(authentication.getName());
        }
        throw new RuntimeException("User not authenticated");
    }

    private boolean canCreateGroups() {
        if (!licenseService.isLicenseValid()) return false;
        return "ED2".equalsIgnoreCase(licenseService.getCurrentEdition());
    }

    private boolean canDeleteGroup(Long groupId) {
        if (!licenseService.isLicenseValid()) return false;
        if ("ED2".equalsIgnoreCase(licenseService.getCurrentEdition())) return true;
        if ("ED1".equalsIgnoreCase(licenseService.getCurrentEdition())) {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            return !isDefaultGroup(group.getGroupName());
        }
        return true;
    }

    private boolean isDefaultGroup(String groupName) {
        if (groupName == null) return false;
        String normalized = groupName.trim().toLowerCase();
        return normalized.equals("design") || normalized.equals("manufacturing") ||
                normalized.equals("quality") || normalized.equals("support");
    }
}
