<template>
  <el-card class="stat-card" shadow="hover" :body-style="{ padding: '16px' }">
    <el-skeleton v-if="loading" animated :rows="2" />
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
    <div v-else class="stat-content">
      <div class="stat-row">
        <div class="stat-label">{{ label }}</div>
        <el-tag :type="tagType" effect="light">{{ tagText }}</el-tag>
      </div>
      <div class="stat-value" :style="{ color: valueColor }">{{ formattedValue }}</div>
      <div class="stat-desc">{{ description }}</div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  label: { type: String, default: '指标' },
  value: { type: [Number, String], default: 0 },
  unit: { type: String, default: '' },
  tagText: { type: String, default: '实时' },
  tagType: { type: String, default: 'success' },
  description: { type: String, default: '' },
  valueColor: { type: String, default: '#1f2937' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false }
})

defineEmits(['retry'])

const formattedValue = computed(() => `${props.value}${props.unit}`)
</script>

<style scoped>
.stat-card { border-radius: 12px; }
.stat-content { /* wrapper */ }
.stat-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.stat-label { font-size: 14px; color: var(--color-text-muted); }
.stat-value { font-size: 28px; font-weight: 600; margin: 8px 0; }
.stat-desc { font-size: 12px; color: var(--color-text-muted); }
.stat-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
}
.stat-error .error-msg {
  font-size: 13px;
  color: #F56C6C;
  text-align: center;
}
.stat-empty {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
</style>
