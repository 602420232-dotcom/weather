<template>
  <div class="uav-layout" :class="{ 'is-collapsed': appStore.collapsed, 'is-dark': appStore.isDark, 'is-mobile': isMobile, 'is-tablet': isTablet }">
    <!-- 无障碍跳转链接 -->
    <a href="#main-content" class="skip-link">{{ $t('app.skipToContent') }}</a>

    <!-- 演示模式顶部提示条 -->
    <div v-if="authStore.demoMode" class="demo-banner">
      <el-icon class="demo-icon"><InfoFilled /></el-icon>
      <span v-html="$t('demo.banner')"></span>
      <el-button type="primary" link @click="goToApiConfig" v-if="canAccessApiConfig">
        {{ $t('demo.goToApiConfig') }}
      </el-button>
    </div>

    <!-- 首次进入 Toast 提示 -->
    <el-alert
      v-if="showDemoToast"
      class="demo-toast"
      :title="$t('demo.welcomeTitle')"
      type="info"
      :description="$t('demo.welcomeDescription')"
      show-icon
      :closable="true"
      @close="onDemoToastClose"
    />

    <el-container class="uav-container">
      <!-- 桌面端侧边栏 -->
      <LayoutSidebar
        v-if="!isMobile"
        :collapsed="appStore.collapsed"
        :current-route="currentRoute"
        :menu-items="menuItems"
        :brand-text="$t('app.brand')"
        :docs-label="$t('layout.openDocs')"
        :settings-label="$t('layout.settings')"
        :system-settings-label="$t('layout.systemSettings')"
        :theme-label="$t('layout.themeCustomizer')"
        :permission-debug-label="$t('layout.permissionDebug')"
        :show-permission-debug="authStore.hasRouteAccess('permission-debug')"
        :icon-map="ICON_MAP"
        @go-home="goHome"
      />

      <!-- 移动端抽屉 -->
      <LayoutMobileDrawer
        v-model="drawerVisible"
        :current-route="currentRoute"
        :menu-items="menuItems"
        :brand-text="$t('app.brand')"
        :docs-label="$t('layout.openDocs')"
        :settings-label="$t('layout.settings')"
        :system-settings-label="$t('layout.systemSettings')"
        :theme-label="$t('layout.themeCustomizer')"
        :permission-debug-label="$t('layout.permissionDebug')"
        :show-permission-debug="authStore.hasRouteAccess('permission-debug')"
        :icon-map="ICON_MAP"
        @go-home="goHome"
        @select="drawerVisible = false"
      />

      <!-- 通知中心抽屉 -->
      <NotificationDrawer v-model="notificationDrawerVisible" />

      <!-- 右侧主区域 -->
      <el-container class="uav-main-container">
        <!-- 顶部栏 -->
        <LayoutHeader
          :is-mobile="isMobile"
          :collapsed="appStore.collapsed"
          :is-dark="appStore.isDark"
          :demo-mode="authStore.demoMode"
          :current-title="currentTitle"
          :display-name="authStore.displayName"
          :role-label="authStore.roleLabel"
          :role-tag-type="roleTagType"
          :unread-count="notificationStore.unreadCount"
          @toggle-drawer="drawerVisible = true"
          @toggle-sidebar="appStore.toggleSidebar()"
          @toggle-theme="appStore.toggleTheme()"
          @open-notifications="notificationDrawerVisible = true"
          @user-command="onUserCommand"
        >
          <template #location>
            <HeaderLocation
              :location-text="locationText"
              :is-locating="isLocating"
              @fetch="fetchCurrentLocation"
            />
          </template>
          <template #clock>
            <HeaderClock />
          </template>
          <template #weather>
            <HeaderWeather
              :weather-icon="weatherIcon"
              :weather-text="weatherText"
            />
          </template>
        </LayoutHeader>

        <!-- 内容区 -->
        <el-main id="main-content" class="uav-main" tabindex="-1">
          <ErrorBoundary>
            <router-view v-slot="{ Component }" :key="route.fullPath">
              <transition name="fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </ErrorBoundary>
        </el-main>

        <!-- 底部 -->
        <el-footer class="uav-footer" height="32px">
          <span>{{ t('app.title') }} · v3.3.0 · MIT License</span>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  HomeFilled, PartlyCloudy, Goods, Monitor, List, Position, Connection,
  DataAnalysis, Coin, Box, Setting, Tools, Document,
  MagicStick, InfoFilled, Cpu, ChatDotRound, Menu, Bell, MapLocation, Clock, Loading,
  Checked, TrendCharts, PieChart, Histogram, User, Link, Share, Aim, Files,
  SetUp, Lock, Notebook
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useAppStore } from '../stores/app'
import { useNotificationStore } from '../stores/notification'
import NotificationDrawer from '../components/shared/NotificationDrawer.vue'
import ErrorBoundary from '../components/shared/ErrorBoundary.vue'
import LayoutSidebar from './components/LayoutSidebar.vue'
import LayoutMobileDrawer from './components/LayoutMobileDrawer.vue'
import LayoutHeader from './components/LayoutHeader.vue'
import HeaderClock from './components/HeaderClock.vue'
import HeaderLocation from './components/HeaderLocation.vue'
import HeaderWeather from './components/HeaderWeather.vue'
import { getCurrentLocation } from '../utils/geolocation'
import { getCurrentWeather } from '../utils/weatherApi'

