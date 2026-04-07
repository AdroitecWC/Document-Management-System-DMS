package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.BookmarkDto;

import java.util.List;

public interface BookmarkService {

    /**
     * Save a new bookmark (multiple bookmarks per document allowed)
     */
//    BookmarkDto saveBookmark(Long userId, Integer documentId, Integer pageNumber, String bookmarkName);
    BookmarkDto saveBookmark(Long userId, Integer documentId, String bookmarkName);

    /**
     * Get all bookmarks for a user and document
     */
    List<BookmarkDto> getDocumentBookmarks(Long userId, Integer documentId);

    /**
     * Get all bookmarks for a user (across all documents)
     */
    List<BookmarkDto> getUserBookmarks(Long userId);

    /**
     * Delete a specific bookmark by ID
     */
    void deleteBookmark(Long bookmarkId);
    void deleteBookmarkByDocument(Long userId, Integer documentId);

    /**
     * Check if a specific page is bookmarked
     */
//    boolean isPageBookmarked(Long userId, Integer documentId, Integer pageNumber);
    boolean isDocumentBookmarked(Long userId, Integer documentId);

    void updateBookmarkName(Long bookmarkId, String newName);

}