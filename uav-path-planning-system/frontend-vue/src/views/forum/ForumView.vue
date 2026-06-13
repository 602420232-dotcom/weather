<template>
  <div class="forum-container">
    <div class="forum-header">
      <div class="header-title">
        <el-icon class="title-icon"><Message /></el-icon>
        <h1>{{ pageTitle }}</h1>
      </div>
      <el-button 
        type="primary" 
        size="default"
        :disabled="!canPost"
        @click="showCreatePostModal = true"
        class="create-btn"
      >
        <el-icon><EditPen /></el-icon>
        {{ t('forum.createPost') }}
      </el-button>
    </div>

    <div class="forum-body">
      <div class="sidebar">
        <div class="section-list">
          <div 
            v-for="section in sections" 
            :key="section.key"
            class="section-item"
            :class="{ 
              active: activeSection === section.key,
              feedback: section.key === 'feedback'
            }"
            @click="selectSection(section.key)"
          >
            <el-icon class="section-icon" :component="getSectionIcon(section.key)" />
            <span>{{ getSectionLabel(section.key) }}</span>
            <span class="section-count">{{ getSectionCount(section.key) }}</span>
          </div>
        </div>

        <div class="quick-actions">
          <h3>{{ t('forum.quickActions') }}</h3>
          <div class="action-item" @click="showMyPosts">
            <el-icon><User /></el-icon>
            <span>{{ t('forum.myPosts') }}</span>
          </div>
          <div class="action-item" @click="showMyFavorites">
            <el-icon><Star /></el-icon>
            <span>{{ t('forum.myFavorites') }}</span>
          </div>
        </div>
      </div>

      <div class="main-content" id="main-content">
        <div class="filter-bar">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索帖子..." 
            class="search-input"
            prefix-icon="Search"
            @input="handleSearch"
          />
          <el-select 
            v-model="sortBy" 
            placeholder="排序方式" 
            class="sort-select"
          >
            <el-option label="最新发布" value="latest" />
            <el-option label="最多回复" value="comments" />
            <el-option label="最多浏览" value="views" />
          </el-select>
        </div>

        <div class="post-list">
          <div 
            v-for="post in posts" 
            :key="post.id" 
            class="post-card"
            @click="viewPost(post)"
          >
            <div class="post-header">
              <div class="author-info">
                <img :src="post.author?.avatar || '/default-avatar.png'" :alt="post.author?.displayName || '用户'" width="40" height="40" loading="lazy" class="author-avatar" />
                <span class="author-name">{{ post.author?.displayName || '匿名用户' }}</span>
                <span class="post-time">{{ formatTime(post.createdAt) }}</span>
              </div>
              <div class="post-tags">
                <span 
                  v-if="post.feedbackStatus" 
                  class="status-tag" 
                  :class="getStatusClass(post.feedbackStatus)"
                >
                  {{ getStatusLabel(post.feedbackStatus) }}
                </span>
                <el-tag 
                  v-for="tag in post.tags" 
                  :key="tag" 
                  size="small" 
                  class="post-tag"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
            <div class="post-title">
              <span v-if="post.status === 'pinned'" class="pin-icon">📌</span>
              {{ post.title }}
            </div>
            <div class="post-preview">{{ stripHtml(post.content) }}</div>
            <div class="post-footer">
              <span class="footer-item">
                <el-icon><Search /></el-icon>
                {{ post.viewCount }}
              </span>
              <span class="footer-item">
                <el-icon><Message /></el-icon>
                {{ post.commentCount }}
              </span>
              <span class="footer-item">
                <el-icon><Check /></el-icon>
                {{ post.likeCount }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="total > pageSize" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, jumper"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <PostDetailModal 
      v-model="showPostDetail"
      :post="selectedPost"
      v-if="selectedPost"
      @update="handlePostUpdate"
    />

    <CreatePostModal 
      v-if="showCreatePostModal"
      :section="activeSection"
      @close="showCreatePostModal = false"
      @created="handlePostCreated"
    />
  </div>
</template>

<script setup>import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { EditPen, Search, User, Star, Check, Message, Bell, ChatDotRound, Document, QuestionFilled } from '@element-plus/icons-vue';
import forumApi, { SECTIONS } from '@/api/forum';
import PostDetailModal from './components/PostDetailModal.vue';
import CreatePostModal from './components/CreatePostModal.vue';
import { ElMessage } from 'element-plus';

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const sections = ref([]);
const posts = ref([]);
const activeSection = ref('');
const searchKeyword = ref('');
const sortBy = ref('latest');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const showPostDetail = ref(false);
const showCreatePostModal = ref(false);
const selectedPost = ref(null);
const sectionStats = ref({});
const isPersonalView = ref(false);
const personalViewType = ref('');

const iconComponents = {
  Bell,
  ChatDotRound,
  User,
  Document,
  QuestionFilled,
  Message
};

const canPost = computed(() => {
  if (!authStore.user) return false;
  if (activeSection.value === SECTIONS.ANNOUNCEMENT) {
    return authStore.hasAction('forum:post:announcement');
  }
  return authStore.hasAction('forum:post');
});

const getSectionIcon = (section) => {
  const iconMap = {
    [SECTIONS.ANNOUNCEMENT]: 'Bell',
    [SECTIONS.TECH_DISCUSS]: 'ChatDotRound',
    [SECTIONS.TASK_COLLAB]: 'User',
    [SECTIONS.KNOWLEDGE]: 'Document',
    [SECTIONS.FEEDBACK]: 'QuestionFilled'
  };
  const iconName = iconMap[section];
  return iconComponents[iconName] || iconComponents.Message;
};

const getSectionLabel = (section) => {
  const labels = {
    [SECTIONS.ANNOUNCEMENT]: t('forum.section.announcement'),
    [SECTIONS.TECH_DISCUSS]: t('forum.section.tech_discuss'),
    [SECTIONS.TASK_COLLAB]: t('forum.section.task_collab'),
    [SECTIONS.KNOWLEDGE]: t('forum.section.knowledge'),
    [SECTIONS.FEEDBACK]: t('forum.section.feedback')
  };
  if (!section) {
    return t('forum.section.unknown');
  }
  return labels[section] || t('forum.section.unknown');
};

const pageTitle = computed(() => {
  if (isPersonalView.value) {
    return personalViewType.value === 'posts' ? t('forum.myPosts') : t('forum.myFavorites');
  }
  return t('forum.title');
});

const getSectionCount = (section) => {
  if (sectionStats.value && sectionStats.value[section] !== undefined) {
    return sectionStats.value[section];
  }
  const count = posts.value.filter(p => p.section === section).length;
  return count;
};

const selectSection = (section) => {
  isPersonalView.value = false;
  personalViewType.value = '';
  activeSection.value = section;
  currentPage.value = 1;
  searchKeyword.value = '';
  loadPosts();
};

const handleSearch = () => {
  currentPage.value = 1;
  loadPosts();
};

const handlePageChange = (page) => {
  currentPage.value = page;
  loadPosts();
};

const loadPosts = async () => {
  const params = {
    search: searchKeyword.value || undefined,
    page: currentPage.value,
    pageSize: pageSize.value,
    sortBy: sortBy.value
  };

  let result;
  if (isPersonalView.value) {
    if (personalViewType.value === 'posts') {
      result = await forumApi.getMyPosts(params);
    } else {
      result = await forumApi.getMyFavorites(params);
    }
  } else {
    params.section = activeSection.value || undefined;
    result = await forumApi.getPosts(params);
  }

  posts.value = result.list;
  total.value = result.total;
};

const viewPost = async (post) => {
  selectedPost.value = post;
  showPostDetail.value = true;
};

const loadPostById = async (postId) => {
  try {
    const post = await forumApi.getPost(postId);
    selectedPost.value = post;
    showPostDetail.value = true;
    if (post.section) {
      activeSection.value = post.section;
    }
  } catch (error) {
    ElMessage.warning(t('forum.postNotFound'));
    console.warn('[ForumView] Failed to load post:', error.message);
  }
};

const handlePostUpdate = ({ id, likeCount, commentCount }) => {
  const postIndex = posts.value.findIndex(p => p.id === id);
  if (postIndex !== -1) {
    posts.value[postIndex].likeCount = likeCount;
    posts.value[postIndex].commentCount = commentCount;
  }
  if (selectedPost.value && selectedPost.value.id === id) {
    selectedPost.value.likeCount = likeCount;
    selectedPost.value.commentCount = commentCount;
  }
};

const handlePostCreated = () => {
  showCreatePostModal.value = false;
  loadPosts();
  loadSectionStats();
};

const loadSectionStats = async () => {
  try {
    const stats = await forumApi.getSectionStats();
    sectionStats.value = stats;
  } catch (error) {
    console.error('加载板块统计失败:', error);
  }
};

const showMyPosts = async () => {
  isPersonalView.value = true;
  personalViewType.value = 'posts';
  currentPage.value = 1;
  searchKeyword.value = '';
  await loadPosts();
};

const showMyFavorites = async () => {
  isPersonalView.value = true;
  personalViewType.value = 'favorites';
  currentPage.value = 1;
  searchKeyword.value = '';
  await loadPosts();
};

const formatTime = (dateStr) => {
  if (!dateStr) {
    return t('forum.unknownTime');
  }
  
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) {
      return t('forum.unknownTime');
    }
    
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 1) return t('forum.time.justNow');
    if (minutes < 60) return t('forum.time.minutesAgo', { count: minutes });
    if (hours < 24) return t('forum.time.hoursAgo', { count: hours });
    if (days < 7) return t('forum.time.daysAgo', { count: days });
    
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  } catch (error) {
    console.error('[ForumView] formatTime error:', error);
    return t('forum.unknownTime');
  }
};

