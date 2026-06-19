// 用户活动日志 API
// 记录用户的所有操作行为

const MOCK_ACTIVITY_LOGS = [
  {
    id: 'log1',
    userId: 'admin',
    userName: '系统管理员',
    action: 'login',
    actionLabel: '登录系统',
    module: '系统',
    details: '成功登录系统',
    ip: '北京',
    timestamp: '2026-06-09T09:00:00Z'
  },
  {
    id: 'log2',
    userId: 'dev',
    userName: '张三',
    action: 'create_post',
    actionLabel: '发布帖子',
    module: '论坛',
    details: '发布了帖子《关于DE-RRT*算法参数调优的讨论》',
    ip: '四川成都',
    timestamp: '2026-06-09T10:30:00Z'
  },
  {
    id: 'log3',
    userId: 'dev',
    userName: '张三',
    action: 'comment',
    actionLabel: '发表评论',
    module: '论坛',
    details: '在帖子《飞控端GPS信号异常问题反馈》下发表评论',
    ip: '四川成都',
    timestamp: '2026-06-09T11:15:00Z'
  },
  {
    id: 'log4',
    userId: 'test',
    userName: '李四',
    action: 'login',
    actionLabel: '登录系统',
    module: '系统',
    details: '成功登录系统',
    ip: '江苏南京',
    timestamp: '2026-06-09T09:30:00Z'
  },
  {
    id: 'log5',
    userId: 'test',
    userName: '李四',
    action: 'view_page',
    actionLabel: '访问页面',
    module: '气象站',
    details: '访问了气象数据页面',
    ip: '江苏南京',
    timestamp: '2026-06-09T10:00:00Z'
  },
  {
    id: 'log6',
    userId: 'deploy',
    userName: '王五',
    action: 'create_post',
    actionLabel: '发布帖子',
    module: '论坛',
    details: '发布了帖子《系统部署指南（完整版）》',
    ip: '广东深圳',
    timestamp: '2026-06-08T14:20:00Z'
  },
  {
    id: 'log7',
    userId: 'admin',
    userName: '系统管理员',
    action: 'create_post',
    actionLabel: '发布帖子',
    module: '论坛',
    details: '发布了帖子《V2.0版本发布公告》',
    ip: '北京',
    timestamp: '2026-06-08T10:00:00Z'
  },
  {
    id: 'log8',
    userId: 'flight',
    userName: '赵六',
    action: 'create_post',
    actionLabel: '发布帖子',
    module: '论坛',
    details: '发布了帖子《飞控端GPS信号异常问题反馈》',
    ip: '浙江杭州',
    timestamp: '2026-06-07T16:45:00Z'
  },
  {
    id: 'log9',
    userId: 'prod',
    userName: '孙七',
    action: 'create_post',
    actionLabel: '发布帖子',
    module: '论坛',
    details: '发布了帖子《生产环境数据同步任务》',
    ip: '上海',
    timestamp: '2026-06-08T08:30:00Z'
  },
  {
    id: 'log10',
    userId: 'dev',
    userName: '张三',
    action: 'view_page',
    actionLabel: '访问页面',
    module: '路径规划',
    details: '访问了路径规划页面',
    ip: '四川成都',
    timestamp: '2026-06-09T14:00:00Z'
  },
  {
    id: 'log11',
    userId: 'dev',
    userName: '张三',
    action: 'algorithm_test',
    actionLabel: '算法测试',
    module: '算法',
    details: '测试了DE-RRT*算法',
    ip: '四川成都',
    timestamp: '2026-06-09T14:30:00Z'
  },
  {
    id: 'log12',
    userId: 'test',
    userName: '李四',
    action: 'comment',
    actionLabel: '发表评论',
    module: '论坛',
    details: '在帖子《系统部署指南（完整版）》下发表评论',
    ip: '江苏南京',
    timestamp: '2026-06-08T15:00:00Z'
  }
]

// 模拟统计数据
const MOCK_USER_STATS = [
  {
    userId: 'admin',
    userName: '系统管理员',
    role: '管理员',
    postCount: 2,
    commentCount: 1,
    loginCount: 15,
    lastActive: '2026-06-09T09:00:00Z',
    location: '北京'
  },
  {
    userId: 'dev',
    userName: '张三',
    role: '开发',
    postCount: 5,
    commentCount: 8,
    loginCount: 22,
    lastActive: '2026-06-09T14:30:00Z',
    location: '四川成都'
  },
  {
    userId: 'test',
    userName: '李四',
    role: '测试',
    postCount: 1,
    commentCount: 6,
    loginCount: 18,
    lastActive: '2026-06-09T10:00:00Z',
    location: '江苏南京'
  },
  {
    userId: 'deploy',
    userName: '王五',
    role: '部署',
    postCount: 3,
    commentCount: 2,
    loginCount: 12,
    lastActive: '2026-06-08T14:20:00Z',
    location: '广东深圳'
  },
  {
    userId: 'flight',
    userName: '赵六',
    role: '飞控',
    postCount: 2,
    commentCount: 0,
    loginCount: 8,
    lastActive: '2026-06-07T16:45:00Z',
    location: '浙江杭州'
  },
  {
    userId: 'prod',
    userName: '孙七',
    role: '生产',
    postCount: 1,
    commentCount: 0,
    loginCount: 5,
    lastActive: '2026-06-08T08:30:00Z',
    location: '上海'
  }
]

