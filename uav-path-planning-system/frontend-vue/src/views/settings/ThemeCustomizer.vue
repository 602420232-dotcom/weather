<template>
  <div class="theme-customizer">
    <!-- 顶部栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon :size="22"><MagicStick /></el-icon>
          主题定制
        </h2>
        <el-tag type="info" effect="plain" class="demo-tag" v-if="authStore.demoMode">
          <el-icon><MagicStick /></el-icon>&nbsp;演示模式
        </el-tag>
      </div>
      <div class="header-right">
        <span class="label-text">当前主题：</span>
        <el-tag :type="currentTagType" effect="light" size="large">
          {{ currentThemeLabel }}
        </el-tag>
        <el-button type="primary" :icon="RefreshLeft" @click="resetToDefault">
          重置为默认
        </el-button>
      </div>
    </div>

    <!-- 主体区域（左右双栏） -->
    <el-row :gutter="20" class="main-body">
      <!-- 顶部：主题模式下拉（含自动） -->
      <el-col :span="24" class="mode-selector-col">
        <el-card shadow="hover" class="mode-selector-card">
          <div class="mode-selector-row">
            <el-icon :size="18"><Brush /></el-icon>
            <span class="mode-label">主题模式：</span>
            <el-select
              v-model="modeValue"
              style="width: 220px"
              placeholder="选择主题模式"
              @change="onModeChange"
            >
              <el-option
                v-for="opt in modeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-tag
              v-if="modeValue === 'auto'"
              :type="isNightNow ? 'info' : 'warning'"
              effect="light"
              size="default"
              class="night-status-tag"
            >
              {{ isNightNow ? '🌙 夜间 · 已应用深色主题' : '☀️ 白天 · 已应用浅色主题' }}
            </el-tag>
            <el-tag v-else :type="currentTagType" effect="light" size="default">
              当前主题：{{ currentThemeLabel }}
            </el-tag>
            <el-button
              v-if="modeValue === 'auto'"
              type="primary"
              plain
              size="small"
              :icon="RefreshLeft"
              @click="forceRefreshNight"
            >
              刷新日夜状态
            </el-button>
          </div>
          <div class="mode-tip">
            自动模式会根据本地时间在日落（19:00 后）与日出（06:00 前）自动切换主题。深色主题默认启用 15% 蓝光过滤（CSS 变量 --color-blue-filter: 0.85）。
          </div>
        </el-card>
      </el-col>

      <!-- 左栏：主题预设卡片 -->
      <el-col :xs="24" :md="9">
        <el-card shadow="hover" class="preset-card">
          <template #header>
            <div class="card-header">
              <el-icon><Brush /></el-icon>
              <span>主题预设</span>
            </div>
          </template>

          <div class="preset-grid">
            <div
              v-for="preset in presets"
              :key="preset.key"
              class="preset-item"
              :class="{ active: currentTheme === preset.key }"
              @click="switchTheme(preset.key)"
            >
              <div class="preset-preview" :style="preset.previewStyle">
                <div class="preview-bg"></div>
                <div class="preview-block" :style="{ background: preset.colors.surface, borderColor: preset.colors.border }"></div>
                <div class="preview-dots">
                  <span :style="{ background: preset.colors.primary }"></span>
                  <span :style="{ background: preset.colors.accent }"></span>
                </div>
              </div>
              <div class="preset-meta">
                <span class="preset-name">{{ preset.label }}</span>
                <span class="preset-desc">{{ preset.description }}</span>
              </div>
              <div class="color-swatches">
                <div v-for="(color, name) in preset.colors" :key="name" class="swatch" :style="{ background: color }" :title="name"></div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右栏：自定义色板 + 自定义背景 -->
      <el-col :xs="24" :md="15">
        <el-card shadow="hover" class="custom-card">
          <template #header>
            <div class="card-header">
              <el-icon><Setting /></el-icon>
              <span>自定义色板（高级模式）</span>
            </div>
          </template>

          <!-- 颜色选择器 -->
          <div class="color-pickers">
            <div class="picker-row">
              <div
                v-for="color in colorList"
                :key="color.var"
                class="picker-item"
              >
                <label class="picker-label">{{ color.label }}</label>
                <el-color-picker
                  v-model="customColors[color.var]"
                  :show-alpha="false"
                  size="default"
                />
              </div>
            </div>
          </div>

          <el-divider />

          <!-- 滑动条 -->
          <div class="slider-group">
            <div class="slider-item">
              <div class="slider-head">
                <span>圆角大小</span>
                <el-tag size="small" type="info">{{ radiusValue }}px</el-tag>
              </div>
              <el-slider
                v-model="radiusValue"
                :min="0"
                :max="20"
                :step="1"
                show-stops
              />
            </div>

            <div class="slider-item">
              <div class="slider-head">
                <span>阴影强度</span>
                <el-tag size="small" type="info">{{ shadowLevel }}</el-tag>
              </div>
              <el-slider
                v-model="shadowLevel"
                :min="0"
                :max="3"
                :step="1"
                :marks="shadowMarks"
                show-stops
              />
            </div>
          </div>

          <el-divider />

          <!-- 应用按钮 -->
          <div class="action-bar">
            <el-button @click="loadFromCurrent">从当前主题继承</el-button>
            <el-button type="warning" @click="clearCustomColors">清空自定义</el-button>
            <el-button type="primary" :icon="MagicStickIcon" @click="applyCustom">应用自定义色板</el-button>
          </div>
        </el-card>

        <!-- 自定义背景 -->
        <el-card shadow="hover" class="bg-card">
          <template #header>
            <div class="card-header">
              <el-icon><PictureFilled /></el-icon>
              <span>自定义背景</span>
              <el-tag v-if="bgImageData" type="success" size="small" effect="light">已设置</el-tag>
            </div>
          </template>

          <!-- 上传区域 -->
          <div class="bg-upload-area" @click="triggerBgUpload" v-if="!bgImagePreview">
            <el-icon :size="36" class="upload-placeholder-icon"><UploadFilled /></el-icon>
            <span class="upload-text">点击此处上传背景图片</span>
            <span class="upload-hint">支持 JPG / PNG / WebP，建议尺寸 1920×1080</span>
          </div>

          <!-- 预览区域 -->
          <div class="bg-preview-area" v-else>
            <div class="bg-preview-img" :style="{ backgroundImage: `url(${bgImagePreview})` }"></div>
            <div class="bg-preview-actions">
              <el-button size="small" type="primary" plain @click="triggerBgUpload">
                <el-icon><UploadFilled /></el-icon> 更换图片
              </el-button>
              <el-button size="small" type="danger" plain @click="removeBgPreview">
                <el-icon><DeleteFilled /></el-icon> 移除
              </el-button>
            </div>
          </div>

          <input
            ref="bgFileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style="display: none"
            @change="handleBgFileChange"
          />

          <el-divider />

          <!-- 背景设置 -->
          <div class="slider-group">
            <div class="slider-item">
              <div class="slider-head">
                <span>背景图可见度</span>
                <el-tag size="small" type="info">{{ Math.round(bgOpacity * 100) }}%</el-tag>
              </div>
              <el-slider
                v-model="bgOpacity"
                :min="0.05"
                :max="1"
                :step="0.05"
                :format-tooltip="(val) => Math.round(val * 100) + '%'"
                show-stops
              />
            </div>

            <div class="bg-size-row">
              <span class="bg-size-label">显示方式</span>
              <el-radio-group v-model="bgSize" size="small">
                <el-radio-button value="cover">填充</el-radio-button>
                <el-radio-button value="contain">完整</el-radio-button>
                <el-radio-button value="auto">原始</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <el-divider />

          <div class="action-bar">
            <el-button
              v-if="bgApplied"
              type="danger"
              plain
              :icon="DeleteIcon"
              @click="removeBgImage"
            >
              移除背景
            </el-button>
            <el-button
              type="primary"
              :icon="PictureIcon"
              :disabled="!bgImagePreview"
              @click="applyBgImage"
            >
              应用背景
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  MagicStick, Brush, Setting, RefreshLeft, PictureFilled, UploadFilled, DeleteFilled
} from '@element-plus/icons-vue'
import { useAppStore } from '../../stores/app'
import { useAuthStore } from '../../stores/auth'
import { isNightTime } from '../../composables/useSunTime'

