<template>
  <el-dialog
    :title="post?.title"
    v-model="dialogVisible"
    width="800px"
    :close-on-click-modal="false"
    class="post-detail-modal"
  >
    <div v-if="post" class="post-detail">
      <div class="post-meta">
        <div class="author-section">
          <img :src="post.author.avatar" :alt="post.author.displayName" class="author-avatar" />
          <div class="author-info">
            <span class="author-name">{{ post.author.displayName }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
            <span v-if="isAdmin && post.authorLocation" class="author-location">
              <el-tag size="small" type="info">📍 {{ post.authorLocation }}</el-tag>
            </span>
          </div>
        </div>
        <div class="post-tags">
          <el-tag
            v-for="tag in post.tags"
            :key="tag"
            size="small"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <div class="post-content" v-html="post.content"></div>

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
          <el-icon><ArrowRight /></el-icon>
          <span>{{ t('forum.share') }}</span>
        </span>
        <span class="action-btn">
          <el-icon><Star /></el-icon>
          <span>{{ isFavorited ? t('forum.favorited') : t('forum.favorite') }}</span>
        </span>
      </div>

      <div class="comments-section">
        <h3 class="comments-title">
          <el-icon><Message /></el-icon>
          {{ t('forum.submitComment') }} ({{ comments.length }})
        </h3>

        <div class="comments-list">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <img :src="comment.author.avatar" :alt="comment.author.displayName" class="comment-avatar" />
            <div class="comment-content">
              <div class="comment-header">
                <span class="comment-author">{{ comment.author.displayName }}</span>
                <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                <span v-if="isAdmin && comment.authorLocation" class="comment-location">
                  <el-tag size="small" type="info">📍 {{ comment.authorLocation }}</el-tag>
                </span>
              </div>
              <p class="comment-text">{{ comment.content }}</p>
            </div>
          </div>
        </div>

        <div v-if="comments.length === 0" class="empty-comments">
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
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { Check, ArrowRight, Star, Message } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import forumApi from '@/api/forum';
import { useAuthStore } from '@/stores/auth';
import { ROLES } from '@/stores/auth';

const { t } = useI18n();

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
});

defineEmits(['close']);

const authStore = useAuthStore();
const comments = ref([]);
const newComment = ref('');
const isLiked = ref(false);
const isFavorited = ref(false);
const loadingComments = ref(false);

// 对话框显示状态
const dialogVisible = ref(true);

// 管理员可见IP（脱敏处理）
const isAdmin = computed(() => authStore.role === ROLES.ADMIN);

const loadComments = async () => {
  loadingComments.value = true;
  try {
    const data = await forumApi.getComments(props.post.id);
    comments.value = data;
  } catch (error) {
    ElMessage.error(t('common.loadFailed') + ': ' + error.message);
  } finally {
    loadingComments.value = false;
  }
};

const handleLike = () => {
  isLiked.value = !isLiked.value;
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
    ElMessage.success(t('common.operationSuccess'));
  } catch (error) {
    ElMessage.error(t('common.operationFailed') + ': ' + error.message);
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
.post-detail-modal {
  :deep(.el-dialog__body) {
    padding: 20px;
    max-height: 70vh;
    overflow-y: auto;
  }
}

.post-detail {
  .post-meta {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid var(--color-border);
  }

  .author-section {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .author-avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    object-fit: cover;
  }

  .author-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .author-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text);
  }

  .post-time {
    font-size: 12px;
    color: var(--color-text-muted);
  }

  .post-tags {
    display: flex;
    gap: 8px;
  }

  .post-content {
    font-size: 15px;
    line-height: 1.8;
    color: var(--color-text);
    margin-bottom: 20px;
  }

  .post-actions {
    display: flex;
    gap: 24px;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid var(--color-border);
  }

  .action-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    color: var(--color-text-muted);
    transition: all 0.2s;
    padding: 8px 12px;
    border-radius: 8px;
  }

  .action-btn:hover {
    background: var(--color-primary-light);
    color: var(--color-primary);
  }

  .action-btn.liked {
    color: #f56c6c;
  }
}

.comments-section {
  .comments-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    margin: 0 0 16px 0;
    color: var(--color-text);
  }

  .comments-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 20px;
  }

  .comment-item {
    display: flex;
    gap: 12px;
  }

  .comment-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
  }

  .comment-content {
    flex: 1;
    background: var(--color-bg-secondary);
    border-radius: 8px;
    padding: 12px;
  }

  .comment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .comment-author {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text);
  }

  .comment-time {
    font-size: 12px;
    color: var(--color-text-muted);
  }

  .comment-location {
    margin-left: auto;
  }

  .author-location {
    margin-left: 8px;
  }

  .comment-text {
    font-size: 14px;
    color: var(--color-text);
    margin: 0;
    line-height: 1.6;
  }

  .empty-comments {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    color: var(--color-text-muted);
  }

  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }

  .comment-input-section {
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border);
    border-radius: 8px;
    padding: 12px;
  }

  .comment-input {
    width: 100%;
    border: none;
    background: transparent;
    resize: none;
    font-size: 14px;
    line-height: 1.5;
    outline: none;
    box-sizing: border-box;
    color: var(--color-text);
  }

  .comment-input::placeholder {
    color: var(--color-text-muted);
  }

  .comment-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--color-border);
  }
}
</style>
