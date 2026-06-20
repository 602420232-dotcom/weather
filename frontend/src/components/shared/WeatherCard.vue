<template>
  <el-card shadow="hover" class="weather-card">
    <el-skeleton v-if="loading" animated :rows="3" />
    <div v-else-if="error" class="weather-error" role="alert">
      <el-icon color="#F56C6C" :size="18"><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" type="primary" plain @click="$emit('retry')">
        重试
      </el-button>
    </div>
    <div v-else-if="empty" class="weather-empty">
      <el-empty :description="$t('common.noData')" :image-size="48" />
    </div>
    <div v-else>
      <div class="row">
        <div>
          <div class="title">{{ city }}</div>
          <div class="time">{{ timeText }}</div>
        </div>
        <el-icon :size="32" color="var(--color-primary)"><Sunny /></el-icon>
      </div>
      <div class="big-temp">{{ temperature }}<span>°C</span></div>
      <div class="metrics">
        <div><span>风向</span><strong>{{ windDir }}</strong></div>
        <div><span>风速</span><strong>{{ windSpeed }} m/s</strong></div>
        <div><span>气压</span><strong>{{ pressure }} hPa</strong></div>
        <div><span>湿度</span><strong>{{ humidity }}%</strong></div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { Sunny, WarningFilled } from '@element-plus/icons-vue'

defineProps({
  city: { type: String, default: '北京' },
  timeText: { type: String, default: '2026-06-09 12:00' },
  temperature: { type: Number, default: 24 },
  windDir: { type: String, default: '东南' },
  windSpeed: { type: Number, default: 4 },
  pressure: { type: Number, default: 1013 },
  humidity: { type: Number, default: 42 },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false }
})

defineEmits(['retry'])
</script>

<style scoped>
.weather-card { border-radius: 12px; }
.row { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 18px; font-weight: 600; }
.time { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; }
.big-temp { font-size: 40px; font-weight: 600; color: var(--color-primary); margin: 12px 0; }
.big-temp span { font-size: 18px; color: var(--color-text-muted); margin-left: 4px; }
.metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.metrics > div { display: flex; justify-content: space-between; font-size: 13px; padding: 6px 10px; background: rgba(0,0,0,0.03); border-radius: 6px; }
.metrics span { color: var(--color-text-muted); }
.weather-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  color: #F56C6C;
  font-size: 13px;
}
.weather-empty {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
</style>