const MagicStickIcon = MagicStick
const PictureIcon = PictureFilled
const DeleteIcon = DeleteFilled

const appStore = useAppStore()
const authStore = useAuthStore()

const THEME_KEY = 'uav_theme_v1'
const CUSTOM_VARS_KEY = 'uav_theme_custom_vars_v1'
const BLUE_FILTER_VAR = '--color-blue-filter'

const DARK_THEMES = ['dark', 'brand', 'highContrast']
const BG_STORAGE_KEY = 'uav_theme_bg_v1'
const MAX_BG_SIZE_BYTES = 20 * 1024 * 1024 // 20MB 上传上限
const BG_MAX_DIM = 1920    // Canvas 压缩后最大边长（像素），可平衡画质与存储
const BG_COMPRESS_QUALITY = 0.75 // JPEG 压缩质量 0-1，值越大小文件越大

// ===== 自定义背景 =====
const bgFileInput = ref(null)
const bgImageData = ref('')        // 已保存的 base64 数据
const bgImagePreview = ref('')     // 当前预览的 base64
const bgSize = ref('cover')
const bgOpacity = ref(0.5)
const bgApplied = ref(false)

function triggerBgUpload() {
  bgFileInput.value?.click()
}

function compressImage(dataUrl) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      // 计算缩放：保持宽高比，最大边不超过 BG_MAX_DIM
      let { width: w, height: h } = img
      if (w > BG_MAX_DIM || h > BG_MAX_DIM) {
        const ratio = BG_MAX_DIM / Math.max(w, h)
        w = Math.round(w * ratio)
        h = Math.round(h * ratio)
      }
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, w, h)
      // JPEG 格式压缩（体积远小于 PNG），若需保留透明通道可改用 'image/png'
      resolve(canvas.toDataURL('image/jpeg', BG_COMPRESS_QUALITY))
    }
    img.onerror = () => reject(new Error('图片解码失败'))
    img.src = dataUrl
  })
}

function handleBgFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > MAX_BG_SIZE_BYTES) {
    ElMessage.warning(`图片大小不能超过 ${Math.round(MAX_BG_SIZE_BYTES / 1024 / 1024)}MB`)
    return
  }
  const reader = new FileReader()
  reader.onload = async (ev) => {
    const raw = String(ev.target?.result || '')
    try {
      bgImagePreview.value = await compressImage(raw)
    } catch {
      // 压缩失败时直接用原图（体积可能较大）
      ElMessage.warning('图片压缩失败，将使用原始图片')
      bgImagePreview.value = raw
    }
  }
  reader.onerror = () => {
    ElMessage.error('图片读取失败')
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

function removeBgPreview() {
  bgImagePreview.value = ''
}

function applyBgImage() {
  if (!bgImagePreview.value) return
  bgImageData.value = bgImagePreview.value
  applyBackgroundToDOM()
  persistBackground()
  bgApplied.value = true
  ElMessage.success('背景已应用')
}

function buildOverlayColor(dark) {
  return dark
    ? `rgba(10, 14, 26, ${(1 - bgOpacity.value).toFixed(2)})`
    : `rgba(245, 247, 250, ${(1 - bgOpacity.value).toFixed(2)})`
}

function applyBackgroundToDOM(targetTheme = null) {
  if (typeof document === 'undefined') return
  const img = bgImageData.value
  const appEl = document.getElementById('app')
  if (!img || !appEl) {
    removeBgFromDOM()
    return
  }
  const isDark = targetTheme
    ? DARK_THEMES.includes(targetTheme)
    : isCurrentlyDark()
  const overlay = buildOverlayColor(isDark)

  appEl.style.setProperty(
    'background',
    `linear-gradient(${overlay}, ${overlay}), url(${img}) center/${bgSize.value} no-repeat fixed`,
    'important'
  )
  document.body.classList.add('has-custom-bg')
}

function removeBgFromDOM() {
  if (typeof document === 'undefined') return
  const appEl = document.getElementById('app')
  document.body.classList.remove('has-custom-bg')
  if (appEl) {
    appEl.style.removeProperty('background')
  }
}

function removeBgImage() {
  bgImageData.value = ''
  bgImagePreview.value = ''
  bgApplied.value = false
  removeBgFromDOM()
  localStorage.removeItem(BG_STORAGE_KEY)
  ElMessage.success('背景已移除')
}

function persistBackground() {
  try {
    localStorage.setItem(BG_STORAGE_KEY, JSON.stringify({
      data: bgImageData.value,
      size: bgSize.value,
      opacity: bgOpacity.value
    }))
  } catch (e) {
    ElMessage.warning('图片过大，无法保存到本地存储。请尝试压缩为更小的图片后再上传。')
  }
}

function restoreBackground() {
  try {
    const raw = localStorage.getItem(BG_STORAGE_KEY)
    if (!raw) return
    const saved = JSON.parse(raw)
    if (saved.data) {
      bgImageData.value = saved.data
      bgImagePreview.value = saved.data
      if (saved.size) bgSize.value = saved.size
      if (saved.opacity !== undefined) bgOpacity.value = saved.opacity
      applyBackgroundToDOM()
      bgApplied.value = true
    }
  } catch (_) {}
}

// ===== 自定义背景（结束） =====

// 判断当前激活的主题是否为暗色系（包括自定义暗色）
function isCurrentlyDark() {
  const activeTheme = appStore.effectiveTheme || appStore.theme || 'light'
  if (DARK_THEMES.includes(activeTheme)) return true
  if (activeTheme === 'custom') {
    if (typeof document !== 'undefined') {
      const bg = document.documentElement.style.getPropertyValue('--color-bg')
      if (bg) {
        // 简单亮度检测：如果背景色偏暗则视为暗色主题
        const hex = bg.replace('#', '')
        if (hex.length === 6) {
          const r = parseInt(hex.substring(0, 2), 16)
          const g = parseInt(hex.substring(2, 4), 16)
          const b = parseInt(hex.substring(4, 6), 16)
          const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
          return luminance < 0.4
        }
      }
    }
  }
  return false
}

const modeOptions = [
  { value: 'auto', label: '自动（跟随日落）' },
  { value: 'light', label: '浅色（固定）' },
  { value: 'dark', label: '深色（固定）' },
  { value: 'brand', label: '品牌（固定）' },
  { value: 'highContrast', label: '高对比度（固定）' }
]

const isNightNow = ref(isNightTime())
function refreshNight() {
  isNightNow.value = isNightTime()
}

const modeValue = computed({
  get() {
    return appStore.themeMode || 'auto'
  },
  set(val) {
    appStore.setThemeMode(val)
    refreshNight()
    applyBlueFilter()
  }
})

function onModeChange(val) {
  appStore.setThemeMode(val)
  refreshNight()
  applyBlueFilter()
  ElMessage.success(`已切换为 ${modeOptions.find(o => o.value === val)?.label || val} 模式`)
}

function forceRefreshNight() {
  appStore.refreshNightStatus()
  refreshNight()
  applyBlueFilter()
  ElMessage.info('日夜状态已刷新')
}

const presets = [
  {
    key: 'light',
    label: '浅色',
    description: '明亮清爽',
    colors: {
      bg: '#f5f7fa',
      surface: '#ffffff',
      text: '#1f2937',
      border: '#e5e7eb',
      primary: '#409EFF',
      success: '#67C23A',
      warning: '#E6A23C',
      danger: '#F56C6C',
      accent: '#f97316'
    },
    previewStyle: { background: '#f5f7fa', color: '#1f2937' }
  },
  {
    key: 'dark',
    label: '深色',
    description: '护眼深色',
    colors: {
      bg: '#0f172a',
      surface: '#1e293b',
      text: '#e2e8f0',
      border: '#334155',
      primary: '#60a5fa',
      success: '#4ade80',
      warning: '#fbbf24',
      danger: '#f87171',
      accent: '#fb923c'
    },
    previewStyle: { background: '#0f172a', color: '#e2e8f0' }
  },
  {
    key: 'brand',
    label: '品牌',
    description: '无人机橙蓝',
    colors: {
      bg: '#0c1a2f',
      surface: '#172554',
      text: '#f8fafc',
      border: '#334155',
      primary: '#38bdf8',
      success: '#4ade80',
      warning: '#f59e0b',
      danger: '#ef4444',
      accent: '#f97316'
    },
    previewStyle: { background: '#0c1a2f', color: '#f8fafc' }
  },
  {
    key: 'highContrast',
    label: '高对比度',
    description: '无障碍模式',
    colors: {
      bg: '#000000',
      surface: '#000000',
      text: '#ffffff',
      border: '#ffffff',
      primary: '#ffff00',
      success: '#00ff00',
      warning: '#ffaa00',
      danger: '#ff6b6b',
      accent: '#ff00ff'
    },
    previewStyle: { background: '#000000', color: '#ffffff' }
  }
]

const colorList = [
  { var: '--color-primary', label: '主色' },
  { var: '--color-bg', label: '背景色' },
  { var: '--color-surface', label: '表面色' },
  { var: '--color-text', label: '文字色' },
  { var: '--color-border', label: '边框色' },
  { var: '--color-success', label: '成功色' },
  { var: '--color-warning', label: '警告色' },
  { var: '--color-danger', label: '危险色' },
  { var: '--color-accent', label: '强调色' }
]

const defaultCustomColors = {
  '--color-primary': '#409EFF',
  '--color-bg': '#f5f7fa',
  '--color-surface': '#ffffff',
  '--color-text': '#1f2937',
  '--color-border': '#e5e7eb',
  '--color-success': '#67C23A',
  '--color-warning': '#E6A23C',
  '--color-danger': '#F56C6C',
  '--color-accent': '#f97316'
}

const customColors = ref({ ...defaultCustomColors })
const radiusValue = ref(8)
const shadowLevel = ref(1)

const shadowMarks = {
  0: '无',
  1: '轻',
  2: '中',
  3: '强'
}

const currentTheme = computed(() => {
  // 检测是否处于自定义模式
  if (typeof document !== 'undefined') {
    if (document.documentElement.getAttribute('data-theme-custom') === 'true') {
      return 'custom'
    }
    const dt = document.documentElement.getAttribute('data-theme')
    if (dt) return dt
  }
  if (localStorage.getItem('uav_theme_custom_active') === 'true') return 'custom'
  return appStore.effectiveTheme || appStore.theme || 'light'
})

const currentThemeLabel = computed(() => {
  const t = currentTheme.value
  const found = presets.find(p => p.key === t)
  return found ? found.label : (t === 'custom' ? '自定义' : t)
})

const currentTagType = computed(() => {
  const t = currentTheme.value
  if (t === 'light') return 'success'
  if (t === 'dark') return 'info'
  if (t === 'brand') return 'warning'
  if (t === 'highContrast') return 'danger'
  if (t === 'custom') return isCurrentlyDark() ? 'info' : 'success'
  return ''
})

function applyBlueFilter() {
  if (typeof document === 'undefined') return
  document.documentElement.style.setProperty(BLUE_FILTER_VAR, isCurrentlyDark() ? '0.85' : '1')
}

function applyDataTheme(themeKey) {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', themeKey)
    localStorage.setItem(THEME_KEY, themeKey)
  }
  applyBlueFilter()
}

