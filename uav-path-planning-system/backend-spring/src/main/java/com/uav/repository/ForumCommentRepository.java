package com.uav.repository;

import com.uav.model.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {

    List<ForumComment> findByPostIdOrderByCreatedAtDesc(Long postId);

    List<ForumComment> findByPostIdAndParentIdOrderByCreatedAtAsc(Long postId, Long parentId);

    int countByPostId(Long postId);

    void deleteByPostId(Long postId);

    void deleteByParentId(Long parentId);
}