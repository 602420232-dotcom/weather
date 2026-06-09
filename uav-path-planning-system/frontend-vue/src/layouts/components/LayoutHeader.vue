<template>
  <el-header class="uav-header" height="52px">
    <div class="header-left">
      <el-button
        v-if="isMobile"
        text
        class="hamburger-btn"
        :aria-label="$t('layout.openNav')"
        @click="$emit('toggle-drawer')"
      >
        <el-icon :size="20"><Menu /></el-icon>
      </el-button>
      <el-button
        v-else
        text
        class="collapse-btn"
        :aria-label="collapsed ? $t('layout.expandSidebar') : $t('layout.collapseSidebar')"
        @click="$emit('toggle-sidebar')"
      >
        <el-icon :size="18">
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
      </el-button>
      <span class="breadcrumb-title">{{ currentTitle }}</span>
    </div>

    <div class="header-right">
      <el-tag
        v-if="demoMode"
        type="info"
        effect="plain"
        size="default"
        class="demo-tag"
      >
        <el-icon><MagicStick /></el-icon>&nbsp;
        <span class="hide-on-mobile">{{ $t('demo.tag') }}</span>
      </el-tag>
      <el-tag v-else type="success" effect="plain" size="default" class="demo-tag">
        <el-icon><Sunny /></el-icon>&nbsp;
        <span class="hide-on-mobile">{{ $t('production.tag') }}</span>
      </el-tag>

      <!-- 位置信息 (子组件) -->
      <slot name="location" />

      <!-- 时间信息 (子组件) -->
      <slot name="clock" />

      <!-- 天气信息 (子组件) -->
      <slot name="weather" />

      <!-- 主题切换 -->
      <el-button
        text
        @click="$emit('toggle-theme')"
        class="theme-btn"
        :aria-label="isDark ? $t('layout.switchToLight') : $t('layout.switchToDark')"
      >
        <el-icon :size="18"><Moon v-if="!isDark" /><Sunny v-else /></el-icon>
      </el-button>

      <!-- 通知铃铛 -->
      <el-badge
        :value="unreadCount"
        :hidden="unreadCount === 0"
        class="notification-badge"
        :max="99"
      >
        <el-button
          text
          class="notification-btn"
          :aria-label="$t('layout.notificationCenter')"
          @click="$emit('open-notifications')"
        >
          <el-icon :size="18"><Bell /></el-icon>
        </el-button>
      </el-badge>

      <!-- 用户下拉 -->
      <el-dropdown @command="(cmd) => $emit('user-command', cmd)">
        <span class="user-info">
          <el-avatar :size="30" class="user-avatar">
            {{ displayName.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="user-name hide-on-mobile">{{ displayName }}</span>
          <el-tag size="small" class="role-tag hide-on-mobile" :type="roleTagType">
            {{ roleLabel }}
          </el-tag>
          <el-icon class="hide-on-mobile"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>{{ $t('userDisplay.profile') }}
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>{{ $t('layout.systemSettings') }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>{{ $t('userDisplay.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import {
  Menu, Fold, Expand, Moon, Sunny, ArrowDown,
  User, SwitchButton, Bell, MagicStick, Setting
} from '@element-plus/icons-vue'

defineProps({
  isMobile: { type: Boolean, default: false },
  collapsed: { type: Boolean, default: false },
  isDark: { type: Boolean, default: false },
  demoMode: { type: Boolean, default: true },
  currentTitle: { type: String, default: '' },
  displayName: { type: String, default: '' },
  roleLabel: { type: String, default: '' },
  roleTagType: { type: String, default: 'info' },
  unreadCount: { type: Number, default: 0 }
})

defineEmits([
  'toggle-drawer', 'toggle-sidebar', 'toggle-theme',
  'open-notifications', 'user-command'
])
</script>

<style scoped>
.uav-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn { color: #333 !important; padding: 0 8px !important; }
.breadcrumb-title {
  font-size: 15px;
  font-weight: 500;
  color: #24292f;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.demo-tag { margin-right: 8px; }
.theme-btn { color: #666 !important; padding: 0 8px !important; }
.notification-btn { color: #666 !important; padding: 0 8px !important; }
.notification-badge { margin-right: 4px; }
.hamburger-btn { color: #333 !important; padding: 0 8px !important; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 8px;
  color: #24292f;
}
.user-avatar {
  background: linear-gradient(135deg, #1890ff, #52c41a);
  color: #fff;
  font-weight: 600;
}
.user-name {
  font-size: 14px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-tag { margin-left: 4px; }
.hide-on-mobile { display: inline; }

@media (max-width: 768px) {
  .hide-on-mobile { display: none !important; }
  .header-right { gap: 8px; }
  .demo-tag { margin-right: 0; font-size: 12px; }
  .uav-header { padding: 0 10px; }
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
