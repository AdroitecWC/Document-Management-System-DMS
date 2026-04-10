package codesAndStandards.springboot.registrationlogin.service;

import codesAndStandards.springboot.registrationlogin.dto.BookmarkDto;
import java.util.List;

public interface BookmarkService {

    BookmarkDto saveBookmark(Long userId, Long documentId, String bookmarkName);

    List<BookmarkDto> getDocumentBookmarks(Long userId, Long documentId);

    List<BookmarkDto> getUserBookmarks(Long userId);

    void deleteBookmark(Long bookmarkId);

    void deleteBookmarkByDocument(Long userId, Long documentId);

    boolean isDocumentBookmarked(Long userId, Long documentId);

    void updateBookmarkName(Long bookmarkId, String newName);
}
