<template>
  <el-drawer
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    direction="ltr"
    size="280px"
    :with-header="false"
    class="uav-drawer"
  >
    <div class="drawer-logo" @click="$emit('go-home')">
      <div class="logo-icon">
        <el-icon :size="22"><PartlyCloudy /></el-icon>
      </div>
      <span class="logo-text">{{ brandText }}</span>
    </div>

    <el-menu
      :default-active="currentRoute"
      class="uav-menu drawer-menu"
      background-color="var(--color-sidebar-bg, #001529)"
      text-color="#c9d1d9"
      active-text-color="#52c41a"
      router
      @select="$emit('select')"
    >
      <template v-for="item in menuItems" :key="item.key">
        <el-menu-item v-if="!item.children" :index="item.path" :disabled="item.disabled">
          <el-icon><component :is="ICON_MAP[item.icon]" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>

        <el-sub-menu v-else :index="'sub-' + item.key">
          <template #title>
            <el-icon><component :is="ICON_MAP[item.icon]" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.key"
            :index="child.path"
            :disabled="child.disabled"
          >
            <el-icon><component :is="ICON_MAP[child.icon]" /></el-icon>
            <template #title>{{ child.title }}</template>
          </el-menu-item>
        </el-sub-menu>
      </template>

      <el-divider style="margin: 8px 0; border-color: rgba(255,255,255,0.1);" />

      <el-menu-item index="/docs">
        <el-icon><Document /></el-icon>
        <template #title>{{ docsLabel }}</template>
      </el-menu-item>

      <el-sub-menu index="sub-settings">
        <template #title>
          <el-icon><Tools /></el-icon>
          <span>{{ settingsLabel }}</span>
        </template>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>{{ systemSettingsLabel }}</template>
        </el-menu-item>
        <el-menu-item index="/theme-customizer">
          <el-icon><MagicStick /></el-icon>
          <template #title>{{ themeLabel }}</template>
        </el-menu-item>
        <el-menu-item
          v-if="showPermissionDebug"
          index="/permission-debug"
        >
          <el-icon><WarningFilled /></el-icon>
          <template #title>{{ permissionDebugLabel }}</template>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </el-drawer>
</template>

<script setup>
import { PartlyCloudy, Document, Tools, Setting, MagicStick, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  currentRoute: { type: String, default: '/' },
  menuItems: { type: Array, default: () => [] },
  brandText: { type: String, default: '' },
  docsLabel: { type: String, default: '' },
  settingsLabel: { type: String, default: '' },
  systemSettingsLabel: { type: String, default: '' },
  themeLabel: { type: String, default: '' },
  permissionDebugLabel: { type: String, default: '' },
  showPermissionDebug: { type: Boolean, default: false },
  iconMap: { type: Object, default: () => ({}) }
})

defineEmits(['update:modelValue', 'go-home', 'select'])

const ICON_MAP = props.iconMap
</script>

<style scoped>
.uav-drawer :deep(.el-drawer) {
  background: var(--color-sidebar-bg, #001529);
}
.uav-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: var(--color-sidebar-bg, #001529);
}
.drawer-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 52px;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid var(--color-sidebar-border, #1f2a3d);
  cursor: pointer;
}
.logo-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: linear-gradient(135deg, #1890ff, #52c41a);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.logo-text {
  white-space: nowrap;
  overflow: hidden;
}
.drawer-menu {
  border-right: none;
  height: calc(100vh - 52px);
  overflow-y: auto;
}
</style>
