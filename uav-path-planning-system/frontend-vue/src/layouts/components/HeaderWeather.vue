<template>
  <div class="info-group">
    <div class="weather-info">
      <span class="weather-icon">{{ weatherIcon }}</span>
      <span class="info-text">{{ weatherText }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  weatherIcon: { type: String, default: '☀️' },
  weatherText: { type: String, default: '' }
})
</script>

<style scoped>
.info-group {
  display: flex;
  align-items: center;
}
.weather-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.05);
  font-size: 13px;
  color: var(--color-text-muted);
}
.weather-icon {
  font-size: 16px;
}
.info-text {
  white-space: nowrap;
}
</style>