const activityLogApi = {
  // 获取活动日志列表
  getActivityLogs(params = {}) {
    let logs = [...MOCK_ACTIVITY_LOGS]
    
    // 按用户筛选
    if (params.userId) {
      logs = logs.filter(log => log.userId === params.userId)
    }
    
    // 按操作类型筛选
    if (params.action) {
      logs = logs.filter(log => log.action === params.action)
    }
    
    // 按模块筛选
    if (params.module) {
      logs = logs.filter(log => log.module === params.module)
    }
    
    // 按日期筛选
    if (params.startDate) {
      logs = logs.filter(log => new Date(log.timestamp) >= new Date(params.startDate))
    }
    if (params.endDate) {
      logs = logs.filter(log => new Date(log.timestamp) <= new Date(params.endDate))
    }
    
    // 排序（默认按时间倒序）
    logs.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
    
    // 分页
    const page = parseInt(params.page) || 1
    const pageSize = parseInt(params.pageSize) || 20
    const total = logs.length
    const start = (page - 1) * pageSize
    const end = start + pageSize
    
    return Promise.resolve({
      list: logs.slice(start, end),
      total,
      page,
      pageSize
    })
  },

  // 记录活动日志
  logActivity(data) {
    const newLog = {
      id: `log_${Date.now()}`,
      ...data,
      timestamp: new Date().toISOString()
    }
    MOCK_ACTIVITY_LOGS.unshift(newLog)
    return Promise.resolve(newLog)
  },

  // 获取用户统计
  getUserStats() {
    return Promise.resolve([...MOCK_USER_STATS])
  },

  // 获取总览统计
  getOverviewStats() {
    const totalPosts = MOCK_USER_STATS.reduce((sum, user) => sum + user.postCount, 0)
    const totalComments = MOCK_USER_STATS.reduce((sum, user) => sum + user.commentCount, 0)
    const totalLogins = MOCK_USER_STATS.reduce((sum, user) => sum + user.loginCount, 0)
    const activeUsers = MOCK_USER_STATS.filter(user => {
      const lastActive = new Date(user.lastActive)
      const now = new Date()
      const diffHours = (now - lastActive) / (1000 * 60 * 60)
      return diffHours < 24 // 24小时内活跃
    }).length

    // 地域分布
    const locationStats = {}
    MOCK_USER_STATS.forEach(user => {
      const province = user.location.replace(/[\u4e00-\u9fa5]+$/, '') // 提取省份
      locationStats[province] = (locationStats[province] || 0) + 1
    })

    // 操作类型分布
    const actionStats = {}
    MOCK_ACTIVITY_LOGS.forEach(log => {
      actionStats[log.actionLabel] = (actionStats[log.actionLabel] || 0) + 1
    })

    return Promise.resolve({
      totalUsers: MOCK_USER_STATS.length,
      totalPosts,
      totalComments,
      totalLogins,
      activeUsers,
      locationStats,
      actionStats
    })
  },

  // 导出日志（返回JSON格式，支持前端转换）
  exportLogs(params = {}) {
    return this.getActivityLogs(params).then(result => {
      return Promise.resolve({
        data: result.list,
        columns: [
          { key: 'timestamp', label: '时间' },
          { key: 'userName', label: '用户名' },
          { key: 'actionLabel', label: '操作' },
          { key: 'module', label: '模块' },
          { key: 'details', label: '详情' },
          { key: 'ip', label: '地理位置' }
        ]
      })
    })
  },

  // 导出用户统计（返回JSON格式）
  exportUserStats() {
    return Promise.resolve({
      data: MOCK_USER_STATS.map(user => ({
        用户名: user.userName,
        角色: user.role,
        发帖数: user.postCount,
        评论数: user.commentCount,
        登录次数: user.loginCount,
        所在地: user.location,
        最后活跃时间: user.lastActive
      })),
      columns: [
        { key: '用户名', label: '用户名' },
        { key: '角色', label: '角色' },
        { key: '发帖数', label: '发帖数' },
        { key: '评论数', label: '评论数' },
        { key: '登录次数', label: '登录次数' },
        { key: '所在地', label: '所在地' },
        { key: '最后活跃时间', label: '最后活跃时间' }
      ]
    })
  }
}

export default activityLogApi