const stripHtml = (html) => {
  if (!html) return '';
  const tmp = document.createElement('DIV');
  tmp.textContent = html;
  const text = tmp.innerHTML || '';
  return text.length > 100 ? text.substring(0, 100) + '...' : text;
};

const getStatusLabel = (status) => {
  const labels = {
    pending: t('forum.status.pending'),
    processing: t('forum.status.processing'),
    solved: t('forum.status.solved')
  };
  return labels[status] || status;
};

const getStatusClass = (status) => {
  const classes = {
    pending: 'status-pending',
    processing: 'status-processing',
    solved: 'status-solved'
  };
  return classes[status] || 'status-pending';
};

onMounted(async () => {
  const sectionsData = await forumApi.getSections();
  sections.value = sectionsData;
  if (sectionsData.length > 0) {
    activeSection.value = sectionsData[0].key;
  }
  await Promise.all([
    loadPosts(),
    loadSectionStats()
  ]);
  
  const postId = route.query.post;
  if (postId) {
    loadPostById(postId);
  }
});
</script>

<style scoped>
.forum-container {
  min-height: 100%;
  background: var(--color-bg);
  padding: 20px;
}

.forum-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  font-size: 28px;
  color: #409eff;
}

.header-title h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.forum-body {
  display: flex;
  gap: 20px;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
}