function switchTheme(key) {
  clearInlineCustomVars()
  localStorage.removeItem(CUSTOM_VARS_KEY)
  localStorage.removeItem('uav_theme_custom_active')
  // appStore.setThemeMode 内部会调用 applyCurrentTheme() → applyDataThemeToDocument()
  appStore.setThemeMode(key)
  // 先切换 theme 再更新背景覆盖层，以匹配新主题颜色
  if (bgImageData.value) {
    applyBackgroundToDOM(key)
    persistBackground()
  }
  refreshNight()
  ElMessage.success(`已切换为 ${presets.find(p => p.key === key)?.label || key} 主题`)
}

function resetToDefault() {
  clearInlineCustomVars()
  localStorage.removeItem(CUSTOM_VARS_KEY)
  localStorage.removeItem('uav_theme_custom_active')
  applyDataTheme('light')
  appStore.setThemeMode('light')
  refreshNight()
  customColors.value = { ...defaultCustomColors }
  radiusValue.value = 8
  shadowLevel.value = 1
  ElMessage.success('已重置为默认浅色主题')
}

function clearInlineCustomVars() {
  if (typeof document === 'undefined') return
  colorList.forEach(item => {
    document.documentElement.style.removeProperty(item.var)
  })
  document.documentElement.style.removeProperty('--radius-md')
  document.documentElement.style.removeProperty('--shadow-md')
  document.documentElement.removeAttribute('data-theme-custom')
  localStorage.removeItem('uav_theme_custom_active')
}

