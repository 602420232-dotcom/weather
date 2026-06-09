<template>
  <div class="info-group">
    <div class="time-info">
      <el-icon :size="16" class="info-icon"><Clock /></el-icon>
      <span class="info-text">{{ currentTime }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { Clock } from '@element-plus/icons-vue'

const { locale } = useI18n()

const currentTime = ref('')
let timeInterval = null

function getWeekdayLabel(date) {
  const weekdayMap = {
    'zh-CN': ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
    'en-US': ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
    'ja-JP': ['日', '月', '火', '水', '木', '金', '土']
  }
  const currentLocale = locale.value || 'zh-CN'
  return (weekdayMap[currentLocale] || weekdayMap['zh-CN'])[date.getDay()]
}

function updateTime() {
  const now = new Date()
  const hours = now.getHours().toString().padStart(2, '0')
  const minutes = now.getMinutes().toString().padStart(2, '0')
  const year = now.getFullYear()
  const month = (now.getMonth() + 1).toString().padStart(2, '0')
  const day = now.getDate().toString().padStart(2, '0')
  const weekday = getWeekdayLabel(now)
  currentTime.value = `${year}-${month}-${day} ${weekday} ${hours}:${minutes}`
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 30000)
})

onBeforeUnmount(() => {
  if (timeInterval) clearInterval(timeInterval)
})
</script>

<style scoped>
.info-group {
  display: flex;
  align-items: center;
}
.time-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.05);
  font-size: 13px;
  color: #666;
}
.info-icon {
  color: #409eff;
}
.info-text {
  white-space: nowrap;
}
</style>