.section-list {
  background: var(--color-surface);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 16px;
}

.section-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.section-item:hover {
  background: var(--color-bg);
}

.section-item.active {
  background: var(--color-hover);
  color: #409eff;
}

.section-item.active.feedback {
  background: #fef3c7;
  color: #d97706;
}

.section-item.active.feedback .section-count {
  background: #f59e0b;
  color: #fff;
}

.section-icon {
  font-size: 18px;
}

.section-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg);
  padding: 2px 8px;
  border-radius: 10px;
}

.section-item.active .section-count {
  background: #409eff;
  color: #fff;
}

.quick-actions {
  background: var(--color-surface);
  border-radius: 10px;
  padding: 16px;
}

.quick-actions h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: var(--color-text-muted);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-item:hover {
  background: var(--color-bg);
}

.main-content {
  flex: 1;
  min-width: 0;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.search-input {
  width: 300px;
}

.sort-select {
  width: 150px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: var(--color-surface);
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--color-border);
}

.post-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
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

.post-tag {
  background: var(--color-hover);
  color: #409eff;
  border: none;
}

.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.status-pending {
  background: #fef08a;
  color: #854d0e;
}

.status-processing {
  background: #bfdbfe;
  color: #1e40af;
}

.status-solved {
  background: #bbf7d0;
  color: #166534;
}

