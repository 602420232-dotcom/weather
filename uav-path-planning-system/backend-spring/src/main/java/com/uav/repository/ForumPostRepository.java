package com.uav.repository;

import com.uav.model.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    Page<ForumPost> findBySectionAndStatus(String section, String status, Pageable pageable);

    Page<ForumPost> findByStatus(String status, Pageable pageable);

    List<ForumPost> findByStatusOrderByCreatedAtDesc(String status);

    List<ForumPost> findBySectionAndStatusOrderByCreatedAtDesc(String section, String status);

    @Modifying
    @Query("UPDATE ForumPost p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    int incrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE ForumPost p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    int decrementLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE ForumPost p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    int incrementCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE ForumPost p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    int decrementCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE ForumPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    Page<ForumPost> findByAuthorIdAndStatus(Long authorId, String status, Pageable pageable);

    Page<ForumPost> findByAuthorIdAndStatusAndTitleContaining(Long authorId, String status, String title, Pageable pageable);

    Page<ForumPost> findByIdInAndStatus(List<Long> ids, String status, Pageable pageable);

    Page<ForumPost> findByIdInAndStatusAndTitleContaining(List<Long> ids, String status, String title, Pageable pageable);
}