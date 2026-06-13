package com.uav.repository;

import com.uav.model.ForumPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike, Long> {

    Optional<ForumPostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    int countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    List<ForumPostLike> findByUserId(Long userId);
}