function applyCustom() {
  // 自定义色板作为覆盖层叠加在当前基础主题上，不改变 data-theme 属性
  // 这样 theme-dark.css 的 Element Plus 暗色组件规则不会被丢失
  const baseTheme = isCurrentlyDark() ? 'dark' : 'light'
  applyDataTheme(baseTheme)
  // 用独立的 key 标记处于自定义模式
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme-custom', 'true')
  }
  localStorage.setItem('uav_theme_custom_active', 'true')
  // 不重置 appStore 的 themeMode，切换预设时会通过 clearInlineCustomVars 清除自定义覆盖

  const vars = { ...customColors.value }
  Object.entries(vars).forEach(([k, v]) => {
    if (typeof document !== 'undefined') {
      document.documentElement.style.setProperty(k, v)
    }
  })
  if (typeof document !== 'undefined') {
    document.documentElement.style.setProperty('--radius-md', `${radiusValue.value}px`)
    document.documentElement.style.setProperty('--shadow-md', `0 ${4 * shadowLevel.value}px ${12 * shadowLevel.value}px rgba(0,0,0,0.12)`)
    vars['--radius-md'] = `${radiusValue.value}px`
    vars['--shadow-md'] = `0 ${4 * shadowLevel.value}px ${12 * shadowLevel.value}px rgba(0,0,0,0.12)`
  }

  try {
    localStorage.setItem(CUSTOM_VARS_KEY, JSON.stringify(vars))
  } catch (e) {
    ElMessage.error('保存失败：本地存储空间不足或已被禁用')
    return
  }
  applyBlueFilter()
  ElMessage.success('已应用自定义色板')
}

function loadFromCurrent() {
  if (typeof document === 'undefined') return
  const styles = getComputedStyle(document.documentElement)
  colorList.forEach(item => {
    const val = styles.getPropertyValue(item.var)
    if (val) customColors.value[item.var] = val.trim()
  })
  // 同时读取当前的 radius 和 shadow 值
  const currentRadius = styles.getPropertyValue('--radius-md')
  if (currentRadius) {
    radiusValue.value = parseInt(currentRadius) || 8
  }
  ElMessage.info('已从当前主题继承颜色')
}

function clearCustomColors() {
  customColors.value = { ...defaultCustomColors }
  radiusValue.value = 8
  shadowLevel.value = 1
}

