package com.uav.model;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "forum_post_tags")
public class ForumPostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "tag_name", nullable = false, length = 50)
    private String tagName;
}