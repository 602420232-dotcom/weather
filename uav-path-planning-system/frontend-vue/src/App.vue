<template>
  <!-- 动态渐变背景（深色系） -->
  <div class="bg-animation">
    <div class="color"></div>
    <div class="color"></div>
    <div class="color"></div>
    <div class="color"></div>
    <div class="color"></div>
    <div class="color"></div>
  </div>

  <a-layout class="app-layout">
    <!-- 顶部导航栏 -->
    <a-layout-header class="header">
      <div class="logo">无人机路径规划系统</div>
      <a-menu
        mode="horizontal"
        :selected-keys="[currentRoute]"
        class="nav-menu"
        theme="dark"
        @click="handleMenuClick"
      >
        <a-menu-item key="/">
          <template #icon>
            <HomeOutlined />
          </template>
          首页
        </a-menu-item>
        <a-menu-item key="/smart-cockpit">
          <template #icon>
            <RadarChartOutlined />
          </template>
          智能驾驶舱
        </a-menu-item>
        <a-menu-item key="/path-planning">
          <template #icon>
            <OrderedListOutlined />
          </template>
          路径规划
        </a-menu-item>
        <a-menu-item key="/weather">
          <template #icon>
            <CloudOutlined />
          </template>
          气象数据
        </a-menu-item>
        <a-menu-item key="/tasks">
          <template #icon>
            <CheckCircleOutlined />
          </template>
          任务管理
        </a-menu-item>
        <a-menu-item key="/drones">
          <template #icon>
            <RocketOutlined />
          </template>
          无人机管理
        </a-menu-item>
        <a-menu-item key="/history">
          <template #icon>
            <HistoryOutlined />
          </template>
          历史记录
        </a-menu-item>
        <a-menu-item key="/data-sources">
          <template #icon>
            <DatabaseOutlined />
          </template>
          数据源管理
        </a-menu-item>
        <a-menu-item key="/monitoring">
          <template #icon>
            <DashboardOutlined />
          </template>
          系统监控
        </a-menu-item>
        <a-menu-item key="/example">
          <template #icon>
            <BookOutlined />
          </template>
          功能示范
        </a-menu-item>
      </a-menu>
    </a-layout-header>

    <!-- 内容区域 -->
    <a-layout-content class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </a-layout-content>

    <!-- 底部 -->
    <a-layout-footer class="footer">
      无人机路径规划系统 ©2024
    </a-layout-footer>
  </a-layout>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeOutlined,
  OrderedListOutlined,
  CloudOutlined,
  CheckCircleOutlined,
  RocketOutlined,
  HistoryOutlined,
  DatabaseOutlined,
  DashboardOutlined,
  BookOutlined,
  RadarChartOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const currentRoute = ref('/')

watch(() => route.path, (newPath) => {
  currentRoute.value = newPath
}, { immediate: true })

function handleMenuClick({ key }) {
  router.push(key)
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  background: rgba(18, 20, 30, 0.45);
  backdrop-filter: blur(24px) saturate(140%);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 1px 1px 0 rgba(255, 255, 255, 0.06);
}

.logo {
  color: #e8e8ec;
  font-size: 20px;
  font-weight: 700;
  margin-right: 30px;
  letter-spacing: -0.02em;
}

.nav-menu {
  flex: 1;
  background: transparent !important;
  border-bottom: none !important;
}

.content {
  padding: 24px;
  margin: 0;
  min-height: 280px;
  position: relative;
  z-index: 1;
}

/* 内容卡片容器 */
.content > div {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 24px;
}

.footer {
  text-align: center;
  background: rgba(0, 0, 0, 0.1);
  color: rgba(255, 255, 255, 0.5);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 0.85rem;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<!-- 非 scoped：深色背景动画 -->
<style>
html {
  background: #0a0d16;
}

body {
  margin: 0;
  background: transparent;
  color: #e8e8ec;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
}

html,
body {
  height: 100%;
  overflow: hidden;
}

#app {
  height: 100%;
  overflow-y: auto;
}

.app-layout {
  min-height: 100vh;
  position: relative;
  z-index: 1;
  background: transparent !important;
}

.bg-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.color {
  position: absolute;
  filter: blur(200px);
  border-radius: 50%;
  opacity: 0.55;
  will-change: filter;
  transform: translateZ(0);
  backface-visibility: hidden;
}

.color:nth-child(1) {
  top: -350px;
  left: -100px;
  width: 600px;
  height: 600px;
  background: rgb(60, 120, 255);
  animation: colorOne 4s ease infinite alternate;
}

@keyframes colorOne {
  0%   { background: rgb(60, 120, 255); }
  25%  { background: rgb(55, 160, 250); }
  50%  { background: rgb(50, 185, 255); }
  75%  { background: rgb(55, 230, 240); }
  100% { background: rgb(50, 245, 210); }
}

.color:nth-child(2) {
  bottom: -150px;
  left: 100px;
  width: 500px;
  height: 500px;
  background: rgb(45, 185, 255);
  animation: colorTwo 4s ease infinite alternate;
}

@keyframes colorTwo {
  0%   { background: rgb(45, 185, 255); }
  25%  { background: rgb(55, 150, 255); }
  50%  { background: rgb(60, 140, 255); }
  75%  { background: rgb(55, 155, 255); }
  100% { background: rgb(65, 100, 255); }
}

.color:nth-child(3) {
  bottom: 50px;
  right: 100px;
  width: 500px;
  height: 500px;
  background: rgb(100, 210, 245);
  animation: colorThree 4s ease infinite alternate;
}

@keyframes colorThree {
  0%   { background: rgb(100, 210, 245); }
  25%  { background: rgb(120, 165, 245); }
  50%  { background: rgb(70, 70, 240); }
  75%  { background: rgb(95, 70, 245); }
  100% { background: rgb(140, 65, 245); }
}

.color:nth-child(4) {
  top: -300px;
  right: -20px;
  width: 600px;
  height: 600px;
  background: rgb(150, 105, 240);
  animation: colorFour 4s ease infinite alternate;
}

@keyframes colorFour {
  0%   { background: rgb(150, 105, 240); }
  25%  { background: rgb(220, 110, 245); }
  50%  { background: rgb(240, 110, 210); }
  75%  { background: rgb(245, 115, 180); }
  100% { background: rgb(240, 120, 140); }
}

.color:nth-child(5) {
  top: 20px;
  left: 40%;
  width: 400px;
  height: 300px;
  background: rgb(120, 155, 250);
  animation: colorFive 4s ease infinite alternate;
}

@keyframes colorFive {
  0%   { background: rgb(120, 155, 250); }
  25%  { background: rgb(130, 240, 185); }
  50%  { background: rgb(170, 245, 140); }
  75%  { background: rgb(235, 245, 130); }
  100% { background: rgb(245, 200, 130); }
}

.color:nth-child(6) {
  bottom: 20px;
  left: 50%;
  width: 300px;
  height: 300px;
  background: rgb(65, 225, 235);
  animation: colorSix 4s ease infinite alternate;
}

@keyframes colorSix {
  0%   { background: rgb(65, 225, 235); }
  25%  { background: rgb(65, 235, 120); }
  50%  { background: rgb(235, 225, 65); }
  75%  { background: rgb(235, 155, 60); }
  100% { background: rgb(235, 100, 55); }
}
</style>
