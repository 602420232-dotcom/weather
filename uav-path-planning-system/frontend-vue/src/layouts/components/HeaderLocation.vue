<template>
  <div class="info-group">
    <div class="location-info" :class="{ 'is-clickable': !isLocating }" @click="$emit('fetch')">
      <el-icon :size="16" class="info-icon"><MapLocation /></el-icon>
      <span class="info-text">{{ locationText }}</span>
      <el-icon v-if="isLocating" :size="14" class="location-loading"><Loading /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { MapLocation, Loading } from '@element-plus/icons-vue'

defineProps({
  locationText: { type: String, default: '' },
  isLocating: { type: Boolean, default: false }
})

defineEmits(['fetch'])
</script>

<style scoped>
.info-group {
  display: flex;
  align-items: center;
}
.location-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.05);
  font-size: 13px;
  color: #666;
  transition: background 0.2s;
}
.location-info.is-clickable {
  cursor: pointer;
}
.location-info.is-clickable:hover {
  background: rgba(0, 0, 0, 0.08);
}
.info-icon {
  color: #409eff;
}
.info-text {
  white-space: nowrap;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.location-loading {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@media (prefers-reduced-motion: reduce) {
  .location-loading {
    animation: none;
  }
}
</style>