onMounted(() => {
  if (typeof document !== 'undefined') {
    const isCustomActive = localStorage.getItem('uav_theme_custom_active') === 'true'
    if (isCustomActive) {
      // 恢复自定义主题：设置标记 + 恢复 CSS 变量
      document.documentElement.setAttribute('data-theme-custom', 'true')
      try {
        const raw = localStorage.getItem(CUSTOM_VARS_KEY)
        if (raw) {
          const vars = JSON.parse(raw)
          Object.entries(vars).forEach(([k, v]) => {
            if (k.startsWith('--color-') && customColors.value[k] !== undefined) {
              customColors.value[k] = v
              document.documentElement.style.setProperty(k, v)
            }
          })
          if (vars['--radius-md']) {
            radiusValue.value = parseInt(vars['--radius-md']) || 8
            document.documentElement.style.setProperty('--radius-md', vars['--radius-md'])
          }
          if (vars['--shadow-md']) {
            document.documentElement.style.setProperty('--shadow-md', vars['--shadow-md'])
          }
        }
      } catch (_) {}
    } else {
      // 非自定义主题：从当前激活的主题 CSS 变量中初始化颜色选择器
      loadFromCurrent()
    }
  }
  refreshNight()
  applyBlueFilter()
  restoreBackground()
})

watch(currentTheme, (newVal) => {
  if (typeof document !== 'undefined') {
    // 自定义主题不改变 data-theme，仅通过 inline style 覆盖；保持基础主题不变
    if (newVal === 'custom') return
    document.documentElement.setAttribute('data-theme', newVal)
    localStorage.setItem(THEME_KEY, newVal)
    applyBlueFilter()
  }
})

// 背景参数变动时实时更新
watch([bgOpacity, bgSize], () => {
  if (bgImageData.value) {
    applyBackgroundToDOM()
    persistBackground()
  }
})
</script>

<style scoped>
.theme-customizer {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--color-bg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  padding: 16px 20px;
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0,0,0,0.08));
  color: var(--color-text);
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text, #1f2937);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.label-text {
  font-size: 14px;
  color: var(--color-text-muted, #6b7280);
}

.main-body {
  margin: 0;
}

