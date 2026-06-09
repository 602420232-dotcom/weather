<template>
  <el-card class="gauge-panel" shadow="hover">
    <el-skeleton v-if="loading" animated>
      <template #template>
        <div style="text-align: center; padding: 16px">
          <el-skeleton-item variant="circle" style="width: 180px; height: 180px; margin: 0 auto" />
          <el-skeleton-item variant="text" style="width: 60%; margin: 12px auto 0" />
        </div>
      </template>
    </el-skeleton>
    <div v-else-if="error" class="stat-error">
      <el-icon color="#F56C6C" :size="18"><WarningFilled /></el-icon>
      <span class="error-msg">{{ error }}</span>
      <el-button size="small" type="primary" plain @click="$emit('retry')">
        {{ $t('common.refresh') }}
      </el-button>
    </div>
    <div v-else-if="empty" class="stat-empty">
      <el-empty :description="$t('common.noData')" :image-size="60" />
    </div>
    <div v-else class="gauge-content">
      <div class="gauge-label">{{ label }}</div>
      <el-progress type="dashboard" :percentage="innerPercent" :color="colorSet" :width="180" />
      <div class="gauge-meta">
        <div><span class="k">{{ $t('common.state') }}</span><span class="v">{{ value }}{{ unit }}</span></div>
        <div><span class="k">阈值</span><span class="v">{{ threshold }}{{ unit }}</span></div>
        <div><span class="k">均值</span><span class="v">{{ avg }}{{ unit }}</span></div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  label: { type: String, default: 'CPU 使用率' },
  value: { type: Number, default: 0 },
  max: { type: Number, default: 100 },
  threshold: { type: Number, default: 80 },
  avg: { type: Number, default: 40 },
  unit: { type: String, default: '%' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false }
})

defineEmits(['retry'])

const innerPercent = computed(() => Math.min(100, Math.round((props.value / props.max) * 100)))
const colorSet = computed(() => {
  if (props.value < 60) return [{ color: '#67C23A', percentage: 100 }]
  if (props.value < props.threshold) return [{ color: '#E6A23C', percentage: 100 }]
  return [{ color: '#F56C6C', percentage: 100 }]
})
</script>

<style scoped>
.gauge-panel { border-radius: 12px; text-align: center; }
.gauge-content { /* wrapper */ }
.gauge-label { font-size: 14px; color: var(--color-text-muted); margin-bottom: 8px; }
.gauge-meta { display: flex; justify-content: space-around; margin-top: 8px; font-size: 13px; }
.gauge-meta .k { color: var(--color-text-muted); margin-right: 4px; }
.gauge-meta .v { font-weight: 600; }
.stat-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 0;
}
.stat-error .error-msg {
  font-size: 13px;
  color: #F56C6C;
  text-align: center;
}
.stat-empty {
  display: flex;
  justify-content: center;
  padding: 18px 0;
}
</style>
