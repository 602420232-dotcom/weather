/** 角色定义 */
export const ROLES: Record<string, string> = {
  USER: 'user',
  PRODUCTION: 'production',
  FLIGHT: 'flight',
  TESTER: 'tester',
  DEPLOYMENT: 'deployment',
  ADMIN: 'admin'
}

export const ROLE_LABELS: Record<string, string> = {
  [ROLES.USER]: '普通用户',
  [ROLES.PRODUCTION]: '生产人员',
  [ROLES.FLIGHT]: '飞控人员',
  [ROLES.TESTER]: '测试人员',
  [ROLES.DEPLOYMENT]: '部署人员',
  [ROLES.ADMIN]: '管理员'
}

export const ROLE_LABELS_EN: Record<string, string> = {
  [ROLES.USER]: 'Normal User',
  [ROLES.PRODUCTION]: 'Production Staff',
  [ROLES.FLIGHT]: 'Flight Control',
  [ROLES.TESTER]: 'Tester',
  [ROLES.DEPLOYMENT]: 'Deployment Engineer',
  [ROLES.ADMIN]: 'Administrator'
}

/** 团队定义 */
export const TEAMS: string[] = ['team-a', 'team-b', 'team-c']

export const TEAM_LABELS: Record<string, string> = {
  'team-a': '团队 A',
  'team-b': '团队 B',
  'team-c': '团队 C'
}

/** 数据范围定义 */
export const DATA_SCOPES: string[] = ['personal', 'team', 'all']

export const DATA_SCOPE_LABELS: Record<string, string> = {
  personal: '个人',
  team: '团队',
  all: '全部'
}

export const DEFAULT_DATA_SCOPE_BY_ROLE: Record<string, string> = {
  user: 'personal',
  admin: 'all',
  production: 'team',
  flight: 'team',
  tester: 'team',
  deployment: 'team'
}

/** 演示账号 -> 团队映射 */
export const DEMO_USER_TEAM_MAP: Record<string, string> = {
  user01: 'team-a',
  flight01: 'team-b',
  prod01: 'team-a',
  test01: 'team-c',
  deploy01: 'team-c',
  admin01: 'team-a'
}

export interface DemoAccount {
  username: string
  password: string
  role: string
  displayName: string
}

/** 默认账号 */
export const DEFAULT_ACCOUNTS: DemoAccount[] = [
  { username: 'user01', password: 'User@123456', role: ROLES.USER, displayName: '普通用户测试账号' },
  { username: 'prod01', password: 'Prod@123456', role: ROLES.PRODUCTION, displayName: '生产人员测试账号' },
  { username: 'flight01', password: 'Flight@123456', role: ROLES.FLIGHT, displayName: '飞控人员测试账号' },
  { username: 'test01', password: 'Test@123456', role: ROLES.TESTER, displayName: '测试人员测试账号' },
  { username: 'deploy01', password: 'Deploy@123456', role: ROLES.DEPLOYMENT, displayName: '部署人员测试账号' },
  { username: 'admin01', password: 'Admin@123456', role: ROLES.ADMIN, displayName: '系统管理员' }
]

/** 演示模式登录匹配 map */
export const DEMO_USER_MAP: Record<string, DemoAccount> = DEFAULT_ACCOUNTS.reduce((acc: Record<string, DemoAccount>, a: DemoAccount) => {
  acc[a.username] = a
  return acc
}, {})
