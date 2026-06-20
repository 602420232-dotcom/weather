import axios from 'axios'
import { ROLES } from '@/stores/auth'

const SECTIONS = {
  ANNOUNCEMENT: 'announcement',
  TECH_DISCUSS: 'tech_discuss',
  TASK_COLLAB: 'task_collab',
  KNOWLEDGE: 'knowledge',
  FEEDBACK: 'feedback'
}

const SECTION_LABELS = {
  [SECTIONS.ANNOUNCEMENT]: '公告通知',
  [SECTIONS.TECH_DISCUSS]: '技术讨论',
  [SECTIONS.TASK_COLLAB]: '任务协作',
  [SECTIONS.KNOWLEDGE]: '知识库',
  [SECTIONS.FEEDBACK]: '意见反馈'
}

const MOCK_USERS = [
  { id: '1', username: 'admin01', displayName: '系统管理员', role: ROLES.ADMIN, avatar: 'https://api.dicebear.com/7.x/avataaars/png?seed=admin01' },
  { id: '2', username: 'dev01', displayName: '张三', role: ROLES.DEVELOPER, avatar: 'https://api.dicebear.com/7.x/avataaars/png?seed=dev01' },
  { id: '3', username: 'test01', displayName: '李四', role: ROLES.TESTER, avatar: 'https://api.dicebear.com/7.x/avataaars/png?seed=test01' },
  { id: '4', username: 'deploy01', displayName: '王五', role: ROLES.DEPLOYMENT, avatar: 'https://api.dicebear.com/7.x/avataaars/png?seed=deploy01' },
  { id: '5', username: 'flight01', displayName: '赵六', role: ROLES.FLIGHT, avatar: 'https://api.dicebear.com/7.x/avataaars/png?seed=flight01' },
  { id: '6', username: 'prod01', displayName: '孙七', role: ROLES.PRODUCTION, avatar: 'https://api.dicebear.com/7.x/avataaars/png?seed=prod01' }
]

const transformPost = (post) => ({
  ...post,
  id: String(post.id),
  authorId: String(post.author?.id || ''),
  author: post.author ? {
    id: String(post.author.id),
    username: post.author.username,
    displayName: post.author.fullName,
    avatar: `https://api.dicebear.com/7.x/avataaars/png?seed=${post.author.username}`,
    role: ROLES.DEVELOPER
  } : null,
  createdAt: post.createdAt,
  updatedAt: post.updatedAt
})

const transformComment = (comment) => ({
  ...comment,
  id: String(comment.id),
  postId: String(comment.postId),
  parentId: comment.parentId ? String(comment.parentId) : null,
  authorId: String(comment.author?.id || ''),
  author: comment.author ? {
    id: String(comment.author.id),
    username: comment.author.username,
    displayName: comment.author.fullName,
    avatar: `https://api.dicebear.com/7.x/avataaars/png?seed=${comment.author.username}`,
    role: ROLES.DEVELOPER
  } : null,
  createdAt: comment.createdAt
})

