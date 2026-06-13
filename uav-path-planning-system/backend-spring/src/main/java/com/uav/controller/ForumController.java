package com.uav.controller;

import com.uav.dto.request.CreateCommentRequest;
import com.uav.dto.request.CreatePostRequest;
import com.uav.dto.response.CommentResponse;
import com.uav.dto.response.PageResponse;
import com.uav.dto.response.PostResponse;
import com.uav.repository.UserRepository;
import com.uav.service.ForumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.uav.model.User;

@Slf4j
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final UserRepository userRepository;

    @GetMapping("/posts")
    public ResponseEntity<PageResponse<PostResponse>> getPosts(
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("获取帖子列表: section={}, page={}, size={}", section, page, size);
        PageResponse<PostResponse> posts = forumService.getPosts(section, page, size);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/posts/section/{section}")
    public ResponseEntity<List<PostResponse>> getPostsBySection(@PathVariable String section) {
        log.debug("获取板块帖子: section={}", section);
        List<PostResponse> posts = forumService.getPostsBySection(section);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        log.debug("获取帖子详情: postId={}", postId);
        Long userId = getCurrentUserId();
        PostResponse post = forumService.getPostById(postId, userId);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@Valid @RequestBody CreatePostRequest request) {
        log.debug("创建帖子: title={}", request.getTitle());
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        PostResponse post = forumService.createPost(request, userId);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostRequest request) {
        log.debug("更新帖子: postId={}", postId);
        Long userId = getCurrentUserId();
        PostResponse post = forumService.updatePost(postId, request, userId);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        log.debug("删除帖子: postId={}", postId);
        Long userId = getCurrentUserId();
        forumService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        log.debug("获取帖子评论: postId={}", postId);
        Long userId = getCurrentUserId();
        List<CommentResponse> comments = forumService.getCommentsByPostId(postId, userId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        log.debug("创建评论: postId={}", postId);
        Long userId = getCurrentUserId();
        CommentResponse comment = forumService.createComment(postId, request, userId);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        log.debug("删除评论: commentId={}", commentId);
        Long userId = getCurrentUserId();
        forumService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<PostResponse> toggleLike(@PathVariable Long postId) {
        log.debug("点赞/取消点赞: postId={}", postId);
        Long userId = getCurrentUserId();
        PostResponse post = forumService.toggleLike(postId, userId);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/posts/{postId}/favorite")
    public ResponseEntity<PostResponse> toggleFavorite(@PathVariable Long postId) {
        log.debug("收藏/取消收藏: postId={}", postId);
        Long userId = getCurrentUserId();
        PostResponse post = forumService.toggleFavorite(postId, userId);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/posts/my")
    public ResponseEntity<PageResponse<PostResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        log.debug("获取我的帖子: page={}, size={}, search={}", page, size, search);
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            // 未登录时返回空列表
            return ResponseEntity.ok(new PageResponse<>(List.of(), 0L, 0, 0, size));
        }
        PageResponse<PostResponse> posts = forumService.getMyPosts(userId, page, size, search);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/posts/favorites")
    public ResponseEntity<PageResponse<PostResponse>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        log.debug("获取我的收藏: page={}, size={}, search={}", page, size, search);
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            // 未登录时返回空列表
            return ResponseEntity.ok(new PageResponse<>(List.of(), 0L, 0, 0, size));
        }
        PageResponse<PostResponse> favorites = forumService.getMyFavorites(userId, page, size, search);
        return ResponseEntity.ok(favorites);
    }

    private Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        return userId;
    }

    private Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String name = authentication.getName();
            try {
                // 首先尝试直接解析为数字ID
                return Long.parseLong(name);
            } catch (NumberFormatException e) {
                // 如果不是数字，则作为用户名查询数据库
                try {
                    return userRepository.findByUsername(name).map(User::getId).orElse(null);
                } catch (Exception ex) {
                    log.warn("无法获取用户ID: {}", name);
                    return null;
                }
            }
        }
        return null;
    }
}