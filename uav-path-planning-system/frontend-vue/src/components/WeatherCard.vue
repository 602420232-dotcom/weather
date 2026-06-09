<template>
  <el-skeleton v-if="loading" animated :rows="1" style="width: 100%; padding: 16px" />
  <div
    v-else-if="error"
    class="weather-card weather-error"
    role="alert"
  >
    <el-icon color="#F56C6C" :size="16"><WarningFilled /></el-icon>
    <span class="error-text">{{ error }}</span>
    <el-button size="small" type="primary" plain @click="$emit('retry')">
      重试
    </el-button>
  </div>
  <div v-else class="weather-card" :class="status">
    <div class="weather-card-title">{{ title }}</div>
    <div class="weather-card-value">
      <span class="value">{{ value }}</span>
      <span class="unit">{{ unit }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'

interface Props {
  title: string
  value: number | string
  unit?: string
  status?: 'normal' | 'warning' | 'danger'
  loading?: boolean
  error?: string
}

withDefaults(defineProps<Props>(), {
  unit: '',
  status: 'normal',
  loading: false,
  error: ''
})

defineEmits<{
  retry: []
}>()
</script>

<style scoped>
.weather-card {
  padding: 16px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  text-align: center;
}
.weather-card.warning {
  border-left: 4px solid #faad14;
}
.weather-card.danger {
  border-left: 4px solid #ff4d4f;
}
.weather-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  border-left: 4px solid #F56C6C;
}
.weather-error .error-text {
  font-size: 12px;
  color: #F56C6C;
  text-align: center;
}
.weather-card-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}
.weather-card-value .value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}
.weather-card-value .unit {
  font-size: 14px;
  color: #999;
  margin-left: 4px;
}
</style>
