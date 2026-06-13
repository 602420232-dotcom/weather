package com.uav.repository;

import com.uav.model.ForumPostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostTagRepository extends JpaRepository<ForumPostTag, Long> {

    List<ForumPostTag> findByPostId(Long postId);

    void deleteByPostId(Long postId);

    List<ForumPostTag> findByTagName(String tagName);
}