package com.uav.service.impl;

import com.uav.dto.request.CreateCommentRequest;
import com.uav.dto.request.CreatePostRequest;
import com.uav.dto.response.CommentResponse;
import com.uav.dto.response.PageResponse;
import com.uav.dto.response.PostResponse;
import com.uav.model.*;
import com.uav.repository.*;
import com.uav.service.ForumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private final ForumPostRepository forumPostRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final ForumPostLikeRepository forumPostLikeRepository;
    private final ForumPostFavoriteRepository forumPostFavoriteRepository;
    private final ForumPostTagRepository forumPostTagRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        ForumPost post = new ForumPost();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setSection(request.getSection());
        post.setAuthorId(userId);
        post.setStatus("normal");

        ForumPost savedPost = forumPostRepository.save(post);

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<ForumPostTag> tags = request.getTags().stream()
                    .map(tag -> {
                        ForumPostTag postTag = new ForumPostTag();
                        postTag.setPostId(savedPost.getId());
                        postTag.setTagName(tag);
                        return postTag;
                    })
                    .collect(Collectors.toList());
            forumPostTagRepository.saveAll(tags);
        }

        log.info("用户 {} 创建帖子: {}", userId, savedPost.getId());
        return buildPostResponse(savedPost, userId);
    }

    @Override
    @Transactional
    public PostResponse getPostById(Long postId, Long userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        forumPostRepository.incrementViewCount(postId);

        return buildPostResponse(post, userId);
    }

    @Override
    public PageResponse<PostResponse> getPosts(String section, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumPost> postPage;

        if (section != null && !section.isEmpty()) {
            postPage = forumPostRepository.findBySectionAndStatus(section, "normal", pageable);
        } else {
            postPage = forumPostRepository.findByStatus("normal", pageable);
        }

        List<PostResponse> responses = postPage.getContent().stream()
                .map(post -> buildPostResponse(post, null))
                .collect(Collectors.toList());

        return new PageResponse<>(responses, postPage.getTotalElements(), 
                postPage.getTotalPages(), postPage.getNumber(), postPage.getSize());
    }

    @Override
    public List<PostResponse> getPostsBySection(String section) {
        List<ForumPost> posts = forumPostRepository.findBySectionAndStatusOrderByCreatedAtDesc(section, "normal");
        return posts.stream()
                .map(post -> buildPostResponse(post, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, CreatePostRequest request, Long userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此帖子");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setSection(request.getSection());

        forumPostTagRepository.deleteByPostId(postId);
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<ForumPostTag> tags = request.getTags().stream()
                    .map(tag -> {
                        ForumPostTag postTag = new ForumPostTag();
                        postTag.setPostId(postId);
                        postTag.setTagName(tag);
                        return postTag;
                    })
                    .collect(Collectors.toList());
            forumPostTagRepository.saveAll(tags);
        }

        ForumPost updatedPost = forumPostRepository.save(post);
        log.info("用户 {} 更新帖子: {}", userId, postId);
        return buildPostResponse(updatedPost, userId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此帖子");
        }

        post.setStatus("deleted");
        forumPostRepository.save(post);
        log.info("用户 {} 删除帖子: {}", userId, postId);
    }

    @Override
    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, Long userId) {
        forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        ForumComment comment = new ForumComment();
        comment.setPostId(postId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setAuthorId(userId);

        ForumComment savedComment = forumCommentRepository.save(comment);
        forumPostRepository.incrementCommentCount(postId);

        log.info("用户 {} 评论帖子 {}: {}", userId, postId, savedComment.getId());
        return buildCommentResponse(savedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByPostId(Long postId, Long userId) {
        List<ForumComment> comments = forumCommentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return comments.stream()
                .map(this::buildCommentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ForumComment comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("评论不存在"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此评论");
        }

        Long postId = comment.getPostId();
        forumCommentRepository.delete(comment);
        forumPostRepository.decrementCommentCount(postId);

        log.info("用户 {} 删除评论: {}", userId, commentId);
    }

    @Override
    @Transactional
    public PostResponse toggleLike(Long postId, Long userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        Optional<ForumPostLike> existingLike = forumPostLikeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            forumPostLikeRepository.delete(existingLike.get());
            forumPostRepository.decrementLikeCount(postId);
            log.info("用户 {} 取消点赞帖子: {}", userId, postId);
        } else {
            ForumPostLike like = new ForumPostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            forumPostLikeRepository.save(like);
            forumPostRepository.incrementLikeCount(postId);
            log.info("用户 {} 点赞帖子: {}", userId, postId);
        }

        return buildPostResponse(post, userId);
    }

    @Override
    @Transactional
    public PostResponse toggleFavorite(Long postId, Long userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        Optional<ForumPostFavorite> existingFavorite = forumPostFavoriteRepository.findByPostIdAndUserId(postId, userId);

        if (existingFavorite.isPresent()) {
            forumPostFavoriteRepository.delete(existingFavorite.get());
            log.info("用户 {} 取消收藏帖子: {}", userId, postId);
        } else {
            ForumPostFavorite favorite = new ForumPostFavorite();
            favorite.setPostId(postId);
            favorite.setUserId(userId);
            forumPostFavoriteRepository.save(favorite);
            log.info("用户 {} 收藏帖子: {}", userId, postId);
        }

        return buildPostResponse(post, userId);
    }

    @Override
    public PageResponse<PostResponse> getMyPosts(Long userId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ForumPost> postPage;

        if (search != null && !search.isEmpty()) {
            postPage = forumPostRepository.findByAuthorIdAndStatusAndTitleContaining(userId, "normal", search, pageable);
        } else {
            postPage = forumPostRepository.findByAuthorIdAndStatus(userId, "normal", pageable);
        }

        List<PostResponse> responses = postPage.getContent().stream()
                .map(post -> buildPostResponse(post, userId))
                .collect(Collectors.toList());

        return new PageResponse<>(responses, postPage.getTotalElements(),
                postPage.getTotalPages(), postPage.getNumber(), postPage.getSize());
    }

    @Override
    public PageResponse<PostResponse> getMyFavorites(Long userId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Long> favoritePostIds = forumPostFavoriteRepository.findByUserId(userId)
                .stream()
                .map(ForumPostFavorite::getPostId)
                .collect(Collectors.toList());

        Page<ForumPost> postPage;

        if (search != null && !search.isEmpty()) {
            postPage = forumPostRepository.findByIdInAndStatusAndTitleContaining(favoritePostIds, "normal", search, pageable);
        } else {
            postPage = forumPostRepository.findByIdInAndStatus(favoritePostIds, "normal", pageable);
        }

        List<PostResponse> responses = postPage.getContent().stream()
                .map(post -> buildPostResponse(post, userId))
                .collect(Collectors.toList());

        return new PageResponse<>(responses, postPage.getTotalElements(),
                postPage.getTotalPages(), postPage.getNumber(), postPage.getSize());
    }

    private PostResponse buildPostResponse(ForumPost post, Long userId) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setSection(post.getSection());
        response.setStatus(post.getStatus());
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getCommentCount());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());

        userRepository.findById(post.getAuthorId()).ifPresent(user -> {
            PostResponse.AuthorResponse author = new PostResponse.AuthorResponse();
            author.setId(user.getId());
            author.setUsername(user.getUsername());
            author.setFullName(user.getFullName());
            response.setAuthor(author);
        });

        if (userId != null) {
            response.setIsLiked(forumPostLikeRepository.existsByPostIdAndUserId(post.getId(), userId));
            response.setIsFavorited(forumPostFavoriteRepository.existsByPostIdAndUserId(post.getId(), userId));
        }

        List<ForumPostTag> tags = forumPostTagRepository.findByPostId(post.getId());
        response.setTags(tags.stream().map(ForumPostTag::getTagName).collect(Collectors.toList()));

        return response;
    }

    private CommentResponse buildCommentResponse(ForumComment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setParentId(comment.getParentId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());

        userRepository.findById(comment.getAuthorId()).ifPresent(user -> {
            PostResponse.AuthorResponse author = new PostResponse.AuthorResponse();
            author.setId(user.getId());
            author.setUsername(user.getUsername());
            author.setFullName(user.getFullName());
            response.setAuthor(author);
        });

        return response;
    }
}