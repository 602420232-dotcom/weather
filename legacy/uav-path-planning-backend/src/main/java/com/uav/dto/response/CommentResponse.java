package com.uav.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentResponse {

    private Long id;

    private Long postId;

    private Long parentId;

    private String content;

    private PostResponse.AuthorResponse author;

    private LocalDateTime createdAt;
}