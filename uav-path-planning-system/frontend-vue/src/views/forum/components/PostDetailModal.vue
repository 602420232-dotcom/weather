<template>
  <el-dialog
    :title="post?.title"
    v-model="dialogVisible"
    width="720px"
    :close-on-click-modal="true"
    class="post-detail-modal"
    destroy-on-close
    @close="handleClose"
  >
    <div v-if="post" class="post-detail">
      <!-- 作者信息区 -->
      <div class="post-meta">
        <div class="author-section">
          <img :src="post.author.avatar" :alt="post.author.displayName" width="48" height="48" loading="lazy" class="author-avatar" />
          <div class="author-info">
            <div class="author-name-row">
              <span class="author-name">{{ post.author.displayName }}</span>
              <span v-if="isAdmin && post.authorLocation" class="author-location">
                <el-tag size="small" type="info" effect="plain">📍 {{ post.authorLocation }}</el-tag>
              </span>
            </div>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
        <div class="post-tags">
          <el-tag
            v-for="tag in post.tags"
            :key="tag"
            size="small"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 帖子内容 -->
      <div class="post-content" v-html="sanitizedContent"></div>

      <!-- 操作栏 -->
      <div class="post-actions">
        <span
          class="action-btn"
          :class="{ liked: isLiked }"
          @click="handleLike"
        >
          <el-icon><Check /></el-icon>
          <span>{{ post.likeCount + (isLiked ? 1 : 0) }}</span>
        </span>
        <span class="action-btn">
          <el-icon><Star /></el-icon>
          <span>{{ isFavorited ? t('forum.favorited') : t('forum.favorite') }}</span>
        </span>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <div class="comments-header">
          <h3 class="comments-title">
            <el-icon><Message /></el-icon>
            {{ t('forum.submitComment') }}
          </h3>
          <span class="comments-count">{{ post.commentCount || 0 }} 条评论</span>
        </div>

        <div v-if="comments.length > 0" class="comments-list">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <img :src="comment.author.avatar" :alt="comment.author.displayName" width="36" height="36" loading="lazy" class="comment-avatar" />
            <div class="comment-content">
              <div class="comment-header">
                <div class="comment-author-row">
                  <span class="comment-author">{{ comment.author.displayName }}</span>
                  <span v-if="isAdmin && comment.authorLocation" class="comment-location">
                    <el-tag size="small" type="info" effect="plain">📍 {{ comment.authorLocation }}</el-tag>
                  </span>
                </div>
                <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
            </div>
          </div>
        </div>

        <div v-else class="empty-comments">
          <el-icon class="empty-icon"><Message /></el-icon>
          <p>{{ t('forum.noComments') }}</p>
        </div>

        <div class="comment-input-section">
          <textarea
            v-model="newComment"
            :placeholder="t('forum.commentPlaceholder')"
            class="comment-input"
            rows="3"
          ></textarea>
          <div class="comment-actions">
            <el-button
              type="primary"
              size="small"
              @click="submitComment"
              :disabled="!newComment.trim()"
            >
              {{ t('forum.submitComment') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { Check, Star, Message } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import forumApi from '@/api/forum';
import { useAuthStore } from '@/stores/auth';
import { ROLES } from '@/stores/auth';
import { sanitizeHtml } from '@/utils/sanitize';

const { t } = useI18n();

const props = defineProps({
  post: {
    type: Object,
    required: true
  },
  modelValue: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'close', 'update']);

const authStore = useAuthStore();
const comments = ref([]);
const newComment = ref('');
const isLiked = ref(false);
const isFavorited = ref(false);
const loadingComments = ref(false);

// 对话框显示状态 - 与 v-model 同步
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

// 监听 dialog 关闭事件
const handleClose = () => {
  emit('update:modelValue', false);
  emit('close');
};

// 管理员可见IP（脱敏处理）
const isAdmin = computed(() => authStore.role === ROLES.ADMIN);

// 帖子内容安全消毒
const sanitizedContent = computed(() => sanitizeHtml(props.post?.content || ''));

const loadComments = async () => {
  loadingComments.value = true;
  try {
    const data = await forumApi.getComments(props.post.id);
    comments.value = data;
  } catch (error) {
    ElMessage.error((t('common.loadFailed') || '加载失败') + ': ' + error.message);
  } finally {
    loadingComments.value = false;
  }
};

const handleLike = () => {
  isLiked.value = !isLiked.value;
  // 通知父组件更新点赞数
  emit('update', {
    id: props.post.id,
    likeCount: props.post.likeCount + (isLiked.value ? 1 : -1),
    commentCount: props.post.commentCount
  });
};

const submitComment = async () => {
  if (!newComment.value.trim()) return;

  try {
    await forumApi.createComment(
      props.post.id,
      newComment.value.trim(),
      authStore.userId
    );
    newComment.value = '';
    await loadComments();
    ElMessage.success((t('common.operationSuccess') || '操作成功'));
    // 通知父组件更新评论数
    emit('update', {
      id: props.post.id,
      likeCount: props.post.likeCount,
      commentCount: comments.value.length
    });
  } catch (error) {
    ElMessage.error((t('common.operationFailed') || '操作失败') + ': ' + error.message);
  }
};

const formatTime = (dateStr) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now - date;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

onMounted(async () => {
  await loadComments();
});
</script>

<style scoped>
/* ===== 基础样式 ===== */
.post-detail-modal {
  :deep(.el-dialog__body) {
    padding: 0;
    max-height: 80vh;
    overflow: hidden;
  }
}

.post-detail {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed);
  background: var(--el-fill-color-lighter, #f5f7fa);
  flex-shrink: 0;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--el-border-color-light, #ebeef5);
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
}

.post-time {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

.author-location {
  margin-left: 4px;
}

.post-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.post-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--el-text-color-primary, #303133);
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  min-height: 100px;
}

.post-actions {
  display: flex;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed);
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--el-text-color-secondary, #909399);
  transition: all 0.2s;
  padding: 6px 10px;
  border-radius: 6px;
  user-select: none;
  font-size: 14px;
}

.action-btn:hover {
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary, #409eff);
}

.action-btn.liked {
  color: var(--el-color-danger, #f56c6c);
}

.comments-section {
  padding: 16px 20px;
  background: var(--el-fill-color-lighter, #f5f7fa);
  max-height: 300px;
  display: flex;
  flex-direction: column;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.comments-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin: 0;
  color: var(--el-text-color-primary, #303133);
}

.comments-count {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
  padding-right: 4px;
}

.comment-item {
  display: flex;
  gap: 10px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-light, #ebeef5);
}

.comment-content {
  flex: 1;
  background: var(--el-bg-color, #ffffff);
  border-radius: 8px;
  padding: 10px 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-author-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary, #303133);
}

.comment-location {
  margin-left: 4px;
}

.comment-time {
  font-size: 11px;
  color: var(--el-text-color-secondary, #909399);
}

.comment-text {
  font-size: 13px;
  color: var(--el-text-color-primary, #303133);
  margin: 0;
  line-height: 1.5;
  word-break: break-word;
}

.empty-comments {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: var(--el-text-color-secondary, #909399);
  flex-shrink: 0;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.comment-input-section {
  background: var(--el-bg-color, #ffffff);
  border: 1px solid var(--el-border-color-lighter, #e4e7ed);
  border-radius: 8px;
  padding: 10px;
  margin-top: 12px;
  flex-shrink: 0;
}

.comment-input {
  width: 100%;
  border: none;
  background: transparent;
  resize: none;
  font-size: 13px;
  line-height: 1.5;
  outline: none;
  box-sizing: border-box;
  color: var(--el-text-color-primary, #303133);
  min-height: 50px;
}

.comment-input::placeholder {
  color: var(--el-text-color-secondary, #909399);
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter, #e4e7ed);
}

/* ===== 暗色模式全局覆盖 ===== */
/* 使用 :global() 让样式穿透到 el-dialog 内部 */
:deep(.el-dialog) {
  --el-dialog-bg-color: var(--el-bg-color, #ffffff);
  --el-dialog-title-font-size: 18px;
}

:deep(.el-dialog__header) {
  background: var(--el-fill-color-lighter, #f5f7fa) !important;
  border-bottom: 1px solid var(--el-border-color-lighter, #e4e7ed) !important;
  padding: 16px 20px !important;
  margin-right: 0 !important;
}

:deep(.el-dialog__title) {
  color: var(--el-text-color-primary, #303133) !important;
  font-size: 18px !important;
  font-weight: 600 !important;
}

:deep(.el-dialog__body) {
  background: var(--el-bg-color, #ffffff) !important;
  color: var(--el-text-color-primary, #303133) !important;
  padding: 20px !important;
}

:deep(.el-dialog__footer) {
  background: var(--el-fill-color-lighter, #f5f7fa) !important;
  border-top: 1px solid var(--el-border-color-lighter, #e4e7ed) !important;
}

/* 帖子内容 HTML 渲染的暗色模式 */
:deep(.post-content) {
  color: var(--el-text-color-primary, #303133) !important;
}

:deep(.post-content p),
:deep(.post-content li),
:deep(.post-content strong),
:deep(.post-content em),
:deep(.post-content h1),
:deep(.post-content h2),
:deep(.post-content h3),
:deep(.post-content h4),
:deep(.post-content h5),
:deep(.post-content h6) {
  color: var(--el-text-color-primary, #303133) !important;
}

:deep(.post-content a) {
  color: var(--el-color-primary, #409eff) !important;
}

:deep(.post-content code) {
  background: var(--el-fill-color-dark, #f5f7fa);
  color: var(--el-text-color-primary, #303133);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
}

:deep(.post-content pre) {
  background: var(--el-fill-color-dark, #f5f7fa);
  color: var(--el-text-color-primary, #303133);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
}

:deep(.post-content blockquote) {
  border-left: 3px solid var(--el-color-primary, #409eff);
  padding-left: 12px;
  color: var(--el-text-color-secondary, #909399);
  margin: 12px 0;
}

/* 评论输入框暗色模式 */
:deep(.comment-input) {
  background: transparent !important;
  color: var(--el-text-color-primary, #303133) !important;
}

:deep(.comment-input)::placeholder {
  color: var(--el-text-color-secondary, #909399) !important;
}

/* 评论内容暗色模式 */
:deep(.comment-content) {
  background: var(--el-fill-color-light, #f5f7fa) !important;
}

:deep(.comment-text) {
  color: var(--el-text-color-primary, #303133) !important;
}

/* 操作按钮暗色模式 */
:deep(.action-btn) {
  color: var(--el-text-color-secondary, #909399) !important;
}

:deep(.action-btn:hover) {
  background: var(--el-color-primary-light-9, #ecf5ff) !important;
  color: var(--el-color-primary, #409eff) !important;
}

:deep(.action-btn.liked) {
  color: var(--el-color-danger, #f56c6c) !important;
}

/* 元信息暗色模式 */
:deep(.author-name),
:deep(.comment-author) {
  color: var(--el-text-color-primary, #303133) !important;
}

:deep(.post-time),
:deep(.comment-time) {
  color: var(--el-text-color-secondary, #909399) !important;
}
</style>
