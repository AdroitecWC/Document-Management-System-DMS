package codesAndStandards.springboot.registrationlogin.repository;

import codesAndStandards.springboot.registrationlogin.entity.Bookmark;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * Find specific bookmark by user and document
     */
    Optional<Bookmark> findByUser_UserIdAndDocument_DocumentId(Long userId, Long documentId);

    /**
     * Find all bookmarks for a specific user
     */
    List<Bookmark> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all bookmarks for a specific document
     */
    List<Bookmark> findByDocument_DocumentId(Long documentId);

    /**
     * Check if bookmark exists for user + document
     */
    boolean existsByUser_UserIdAndDocument_DocumentId(Long userId, Long documentId);

    @Procedure(procedureName = "AddBookmark")
    void addBookmark(
            @Param("UserId") Long userId,
            @Param("DocumentId") Long documentId,
            @Param("BookmarkName") String bookmarkName
    );

    @Procedure(procedureName = "DeleteBookmark")
    void deleteBookmarkSP(@Param("BookmarkId") Long bookmarkId);

    @Modifying
    @Query(value = "EXEC updateBookmark :bookmarkId, :bookmarkName", nativeQuery = true)
    void updateBookmarkNameSP(@Param("bookmarkId") Long bookmarkId, @Param("bookmarkName") String bookmarkName);

    @Modifying
    @Transactional
    @Query("DELETE FROM Bookmark b WHERE b.user.id = :userId AND b.document.id = :documentId")
    void deleteByUserIdAndDocumentId(@Param("userId") Long userId, @Param("documentId") Long documentId);
}