const forumApi = {
  SECTIONS,
  SECTION_LABELS,

  async getSectionStats() {
    try {
      const stats = {}
      for (const section of Object.values(SECTIONS)) {
        const response = await axios.get(`/api/forum/posts/section/${section}`)
        stats[section] = response.data.length
      }
      return stats
    } catch (error) {
      console.error('获取板块统计失败:', error)
      const mockStats = {}
      Object.values(SECTIONS).forEach(section => {
        mockStats[section] = Math.floor(Math.random() * 10) + 5
      })
      return mockStats
    }
  },

  getSections() {
    return Promise.resolve(Object.entries(SECTION_LABELS).map(([key, label]) => ({
      key,
      label
    })))
  },

  async getPosts(params = {}) {
    try {
      const { section, search } = params
      const page = parseInt(params.page) || 0
      const pageSize = parseInt(params.pageSize) || 10

      const paramsObj = { page, size: pageSize }
      if (section) paramsObj.section = section

      const response = await axios.get('/api/forum/posts', { params: paramsObj })
      const { content, totalElements, currentPage, pageSize: respPageSize } = response.data

      let posts = content.map(transformPost)

      if (search) {
        const searchLower = search.toLowerCase()
        posts = posts.filter(p => 
          p.title.toLowerCase().includes(searchLower) ||
          p.content.toLowerCase().includes(searchLower)
        )
      }

      return {
        list: posts,
        total: totalElements,
        page: currentPage + 1,
        pageSize: respPageSize
      }
    } catch (error) {
      console.error('获取帖子列表失败:', error)
      return getMockPosts(params)
    }
  },

  async getPost(id) {
    try {
      const response = await axios.get(`/api/forum/posts/${id}`)
      return transformPost(response.data)
    } catch (error) {
      console.error('获取帖子详情失败:', error)
      throw error
    }
  },

  async createPost(data) {
    try {
      const requestData = {
        title: data.title,
        content: data.content,
        section: data.section,
        tags: data.tags || []
      }
      const response = await axios.post('/api/forum/posts', requestData)
      return transformPost(response.data)
    } catch (error) {
      console.error('创建帖子失败:', error)
      throw error
    }
  },

  async updatePost(id, data) {
    try {
      const requestData = {
        title: data.title,
        content: data.content,
        section: data.section,
        tags: data.tags || []
      }
      const response = await axios.put(`/api/forum/posts/${id}`, requestData)
      return transformPost(response.data)
    } catch (error) {
      console.error('更新帖子失败:', error)
      throw error
    }
  },

  async deletePost(id) {
    try {
      await axios.delete(`/api/forum/posts/${id}`)
      return { success: true }
    } catch (error) {
      console.error('删除帖子失败:', error)
      throw error
    }
  },

  async getComments(postId) {
    try {
      const response = await axios.get(`/api/forum/posts/${postId}/comments`)
      return response.data.map(transformComment)
    } catch (error) {
      console.error('获取评论失败:', error)
      return []
    }
  },

  async createComment(postId, content, authorId, authorLocation = null) {
    try {
      const requestData = {
        content,
        parentId: null
      }
      const response = await axios.post(`/api/forum/posts/${postId}/comments`, requestData)
      return transformComment(response.data)
    } catch (error) {
      console.error('创建评论失败:', error)
      throw error
    }
  },

  async deleteComment(id) {
    try {
      await axios.delete(`/api/forum/comments/${id}`)
      return { success: true }
    } catch (error) {
      console.error('删除评论失败:', error)
      throw error
    }
  },

  async toggleLike(postId) {
    try {
      const response = await axios.post(`/api/forum/posts/${postId}/like`)
      return transformPost(response.data)
    } catch (error) {
      console.error('点赞失败:', error)
      throw error
    }
  },

  async toggleFavorite(postId) {
    try {
      const response = await axios.post(`/api/forum/posts/${postId}/favorite`)
      return transformPost(response.data)
    } catch (error) {
      console.error('收藏失败:', error)
      throw error
    }
  },

  async getMyPosts(params = {}) {
    try {
      const { search } = params
      const page = parseInt(params.page) || 0
      const pageSize = parseInt(params.pageSize) || 10

      const paramsObj = { page, size: pageSize }
      if (search) paramsObj.search = search

      const response = await axios.get('/api/forum/posts/my', { params: paramsObj })
      const { content, totalElements, currentPage, pageSize: respPageSize } = response.data

      return {
        list: content.map(transformPost),
        total: totalElements,
        page: currentPage + 1,
        pageSize: respPageSize
      }
    } catch (error) {
      console.error('获取我的帖子失败:', error)
      return { list: [], total: 0, page: 1, pageSize: 10 }
    }
  },

  async getMyFavorites(params = {}) {
    try {
      const { search } = params
      const page = parseInt(params.page) || 0
      const pageSize = parseInt(params.pageSize) || 10

      const paramsObj = { page, size: pageSize }
      if (search) paramsObj.search = search

      const response = await axios.get('/api/forum/posts/favorites', { params: paramsObj })
      const { content, totalElements, currentPage, pageSize: respPageSize } = response.data

      return {
        list: content.map(transformPost),
        total: totalElements,
        page: currentPage + 1,
        pageSize: respPageSize
      }
    } catch (error) {
      console.error('获取我的收藏失败:', error)
      return { list: [], total: 0, page: 1, pageSize: 10 }
    }
  },

  getNotifications(userId) {
    const MOCK_NOTIFICATIONS = [
      {
        id: 'n1',
        userId: '2',
        type: 'reply',
        sourceId: 'c3',
        source: { postId: '2', postTitle: '关于DE-RRT*算法参数调优的讨论' },
        read: false,
        createdAt: '2026-06-07T15:20:00Z'
      },
      {
        id: 'n2',
        userId: '5',
        type: 'reply',
        sourceId: 'c4',
        source: { postId: '3', postTitle: '飞控端GPS信号异常问题反馈' },
        read: false,
        createdAt: '2026-06-07T17:00:00Z'
      }
    ]
    const notifications = MOCK_NOTIFICATIONS.filter(n => n.userId === userId)
    notifications.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    return Promise.resolve(notifications)
  },

  markNotificationAsRead(id) {
    return Promise.resolve({ success: true })
  },

  markAllNotificationsAsRead(userId) {
    return Promise.resolve({ success: true })
  },

  getUsers() {
    return Promise.resolve(MOCK_USERS)
  }
}