const authStore = useAuthStore()
const appStore = useAppStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const showDemoToast = ref(false)
const drawerVisible = ref(false)
const notificationDrawerVisible = ref(false)

const isMobile = ref(false)
const isTablet = ref(false)
let mqHandler = null
let tabletMqHandler = null

const locationText = ref(t('location.clickToFetch'))
const currentLocation = ref(null)
const isLocating = ref(false)

const weatherIcon = ref('☀️')
const weatherText = ref(t('demo.defaultWeather'))

async function updateWeather() {
  if (!currentLocation.value?.position) return
  const { latitude, longitude } = currentLocation.value.position
  const result = await getCurrentWeather(latitude, longitude)
  if (result.success) {
    weatherIcon.value = result.data.icon
    weatherText.value = `${result.data.description} ${result.data.temp}`
  } else if (result.fallback) {
    weatherIcon.value = result.fallback.data.icon
    weatherText.value = `${result.fallback.data.description} ${result.fallback.data.temp}`
  }
}

async function fetchCurrentLocation() {
  if (isLocating.value) return
  isLocating.value = true
  locationText.value = t('location.fetching')

  const result = await getCurrentLocation()

  if (result.success) {
    currentLocation.value = result
    if (result.address && result.address.formatted) {
      const addr = result.address.formatted
      const regionName = result.region?.name || ''
      locationText.value = regionName ? `${regionName} · ${addr}` : addr
    } else if (result.position) {
      const { latitude, longitude } = result.position
      locationText.value = `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
    } else {
      locationText.value = t('location.invalid')
    }
    updateWeather()
  } else {
    locationText.value = t('location.failed')
    console.warn('[Header] Failed to get location:', result.error)
  }
  isLocating.value = false
}

function updateIsMobile() {
  if (typeof window === 'undefined' || !window.matchMedia) return
  isMobile.value = window.matchMedia('(max-width: 768px)').matches
  isTablet.value = window.matchMedia('(min-width: 769px) and (max-width: 1024px)').matches
}

onMounted(() => {
  if (authStore.demoMode && !authStore.demoShownOnce) {
    showDemoToast.value = true
    authStore.markDemoShown()
  }
  if (typeof window !== 'undefined' && window.matchMedia) {
    const mql = window.matchMedia('(max-width: 768px)')
    mqHandler = (e) => {
      isMobile.value = e.matches
      if (e.matches) drawerVisible.value = false
    }
    if (typeof mql.addEventListener === 'function') {
      mql.addEventListener('change', mqHandler)
    } else if (typeof mql.addListener === 'function') {
      mql.addListener(mqHandler)
    }
    isMobile.value = mql.matches

    // 平板断点监听
    const tabletMql = window.matchMedia('(min-width: 769px) and (max-width: 1024px)')
    tabletMqHandler = (e) => {
      isTablet.value = e.matches
    }
    if (typeof tabletMql.addEventListener === 'function') {
      tabletMql.addEventListener('change', tabletMqHandler)
    } else if (typeof tabletMql.addListener === 'function') {
      tabletMql.addListener(tabletMqlHandler)
    }
    isTablet.value = tabletMql.matches
  }

  // 系统上线通知
  try {
    const today = new Date().toISOString().slice(0, 10)
    const lastShown = localStorage.getItem('uav_system_notify_date_v1')
    if (lastShown !== today) {
      notificationStore.pushWithDesktop({
        type: 'info',
        title: t('notification.systemOnline'),
        message: t('notification.systemOnlineMsg'),
        source: 'system'
      })
      localStorage.setItem('uav_system_notify_date_v1', today)
    }
  } catch (_) {}

  if (authStore.userId) {
    notificationStore.connect(authStore.userId)
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined' && window.matchMedia) {
    if (mqHandler) {
      const mql = window.matchMedia('(max-width: 768px)')
      if (typeof mql.removeEventListener === 'function') {
        mql.removeEventListener('change', mqHandler)
      } else if (typeof mql.removeListener === 'function') {
        mql.removeListener(mqHandler)
      }
    }
    if (tabletMqHandler) {
      const tabletMql = window.matchMedia('(min-width: 769px) and (max-width: 1024px)')
      if (typeof tabletMql.removeEventListener === 'function') {
        tabletMql.removeEventListener('change', tabletMqHandler)
      } else if (typeof tabletMql.removeListener === 'function') {
        tabletMql.removeListener(tabletMqHandler)
      }
    }
  }
  notificationStore.disconnect()
})

function onDemoToastClose() { showDemoToast.value = false }

const currentRoute = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || t('layout.breadcrumbHome'))

const canAccessApiConfig = computed(() => authStore.hasRouteAccess('api-config'))

const roleTagType = computed(() => {
  const map = {
    admin: 'danger', deployment: 'warning', flight: 'success',
    production: '', tester: 'info', user: 'info'
  }
  return map[authStore.role] || 'info'
})

const ICON_MAP = {
  HomeFilled, PartlyCloudy, Goods, Monitor, List, Position, Connection,
  DataAnalysis, Coin, Box, Setting, Tools, Document,
  ChatDotRound, MagicStick, InfoFilled, Cpu, Menu, Bell, MapLocation, Clock, Loading,
  Checked, TrendCharts, PieChart, Histogram, User, Link, Share, Aim, Files,
  SetUp, Lock, Notebook
}

const menuItems = computed(() => {
  const all = [
    { key: 'dashboard', title: t('menu.dashboard'), icon: 'HomeFilled', path: '/dashboard' },
    { key: 'weather', title: t('menu.weather'), icon: 'PartlyCloudy', path: '/weather' },
    { key: 'weather-station', title: t('menu.weatherStation'), icon: 'Position', path: '/weather-station' },
    { key: 'weather-source', title: t('menu.weatherSource'), icon: 'Connection', path: '/weather-source' },
    { key: 'orders', title: t('menu.orders'), icon: 'Goods', path: '/orders' },
    { key: 'cockpit', title: t('menu.cockpit'), icon: 'Monitor', path: '/cockpit' },
    { key: 'tasks', title: t('menu.tasks'), icon: 'List', path: '/tasks' },
    { key: 'task-report', title: t('menu.taskReport'), icon: 'Notebook', path: '/task-report' },
    { key: 'utm-integration', title: t('menu.utmIntegration'), icon: 'Link', path: '/utm-integration' },
    { key: 'path-planning', title: t('menu.pathPlanning'), icon: 'Aim', path: '/path-planning' },
    { key: 'airworthiness', title: t('menu.airworthiness'), icon: 'Checked', path: '/airworthiness' },
    { key: 'model-evaluation', title: t('menu.modelEvaluation'), icon: 'TrendCharts', path: '/model-evaluation' },
    { key: 'parameter-tuning', title: t('menu.parameterTuning'), icon: 'SetUp', path: '/parameter-tuning' },
    { key: 'sensitivity-analysis', title: t('paramsTuning.sensitivity'), icon: 'PieChart', path: '/sensitivity-analysis' },
    { key: 'experiment-compare', title: t('menu.experimentCompare'), icon: 'Histogram', path: '/experiment-compare' },
    { key: 'assimilation', title: t('menu.dataAssimilation'), icon: 'Share', path: '/assimilation' },
    { key: 'monitoring', title: t('menu.monitoring'), icon: 'DataAnalysis', path: '/monitoring' },
    { key: 'database', title: t('menu.database'), icon: 'Coin', path: '/database' },
    { key: 'docker', title: t('menu.docker'), icon: 'Box', path: '/docker' },
    { key: 'docker-build', title: t('menu.dockerBuild'), icon: 'Files', path: '/docker-build' },
    { key: 'api-config', title: t('menu.apiConfig'), icon: 'Cpu', path: '/api-config' },
    { key: 'forum', title: t('menu.forum'), icon: 'ChatDotRound', path: '/forum' },
    { key: 'user-stats', title: t('menu.userStats'), icon: 'User', path: '/user-stats', roles: ['admin'] },
    { key: 'permission-templates', title: t('menu.permissionTemplates'), icon: 'Lock', path: '/permission-templates' }
  ]
  return all.filter(item => authStore.hasRouteAccess(item.key))
})

function goHome() {
  const defaultRoute = appStore.getDefaultRoute(authStore.role)
  router.push('/' + defaultRoute)
}

function goToApiConfig() { router.push('/api-config') }

function onUserCommand(cmd) {
  switch (cmd) {
    case 'profile': router.push('/settings'); break
    case 'settings': router.push('/settings'); break
    case 'logout':
      ElMessage.success(t('userDisplay.loggedOut'))
      authStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style scoped>
.uav-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}
.uav-layout.is-dark {
  background: var(--bg-primary, #0a0e1a);
  color: var(--text-primary, #c9d1d9);
  color-scheme: dark;
}

.skip-link {
  position: absolute;
  left: -9999px;
  top: 8px;
  z-index: 9999;
  padding: 8px 16px;
  background: #1890ff;
  color: #fff;
  font-size: 14px;
  border-radius: 0 4px 4px 0;
  text-decoration: none;
}
.skip-link:focus { left: 0; }

.demo-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-surface);
  color: var(--color-warning);
  font-size: 13px;
  border-bottom: 1px solid var(--color-border);
}
.demo-banner .demo-icon { font-size: 16px; }
.demo-banner strong { color: var(--color-warning); margin: 0 2px; }

.demo-toast { margin: 8px 16px; }

.uav-container {
  height: calc(100vh - (var(--banner-height, 0px)));
}

.uav-main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.uav-main {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.uav-footer {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-muted);
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

/* 页面切换动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (prefers-reduced-motion: reduce) {
  .fade-enter-active, .fade-leave-active { transition: none; }
}

/* 暗色主题 */
.is-dark .uav-header { background: var(--color-header-bg); border-bottom-color: var(--color-header-border); }
.is-dark .collapse-btn { color: var(--color-text) !important; }
.is-dark .breadcrumb-title { color: var(--color-text); }
.is-dark .theme-btn { color: #e5c07b !important; }
.is-dark .notification-btn { color: var(--color-text) !important; }
.is-dark .user-info { color: var(--color-text); }
.is-dark .uav-footer { border-top-color: var(--color-border); color: var(--color-text-muted); }
.is-dark .hamburger-btn { color: var(--color-text) !important; }
.is-dark .info-group .location-info,
.is-dark .info-group .time-info,
.is-dark .info-group .weather-info {
  background: var(--color-hover);
  color: var(--color-text);
}
.is-dark .info-group .info-icon { color: var(--color-primary); }
.is-dark .info-group .location-info.is-clickable:hover {
  background: var(--color-hover);
}

/* 移动端 */
.is-mobile .uav-main { padding: 12px; }
.is-mobile .breadcrumb-title { font-size: 14px; }

@media (max-width: 768px) {
  .uav-footer { font-size: 11px; padding: 0 8px; }
  .demo-banner { font-size: 12px; padding: 6px 10px; }
}

@media (max-width: 480px) {
  .breadcrumb-title {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
