package codesAndStandards.springboot.registrationlogin.service.Impl;

import codesAndStandards.springboot.registrationlogin.dto.BookmarkDto;
import codesAndStandards.springboot.registrationlogin.entity.Bookmark;
import codesAndStandards.springboot.registrationlogin.entity.Document;
import codesAndStandards.springboot.registrationlogin.entity.User;
import codesAndStandards.springboot.registrationlogin.repository.BookmarkRepository;
import codesAndStandards.springboot.registrationlogin.repository.DocumentRepository;
import codesAndStandards.springboot.registrationlogin.repository.UserRepository;
import codesAndStandards.springboot.registrationlogin.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkServiceImpl.class);

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    @Transactional
    public BookmarkDto saveBookmark(Long userId, Long documentId, String bookmarkName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        try {
            bookmarkRepository.addBookmark(userId, documentId, bookmarkName);
            logger.info("Bookmark added via SP for user: {} on document: {}", userId, documentId);

            Bookmark savedBookmark = bookmarkRepository.findByUser_UserIdAndDocument_DocumentId(userId, documentId)
                    .orElseThrow(() -> new RuntimeException("Bookmark not found after creation"));
            return convertToDto(savedBookmark);

        } catch (Exception e) {
            if (e.getMessage() != null &&
                    (e.getMessage().contains("UK_user_document") || e.getMessage().contains("duplicate key"))) {
                logger.warn("Bookmark already exists for user: {} and document: {}", userId, documentId);
                return bookmarkRepository.findByUser_UserIdAndDocument_DocumentId(userId, documentId)
                        .map(this::convertToDto)
                        .orElseThrow(() -> new RuntimeException("Bookmark exists but could not be retrieved."));
            }

            logger.error("SP failed, trying normal save: {}", e.getMessage());
            try {
                Bookmark fallbackBookmark = new Bookmark();
                fallbackBookmark.setUser(user);
                fallbackBookmark.setDocument(document);
                fallbackBookmark.setBookmarkName(bookmarkName);
                fallbackBookmark.setCreatedAt(LocalDateTime.now());
                fallbackBookmark = bookmarkRepository.save(fallbackBookmark);
                return convertToDto(fallbackBookmark);
            } catch (Exception ex) {
                throw new RuntimeException("Error while adding bookmark: " + ex.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkDto> getDocumentBookmarks(Long userId, Long documentId) {
        return bookmarkRepository.findByUser_UserIdAndDocument_DocumentId(userId, documentId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookmarkDto> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBookmark(Long bookmarkId) {
        try {
            bookmarkRepository.deleteBookmarkSP(bookmarkId);
            logger.info("Bookmark deleted via SP, bookmarkId: {}", bookmarkId);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Bookmark not found")) {
                throw new RuntimeException("Bookmark not found with ID: " + bookmarkId);
            }
            bookmarkRepository.deleteById(bookmarkId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDocumentBookmarked(Long userId, Long documentId) {
        return bookmarkRepository.existsByUser_UserIdAndDocument_DocumentId(userId, documentId);
    }

    @Override
    @Transactional
    public void updateBookmarkName(Long bookmarkId, String newName) {
        try {
            bookmarkRepository.updateBookmarkNameSP(bookmarkId, newName);
        } catch (Exception e) {
            logger.error("SP failed for updateBookmarkName: {}", e.getMessage());
            Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                    .orElseThrow(() -> new RuntimeException("Bookmark not found with ID: " + bookmarkId));
            bookmark.setBookmarkName(newName);
            bookmarkRepository.save(bookmark);
        }
    }

    @Override
    @Transactional
    public void deleteBookmarkByDocument(Long userId, Long documentId) {
        Optional<Bookmark> bookmark = bookmarkRepository.findByUser_UserIdAndDocument_DocumentId(userId, documentId);
        if (bookmark.isEmpty()) {
            throw new RuntimeException("Bookmark not found for user " + userId + " and document " + documentId);
        }
        bookmarkRepository.deleteByUserIdAndDocumentId(userId, documentId);
    }

    private BookmarkDto convertToDto(Bookmark bookmark) {
        BookmarkDto dto = new BookmarkDto();
        dto.setId(bookmark.getBookmarkId());
        dto.setUserId(bookmark.getUser().getUserId());
        dto.setDocumentId(bookmark.getDocument().getDocumentId());
        dto.setBookmarkName(bookmark.getBookmarkName());
        dto.setCreatedAt(bookmark.getCreatedAt());
        dto.setUserName(bookmark.getUser().getUsername());
        dto.setDocumentTitle(bookmark.getDocument().getTitle());
        return dto;
    }
}
