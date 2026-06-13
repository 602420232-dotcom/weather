package com.uav.repository;

import com.uav.model.ForumPostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumPostFavoriteRepository extends JpaRepository<ForumPostFavorite, Long> {

    Optional<ForumPostFavorite> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    int countByUserId(Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    List<ForumPostFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ForumPostFavorite> findByUserId(Long userId);
}