.preset-card,
.custom-card {
  background: var(--color-surface, #fff);
  color: var(--color-text, #1f2937);
  border: 1px solid var(--color-border, #e5e7eb);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.preset-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.preset-item {
  padding: 12px;
  border: 2px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preset-item:hover {
  border-color: var(--color-primary, #409EFF);
  transform: translateY(-1px);
}

.preset-item.active {
  border-color: var(--color-primary, #409EFF);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary, #409EFF) 25%, transparent);
}

.preset-preview {
  position: relative;
  height: 60px;
  border-radius: var(--radius-sm, 4px);
  overflow: hidden;
}

.preview-bg {
  position: absolute;
  inset: 0;
}

.preview-block {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 50%;
  height: 36px;
  border: 1px solid;
  border-radius: var(--radius-sm, 4px);
}

.preview-dots {
  position: absolute;
  top: 18px;
  right: 12px;
  display: flex;
  gap: 6px;
}

.preview-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.preset-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.preset-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text, #1f2937);
}

.preset-desc {
  font-size: 12px;
  color: var(--color-text-muted, #6b7280);
}

.color-swatches {
  display: flex;
  gap: 4px;
}

.swatch {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid var(--color-border, #e5e7eb);
}

.color-pickers {
  margin-top: 4px;
}

.picker-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.picker-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: color-mix(in srgb, var(--color-bg, #f5f7fa) 70%, transparent);
  border-radius: var(--radius-sm, 4px);
}

.picker-label {
  font-size: 13px;
  color: var(--color-text-muted, #6b7280);
}

.slider-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.slider-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.slider-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--color-text, #1f2937);
}

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .picker-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

</style>

<!-- 非 scoped：自定义背景激活时 UI 表面半透明效果 -->
<!-- 注意：不使用 backdrop-filter 和 .el-popper，避免 GPU 合成层干扰 Element Plus 颜色渲染 -->
<style>
/* ===== 根级容器 ===== */
html[data-theme='dark'] body.has-custom-bg,
html[data-theme='brand'] body.has-custom-bg,
html[data-theme='highContrast'] body.has-custom-bg {
  background-color: transparent !important;
}
body.has-custom-bg .app-container,
body.has-custom-bg .uav-layout,
body.has-custom-bg .uav-layout.is-dark {
  background: transparent !important;
}

/* ===== 侧边栏（实体，不参与透明） ===== */
body.has-custom-bg .uav-aside {
  background: var(--color-sidebar-bg, #001529) !important;
}

/* ===== 顶栏 ===== */
body.has-custom-bg .uav-header {
  background: rgba(255, 255, 255, 0.78) !important;
}
html[data-theme='dark'] body.has-custom-bg .uav-header,
html[data-theme='brand'] body.has-custom-bg .uav-header,
html[data-theme='highContrast'] body.has-custom-bg .uav-header,
body.has-custom-bg .is-dark .uav-header {
  background: rgba(13, 17, 23, 0.78) !important;
}

/* ===== 卡片 ===== */
body.has-custom-bg .el-card {
  background: rgba(255, 255, 255, 0.82) !important;
  border-color: rgba(229, 231, 235, 0.5) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-card,
html[data-theme='brand'] body.has-custom-bg .el-card,
html[data-theme='highContrast'] body.has-custom-bg .el-card {
  background: rgba(22, 27, 34, 0.78) !important;
  border-color: rgba(51, 65, 85, 0.5) !important;
}

/* ===== 表格 ===== */
body.has-custom-bg .el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  background: rgba(255, 255, 255, 0.75) !important;
}
body.has-custom-bg .el-table__header th {
  background: rgba(245, 247, 250, 0.9) !important;
}
body.has-custom-bg .el-table__body tr.el-table__row--striped td {
  background: rgba(245, 247, 250, 0.6) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-table,
html[data-theme='brand'] body.has-custom-bg .el-table,
html[data-theme='highContrast'] body.has-custom-bg .el-table {
  background: rgba(22, 27, 34, 0.72) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-table__header th,
html[data-theme='brand'] body.has-custom-bg .el-table__header th,
html[data-theme='highContrast'] body.has-custom-bg .el-table__header th {
  background: rgba(13, 17, 23, 0.88) !important;
}

/* ===== 对话框 / 抽屉 ===== */
body.has-custom-bg .el-dialog {
  background: rgba(255, 255, 255, 0.9) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-dialog,
html[data-theme='brand'] body.has-custom-bg .el-dialog,
html[data-theme='highContrast'] body.has-custom-bg .el-dialog {
  background: rgba(22, 27, 34, 0.9) !important;
}
body.has-custom-bg .uav-drawer .el-drawer,
body.has-custom-bg .uav-drawer .el-drawer__body {
  background: var(--color-sidebar-bg, #001529) !important;
}

/* ===== 分页 / 横幅 / 页面头 ===== */
body.has-custom-bg .el-pagination {
  background: transparent !important;
}
body.has-custom-bg .demo-banner {
  background: rgba(255, 255, 255, 0.8) !important;
}
html[data-theme='dark'] body.has-custom-bg .demo-banner,
html[data-theme='brand'] body.has-custom-bg .demo-banner {
  background: rgba(22, 27, 34, 0.78) !important;
}
body.has-custom-bg .page-header {
  background: rgba(255, 255, 255, 0.82) !important;
}
html[data-theme='dark'] body.has-custom-bg .page-header,
html[data-theme='brand'] body.has-custom-bg .page-header {
  background: rgba(22, 27, 34, 0.78) !important;
}

/* ===== 输入框 ===== */
body.has-custom-bg .el-input__wrapper {
  background: rgba(255, 255, 255, 0.65) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-input__wrapper,
html[data-theme='brand'] body.has-custom-bg .el-input__wrapper {
  background: rgba(30, 41, 59, 0.65) !important;
}

/* ===== 下拉菜单（精准选择器，不用 .el-popper） ===== */
body.has-custom-bg .el-dropdown-menu,
body.has-custom-bg .el-select-dropdown {
  background: rgba(255, 255, 255, 0.94) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-dropdown-menu,
html[data-theme='dark'] body.has-custom-bg .el-select-dropdown,
html[data-theme='brand'] body.has-custom-bg .el-dropdown-menu,
html[data-theme='brand'] body.has-custom-bg .el-select-dropdown {
  background: rgba(22, 27, 34, 0.94) !important;
}

/* ===== Element Plus 组件（多页面共用） ===== */
/* 标签页 */
body.has-custom-bg .el-tabs__header {
  background: rgba(255, 255, 255, 0.75) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-tabs__header,
html[data-theme='brand'] body.has-custom-bg .el-tabs__header,
html[data-theme='highContrast'] body.has-custom-bg .el-tabs__header {
  background: rgba(22, 27, 34, 0.7) !important;
}
/* 描述列表 */
body.has-custom-bg .el-descriptions {
  background: rgba(255, 255, 255, 0.8) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-descriptions,
html[data-theme='brand'] body.has-custom-bg .el-descriptions,
html[data-theme='highContrast'] body.has-custom-bg .el-descriptions {
  background: rgba(22, 27, 34, 0.75) !important;
}
/* 空状态 */
body.has-custom-bg .el-empty {
  background: transparent !important;
}
/* 警告提示 */
body.has-custom-bg .el-alert {
  background: rgba(255, 255, 255, 0.78) !important;
}
html[data-theme='dark'] body.has-custom-bg .el-alert,
html[data-theme='brand'] body.has-custom-bg .el-alert,
html[data-theme='highContrast'] body.has-custom-bg .el-alert {
  background: rgba(22, 27, 34, 0.72) !important;
}

/* ===== 页面级容器（*="..." 匹配多 class 元素） ===== */
body.has-custom-bg [class*="-view"]:not([class*="el-"]):not([class*="map"]),
body.has-custom-bg [class*="-page"]:not([class*="el-"]):not([class*="map"]),
body.has-custom-bg [class*="-container"]:not([class*="el-"]):not([class*="map"]) {
  background-color: transparent !important;
}
body.has-custom-bg [class*="-panel"]:not([class*="el-"]):not([class*="map"]),
body.has-custom-bg [class*="-list"]:not([class*="el-"]):not([class*="map"]),
body.has-custom-bg [class*="-grid"]:not([class*="el-"]):not([class*="map"]) {
  background-color: transparent !important;
}

/* ===== 自定义卡片类 ===== */
body.has-custom-bg [class*="-card"]:not([class*="el-"]):not([class*="map"]) {
  background: rgba(255, 255, 255, 0.8) !important;
}
html[data-theme='dark'] body.has-custom-bg [class*="-card"]:not([class*="el-"]):not([class*="map"]),
html[data-theme='brand'] body.has-custom-bg [class*="-card"]:not([class*="el-"]):not([class*="map"]),
html[data-theme='highContrast'] body.has-custom-bg [class*="-card"]:not([class*="el-"]):not([class*="map"]) {
  background: rgba(22, 27, 34, 0.75) !important;
}

/* ===== 信息/统计单元格 ===== */
body.has-custom-bg [class*="-cell"]:not([class*="el-"]):not([class*="map"]),
body.has-custom-bg [class*="-item"]:not([class*="el-"]):not([class*="map"]) {
  background-color: transparent !important;
}

/* ===== 控制台 / 日志面板 ===== */
body.has-custom-bg [class*="-console"]:not([class*="map"]),
body.has-custom-bg [class*="-preview"]:not([class*="map"]),
body.has-custom-bg [class*="-box"]:not([class*="map"]) {
  background: rgba(255, 255, 255, 0.75) !important;
}
html[data-theme='dark'] body.has-custom-bg [class*="-console"]:not([class*="map"]),
html[data-theme='dark'] body.has-custom-bg [class*="-preview"]:not([class*="map"]),
html[data-theme='dark'] body.has-custom-bg [class*="-box"]:not([class*="map"]),
html[data-theme='brand'] body.has-custom-bg [class*="-console"]:not([class*="map"]),
html[data-theme='brand'] body.has-custom-bg [class*="-preview"]:not([class*="map"]),
html[data-theme='brand'] body.has-custom-bg [class*="-box"]:not([class*="map"]) {
  background: rgba(22, 27, 34, 0.72) !important;
}

/* ===== 特殊页面（驾驶舱、任务报告、路径规划）半透明背景 ===== */
body.has-custom-bg .cockpit {
  background: rgba(10, 25, 41, 0.82) !important;
}
body.has-custom-bg .task-report {
  background: rgba(255, 255, 255, 0.78) !important;
}
html[data-theme='dark'] body.has-custom-bg .task-report,
html[data-theme='brand'] body.has-custom-bg .task-report,
html[data-theme='highContrast'] body.has-custom-bg .task-report {
  background: rgba(22, 27, 34, 0.75) !important;
}
body.has-custom-bg .path-planning-view {
  background: rgba(255, 255, 255, 0.78) !important;
}
html[data-theme='dark'] body.has-custom-bg .path-planning-view,
html[data-theme='brand'] body.has-custom-bg .path-planning-view,
html[data-theme='highContrast'] body.has-custom-bg .path-planning-view {
  background: rgba(22, 27, 34, 0.75) !important;
}

/* ===== 地图相关——保持不透明 ===== */
body.has-custom-bg .map-wrapper,
body.has-custom-bg .map-container,
body.has-custom-bg .map-legend,
body.has-custom-bg [class*="map-"] {
  background-color: initial !important;
  background: initial !important;
}
</style>

<style scoped>
/* 自定义背景 */
.bg-card {
  background: var(--color-surface, #fff);
  color: var(--color-text, #1f2937);
  border: 1px solid var(--color-border, #e5e7eb);
  margin-top: 16px;
}

.bg-upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 16px;
  border: 2px dashed var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
  background: color-mix(in srgb, var(--color-bg, #f5f7fa) 70%, transparent);
}

.bg-upload-area:hover {
  border-color: var(--color-primary, #409EFF);
  background: color-mix(in srgb, var(--color-primary, #409EFF) 8%, transparent);
}

.upload-placeholder-icon {
  color: var(--color-text-muted, #6b7280);
}

.upload-text {
  font-size: 14px;
  color: var(--color-text, #1f2937);
}

.upload-hint {
  font-size: 12px;
  color: var(--color-text-muted, #6b7280);
}

.bg-preview-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bg-preview-img {
  width: 100%;
  height: 120px;
  border-radius: var(--radius-md, 8px);
  background-size: cover;
  background-position: center;
  border: 1px solid var(--color-border, #e5e7eb);
}

.bg-preview-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.bg-size-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.bg-size-label {
  font-size: 14px;
  color: var(--color-text, #1f2937);
  min-width: 70px;
}
</style>
