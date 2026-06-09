<script setup>
import { ref, onErrorCaptured, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getUserFriendlyMessage } from '@/utils/errorTypes'

const router = useRouter()

const hasError = ref(false)
const errorMessage = ref('')
const errorDetail = ref('')

onErrorCaptured((err, instance, info) => {
  hasError.value = true
  errorMessage.value = getUserFriendlyMessage(err)
  errorDetail.value = err.stack || err.message || ''
  console.error('[ErrorBoundary] 捕获到组件错误:', {
    message: err.message,
    name: err.name,
    code: err.code,
    component: instance?.$options?.name || instance?.type?.name || 'unknown',
    info
  })
  // 返回 false 阻止错误继续向上传播
  return false
})

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''
  errorDetail.value = ''
}

function handleGoHome() {
  hasError.value = false
  errorMessage.value = ''
  errorDetail.value = ''
  router.push('/dashboard')
}

// 监听路由变化，自动清除错误状态
watch(
  () => router.currentRoute.value?.fullPath,
  () => {
    if (hasError.value) {
      hasError.value = false
      errorMessage.value = ''
      errorDetail.value = ''
    }
  }
)
</script>

<template>
  <slot v-if="!hasError" />
  <div v-else class="error-boundary">
    <el-result icon="error" :title="$t('errorBoundary.title')" :sub-title="errorMessage">
      <template #extra>
        <el-button type="primary" @click="handleRetry">
          {{ $t('errorBoundary.retry') }}
        </el-button>
        <el-button @click="handleGoHome">
          {{ $t('errorBoundary.goHome') }}
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 32px;
}
</style>
