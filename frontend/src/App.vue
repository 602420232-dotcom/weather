<template>
  <el-config-provider :locale="elementLocale">
    <div class="app-container">
      <a 
        href="#main-content" 
        class="skip-link"
        :aria-label="t('app.skipToContent')"
      >
        {{ t('app.skipToContent') }}
      </a>
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

const { t, locale } = useI18n()

const elementLocale = computed(() => {
  return locale.value.startsWith('zh') ? zhCn : en
})
</script>

<style>
html,
body,
#app {
  margin: 0;
  padding: 0;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 深色模式背景 */
html[data-theme='dark'] body,
html[data-theme='dark'] #app,
html[data-theme='dark'] .app-container {
  background: var(--bg-primary, #0a0e1a) !important;
  color: var(--color-text) !important;
}

html[data-theme='dark'] {
  color-scheme: dark;
}

a {
  color: var(--color-primary, #1677ff);
  text-decoration: none;
}

html[data-theme='dark'] a {
  color: var(--color-primary);
}

.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #409eff;
  color: white;
  padding: 8px 16px;
  z-index: 1000;
  border-radius: 0 0 4px 4px;
  transition: top 0.2s;
}

.skip-link:focus {
  top: 0;
}

.app-container {
  min-height: 100vh;
}

html[data-theme='dark'] .app-container {
  background: var(--bg-primary);
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

*:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}
</style>
