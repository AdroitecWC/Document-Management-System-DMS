package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.AccessControlLogicRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAccessService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AccessControlLogicRepository accessControlLogicRepository;

    @Transactional(readOnly = true)
    public List<Document> getAccessibleDocuments() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            log.warn("No authenticated user found");
            return List.of();
        }

        String role = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "Viewer";

        if ("Admin".equals(role) || "Super Admin".equals(role)) {
            return documentRepository.findAll();
        }

        List<Long> accessibleDocIds = accessControlLogicRepository.findAccessibleDocumentIdsByUserId(currentUser.getUserId());

        if (accessibleDocIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllById(accessibleDocIds);
    }

    @Transactional(readOnly = true)
    public List<Document> getAccessibleDocumentsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String role = user.getRole() != null ? user.getRole().getRoleName() : "Viewer";

        if ("Admin".equals(role) || "Super Admin".equals(role)) {
            return documentRepository.findAll();
        }

        List<Long> accessibleDocIds = accessControlLogicRepository.findAccessibleDocumentIdsByUserId(userId);

        if (accessibleDocIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllById(accessibleDocIds);
    }

    @Transactional(readOnly = true)
    public boolean hasAccessToDocument(Long documentId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        String role = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "Viewer";
        if ("Admin".equals(role) || "Super Admin".equals(role)) return true;

        return accessControlLogicRepository.hasUserAccessToDocument(currentUser.getUserId(), documentId);
    }

    @Transactional(readOnly = true)
    public boolean hasAccessToDocument(Long userId, Long documentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String role = user.getRole() != null ? user.getRole().getRoleName() : "Viewer";
        if ("Admin".equals(role) || "Super Admin".equals(role)) return true;

        return accessControlLogicRepository.hasUserAccessToDocument(userId, documentId);
    }

    @Transactional(readOnly = true)
    public List<Long> getAccessibleDocumentIds() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return List.of();

        String role = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "Viewer";

        if ("Admin".equals(role) || "Super Admin".equals(role)) {
            return documentRepository.findAll().stream()
                    .map(Document::getDocumentId)
                    .collect(Collectors.toList());
        }

        return accessControlLogicRepository.findAccessibleDocumentIdsByUserId(currentUser.getUserId());
    }

    @Transactional(readOnly = true)
    public List<Document> filterByUserAccess(List<Document> documents) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return List.of();

        String role = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "Viewer";
        if ("Admin".equals(role) || "Super Admin".equals(role)) return documents;

        List<Long> accessibleDocIds = accessControlLogicRepository.findAccessibleDocumentIdsByUserId(currentUser.getUserId());

        return documents.stream()
                .filter(doc -> accessibleDocIds.contains(doc.getDocumentId()))
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName());
    }
}