const MOCK_POSTS = [
  {
    id: '1',
    title: 'V2.0版本发布公告',
    content: '<p>各位同事，经过两周的开发和测试，我们的无人机路径规划系统V2.0版本正式发布！</p><p><strong>新增功能：</strong></p><ul><li>支持VRPTW路径规划算法</li><li>集成实时气象数据</li><li>新增多语言支持</li><li>优化界面响应速度</li></ul>',
    section: 'announcement',
    status: 'pinned',
    author: { id: '1', username: 'admin', fullName: '系统管理员' },
    viewCount: 128,
    likeCount: 24,
    commentCount: 2,
    createdAt: '2026-06-08T10:00:00',
    tags: ['版本发布', '系统更新']
  },
  {
    id: '2',
    title: '关于DE-RRT*算法参数调优的讨论',
    content: '<p>最近在测试DE-RRT*算法时发现，当障碍物密度较高时，路径规划时间明显增加。欢迎大家分享优化经验。</p>',
    section: 'tech_discuss',
    status: 'normal',
    author: { id: '2', username: 'dev01', fullName: '张三' },
    viewCount: 86,
    likeCount: 15,
    commentCount: 1,
    createdAt: '2026-06-07T14:30:00',
    tags: ['算法', 'DE-RRT*', '参数调优']
  },
  {
    id: '3',
    title: '飞控端GPS信号异常问题反馈',
    content: '<p>今天在现场测试时发现，无人机在飞行过程中出现GPS信号丢失的情况，导致自动返航功能触发。</p>',
    section: 'task_collab',
    status: 'normal',
    author: { id: '3', username: 'test01', fullName: '李四' },
    viewCount: 45,
    likeCount: 8,
    commentCount: 1,
    createdAt: '2026-06-07T16:45:00',
    tags: ['问题反馈', 'GPS', '飞控']
  },
  {
    id: '4',
    title: '系统部署指南（完整版）',
    content: '<h3>1. 环境要求</h3><p>操作系统：Ubuntu 20.04 LTS</p><p>内存：至少8GB</p>',
    section: 'knowledge',
    status: 'pinned',
    author: { id: '4', username: 'deploy01', fullName: '王五' },
    viewCount: 203,
    likeCount: 32,
    commentCount: 1,
    createdAt: '2026-06-05T09:00:00',
    tags: ['部署', '文档', '指南']
  },
  {
    id: '5',
    title: '建议增加任务批量导出功能',
    content: '<p>建议增加任务批量导出功能，可以方便地将任务数据导出为Excel格式，便于线下分析和归档。</p>',
    section: 'feedback',
    status: 'normal',
    author: { id: '5', username: 'flight01', fullName: '赵六' },
    viewCount: 32,
    likeCount: 12,
    commentCount: 2,
    createdAt: '2026-06-06T11:00:00',
    tags: ['功能建议'],
    feedbackStatus: 'pending'
  },
  {
    id: '6',
    title: '地图加载缓慢问题',
    content: '<p>在使用系统时发现地图加载速度较慢，特别是在网络不稳定的情况下。希望能优化地图加载性能。</p>',
    section: 'feedback',
    status: 'normal',
    author: { id: '6', username: 'prod01', fullName: '孙七' },
    viewCount: 28,
    likeCount: 8,
    commentCount: 1,
    createdAt: '2026-06-06T15:30:00',
    tags: ['性能优化', '地图'],
    feedbackStatus: 'processing'
  },
  {
    id: '7',
    title: '路径规划算法对比分析',
    content: '<p>对比了A*、Dijkstra和DE-RRT*三种算法在不同场景下的性能表现，分享一下分析结果...</p>',
    section: 'tech_discuss',
    status: 'normal',
    author: { id: '2', username: 'dev01', fullName: '张三' },
    viewCount: 67,
    likeCount: 21,
    commentCount: 3,
    createdAt: '2026-06-05T14:00:00',
    tags: ['算法', '性能分析']
  },
  {
    id: '8',
    title: '团队协作流程优化建议',
    content: '<p>建议增加任务分配提醒功能，当任务分配给某人时自动发送通知，提高团队协作效率。</p>',
    section: 'task_collab',
    status: 'normal',
    author: { id: '3', username: 'test01', fullName: '李四' },
    viewCount: 42,
    likeCount: 9,
    commentCount: 2,
    createdAt: '2026-06-04T10:30:00',
    tags: ['协作', '建议']
  }
]

function getMockPosts(params = {}) {
  const { section, search } = params
  const page = parseInt(params.page) || 1
  const pageSize = parseInt(params.pageSize) || 10

  let posts = [...MOCK_POSTS]

  if (section) {
    posts = posts.filter(p => p.section === section)
  }

  if (search) {
    const searchLower = search.toLowerCase()
    posts = posts.filter(p =>
      p.title.toLowerCase().includes(searchLower) ||
      p.content.toLowerCase().includes(searchLower)
    )
  }

  posts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  const start = (page - 1) * pageSize
  const end = start + pageSize
  const paginatedPosts = posts.slice(start, end)

  return {
    list: paginatedPosts.map(transformPost),
    total: posts.length,
    page,
    pageSize
  }
}

export { SECTIONS, SECTION_LABELS }
export default forumApi