.post-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 10px;
  line-height: 1.5;
}

.pin-icon {
  margin-right: 8px;
}

.post-preview {
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.6;
  margin-bottom: 12px;
}

.post-footer {
  display: flex;
  gap: 20px;
  color: var(--color-text-muted);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .forum-body {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .section-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .section-item {
    flex: 1;
    min-width: calc(50% - 4px);
    justify-content: center;
  }

  .filter-bar {
    flex-direction: column;
  }

  .search-input, .sort-select {
    width: 100%;
  }
}

/* ===== 暗色模式适配 ===== */
.is-dark .forum-container {
  background: var(--color-bg);
}

.is-dark .header-title h1 {
  color: var(--color-text);
}

.is-dark .section-list {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.is-dark .section-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.is-dark .section-item.active {
  background: rgba(96, 165, 250, 0.15);
  color: var(--color-primary);
}

.is-dark .section-count {
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text-muted);
}

.is-dark .section-item.active .section-count {
  background: var(--color-primary);
  color: #fff;
}

.is-dark .quick-actions {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.is-dark .quick-actions h3 {
  color: var(--color-text-muted);
}

.is-dark .action-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.is-dark .post-card {
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-dark .post-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  border-color: var(--color-primary);
}

.is-dark .author-name {
  color: var(--color-text);
}

.is-dark .post-time {
  color: var(--color-text-muted);
}

.is-dark .post-tag {
  background: rgba(96, 165, 250, 0.15);
  color: var(--color-primary);
}

.is-dark .post-title {
  color: var(--color-text);
}

.is-dark .post-preview {
  color: var(--color-text-muted);
}

.is-dark .post-footer {
  color: var(--color-text-muted);
}

.is-dark .status-pending {
  background: rgba(251, 240, 138, 0.2);
  color: #fde047;
}

.is-dark .status-processing {
  background: rgba(191, 219, 254, 0.2);
  color: #60a5fa;
}

.is-dark .status-solved {
  background: rgba(187, 247, 208, 0.2);
  color: #4ade80;
}

.is-dark .section-item.active.feedback {
  background: rgba(251, 243, 199, 0.15);
  color: #fbbf24;
}

/* 补充深色模式样式 */
.is-dark .title-icon {
  color: var(--color-primary);
}

.is-dark .create-btn {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.is-dark .filter-bar .search-input .el-input__wrapper,
.is-dark .filter-bar .sort-select .el-input__wrapper {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

.is-dark .filter-bar .search-input .el-input__inner,
.is-dark .filter-bar .sort-select .el-input__inner {
  color: var(--color-text);
}

.is-dark .action-item {
  color: var(--color-text);
}

.is-dark .author-info {
  color: var(--color-text-muted);
}

.is-dark .pagination {
  color: var(--color-text-muted);
}

.is-dark .pagination .el-pager li {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text);
}

.is-dark .pagination .el-pager li:hover {
  color: var(--color-primary);
}

.is-dark .pagination .el-pager li.is-active {
  background: var(--color-primary);
  color: #fff;
}

.is-dark .pagination .btn-prev,
.is-dark .pagination .btn-next {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text);
}

.is-dark .pagination .btn-prev:hover,
.is-dark .pagination .btn-next:hover {
  color: var(--color-primary);
}
</style>