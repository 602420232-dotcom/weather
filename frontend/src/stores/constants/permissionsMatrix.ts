import { ROLES } from './roles'

type RouteKey = string
type RoleKey = string
type ActionKey = string

/** 权限矩阵（角色 → 可访问的路由 key 列表） */
export const PERMISSION_MATRIX: Record<RoleKey, RouteKey[]> = {
  [ROLES.USER]: [
    'dashboard', 'weather', 'orders', 'forum', 'settings', 'docs', 'theme-customizer'
  ],
  [ROLES.PRODUCTION]: [
    'dashboard', 'weather', 'orders', 'cockpit', 'tasks',
    'forum', 'utm-integration', 'task-report', 'settings', 'docs', 'theme-customizer'
  ],
  [ROLES.FLIGHT]: [
    'dashboard', 'weather', 'cockpit', 'tasks', 'path-planning',
    'airworthiness', 'model-evaluation', 'parameter-tuning', 'sensitivity-analysis',
    'experiment-compare', 'assimilation',
    'forum', 'utm-integration', 'task-report', 'settings', 'docs', 'theme-customizer'
  ],
  [ROLES.TESTER]: [
    'dashboard', 'weather', 'weather-station', 'weather-source', 'path-planning', 'airworthiness',
    'model-evaluation', 'parameter-tuning', 'sensitivity-analysis', 'experiment-compare',
    'assimilation', 'monitoring',
    'forum', 'settings', 'docs', 'theme-customizer'
  ],
  [ROLES.DEPLOYMENT]: [
    'dashboard', 'weather', 'weather-station', 'weather-source', 'monitoring', 'docker', 'docker-build',
    'api-config',
    'forum', 'settings', 'docs', 'theme-customizer'
  ],
  [ROLES.ADMIN]: [
    'dashboard', 'weather', 'weather-station', 'weather-source', 'orders', 'cockpit', 'tasks',
    'path-planning', 'airworthiness', 'model-evaluation', 'parameter-tuning', 'sensitivity-analysis',
    'experiment-compare', 'assimilation', 'monitoring', 'database',
    'docker', 'docker-build', 'api-config', 'permission-templates', 'utm-integration', 'task-report',
    'forum', 'user-stats',
    'settings', 'docs', 'theme-customizer', 'permission-debug'
  ]
}

/** 按钮级权限（页面内的操作权限） */
export const ACTION_PERMISSIONS: Record<ActionKey, RoleKey[]> = {
  // === 通用权限 ===
  'common:export': [ROLES.ADMIN],
  'common:delete': [ROLES.ADMIN],
  'common:edit': [ROLES.ADMIN],

  // === 气象数据 ===
  'weather:view': [ROLES.USER, ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'weather:download': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'weather:advanced': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],

  // === 下单 ===
  'orders:view': [ROLES.USER, ROLES.PRODUCTION, ROLES.ADMIN],
  'orders:create': [ROLES.USER, ROLES.PRODUCTION, ROLES.ADMIN],
  'orders:advanced': [ROLES.FLIGHT, ROLES.ADMIN],
  'orders:cancel': [ROLES.USER, ROLES.PRODUCTION, ROLES.ADMIN],

  // === 驾驶舱 ===
  'cockpit:view': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.ADMIN],
  'cockpit:control': [ROLES.FLIGHT, ROLES.ADMIN],
  'cockpit:emergency': [ROLES.FLIGHT, ROLES.ADMIN],

  // === 任务管理 ===
  'tasks:view': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.ADMIN],
  'tasks:create': [ROLES.PRODUCTION, ROLES.ADMIN],
  'tasks:edit': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.ADMIN],
  'tasks:delete': [ROLES.ADMIN],
  'tasks:assign': [ROLES.PRODUCTION, ROLES.ADMIN],

  // === API 配置 ===
  'api-config:view': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'api-config:edit': [ROLES.ADMIN],

  // === 路径规划 ===
  'planning:view': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'planning:execute': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'planning:save': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],

  // === 模型评估 ===
  'evaluation:view': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'evaluation:run': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'evaluation:compare': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],

  // === 参数调优 ===
  'tuning:view': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'tuning:adjust': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'tuning:save': [ROLES.ADMIN],

  // === 数据同化 ===
  'assimilation:view': [ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'assimilation:execute': [ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'assimilation:config': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'assimilation:download': [ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'assimilation:delete': [ROLES.DEPLOYMENT, ROLES.ADMIN],

  // === 数据库 ===
  'database:view': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'database:backup': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'database:restore': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'database:config': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'database:cleanup': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'database:edit': [ROLES.ADMIN],
  'database:execute': [ROLES.ADMIN],

  // === Docker ===
  'docker:view': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'docker:restart': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'docker:build': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'docker:logs': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'docker:cleanup': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'docker:stop': [ROLES.ADMIN],

  // === 系统监控 ===
  'monitoring:view': [ROLES.DEPLOYMENT, ROLES.ADMIN],
  'monitoring:restart': [ROLES.ADMIN],
  'monitoring:stop': [ROLES.ADMIN],
  'monitoring:config': [ROLES.ADMIN],

  // === 任务报告 ===
  'report:view': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'report:create': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.ADMIN],
  'report:export': [ROLES.ADMIN],
  'report:delete': [ROLES.ADMIN],

  // === 气象站点 ===
  'weather-station:view': [ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'weather-station:add': [ROLES.ADMIN],
  'weather-station:edit': [ROLES.ADMIN],
  'weather-station:delete': [ROLES.ADMIN],
  'weather-station:toggle': [ROLES.ADMIN],

  // === 气象数据源 ===
  'weather-source:view': [ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'weather-source:add': [ROLES.ADMIN],
  'weather-source:edit': [ROLES.ADMIN],
  'weather-source:delete': [ROLES.ADMIN],
  'weather-source:toggle': [ROLES.ADMIN],
  'weather-source:config': [ROLES.ADMIN],

  // === 适航性 ===
  'airworthiness:view': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'airworthiness:evaluate': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'airworthiness:approve': [ROLES.ADMIN],

  // === 敏感性分析 ===
  'sensitivity:view': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'sensitivity:run': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'sensitivity:export': [ROLES.ADMIN],

  // === 实验对比 ===
  'experiment:view': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'experiment:run': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],
  'experiment:compare': [ROLES.FLIGHT, ROLES.TESTER, ROLES.ADMIN],

  // === 论坛 ===
  'forum:view': [ROLES.USER, ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'forum:post': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'forum:comment': [ROLES.USER, ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.TESTER, ROLES.DEPLOYMENT, ROLES.ADMIN],
  'forum:delete': [ROLES.ADMIN],
  'forum:pin': [ROLES.ADMIN],
  'forum:admin': [ROLES.ADMIN],

  // === 用户统计 ===
  'user-stats:view': [ROLES.ADMIN],
  'user-stats:export': [ROLES.ADMIN],

  // === UTM ===
  'utm:view': [ROLES.PRODUCTION, ROLES.FLIGHT, ROLES.ADMIN],
  'utm:request': [ROLES.FLIGHT, ROLES.ADMIN],
  'utm:approve': [ROLES.ADMIN]
}
