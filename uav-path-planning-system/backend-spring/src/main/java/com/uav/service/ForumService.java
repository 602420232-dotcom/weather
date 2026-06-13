package com.uav.service;

import com.uav.dto.request.CreateCommentRequest;
import com.uav.dto.request.CreatePostRequest;
import com.uav.dto.response.CommentResponse;
import com.uav.dto.response.PageResponse;
import com.uav.dto.response.PostResponse;

import java.util.List;

public interface ForumService {

    PostResponse createPost(CreatePostRequest request, Long userId);

    PostResponse getPostById(Long postId, Long userId);

    PageResponse<PostResponse> getPosts(String section, int page, int size);

    List<PostResponse> getPostsBySection(String section);

    PostResponse updatePost(Long postId, CreatePostRequest request, Long userId);

    void deletePost(Long postId, Long userId);

    CommentResponse createComment(Long postId, CreateCommentRequest request, Long userId);

    List<CommentResponse> getCommentsByPostId(Long postId, Long userId);

    void deleteComment(Long commentId, Long userId);

    PostResponse toggleLike(Long postId, Long userId);

    PostResponse toggleFavorite(Long postId, Long userId);

    PageResponse<PostResponse> getMyPosts(Long userId, int page, int size, String search);

    PageResponse<PostResponse> getMyFavorites(Long userId, int page, int size, String search);
}