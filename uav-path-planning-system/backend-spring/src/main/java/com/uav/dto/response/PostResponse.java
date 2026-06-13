package com.uav.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {

    private Long id;

    private String title;

    private String content;

    private String section;

    private String status;

    private AuthorResponse author;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Boolean isLiked;

    private Boolean isFavorited;

    private List<String> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    public static class AuthorResponse {
        private Long id;
        private String username;
        private String fullName;
    }
}