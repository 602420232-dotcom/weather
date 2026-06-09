<template>
  <div class="weather-source-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>🌦️ {{ t('weatherSource.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">📡</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.total }}</div>
              <div class="stat-label">{{ t('weatherSource.totalSources') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-online">
          <div class="stat-content">
            <div class="stat-icon">✅</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.online }}</div>
              <div class="stat-label">{{ t('weatherSource.onlineSources') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-offline">
          <div class="stat-content">
            <div class="stat-icon">⚠️</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.offline }}</div>
              <div class="stat-label">{{ t('weatherSource.offlineSources') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon">📊</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.uptime }}%</div>
              <div class="stat-label">{{ t('weatherSource.systemUptime') }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 类型筛选 -->
    <el-card class="filter-card">
      <div class="type-filters">
        <el-radio-group v-model="selectedType" @change="handleTypeChange">
          <el-radio-button value="">
            全部 ({{ stats.total }})
          </el-radio-button>
          <el-radio-button 
            v-for="(count, type) in stats.byType" 
            :key="type"
            :value="type"
          >
            {{ DATA_SOURCE_ICONS[type] }} {{ getTypeLabel(type) }} ({{ count.total }})
          </el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <!-- 数据源列表 -->
    <div class="source-list">
      <el-row :gutter="20">
        <el-col 
          v-for="source in filteredSources" 
          :key="source.id" 
          :span="8"
          class="source-col"
        >
          <el-card 
            shadow="hover" 
            class="source-card"
            :class="{ 'source-offline': source.status === 'offline' }"
          >
            <!-- 卡片头部 -->
            <template #header>
              <div class="card-header">
                <div class="source-title">
                  <span class="source-icon">{{ DATA_SOURCE_ICONS[source.type] }}</span>
                  <span class="source-name">{{ source.name }}</span>
                </div>
                <el-tag 
                  :type="source.status === 'online' ? 'success' : 'danger'"
                  size="small"
                >
                  {{ source.status === 'online' ? '● 在线' : '○ 离线' }}
                </el-tag>
              </div>
            </template>

            <!-- 卡片内容 -->
            <div class="source-body">
              <!-- 位置信息 -->
              <div class="source-location">
                <el-icon><Location /></el-icon>
                {{ source.location }}
                <span v-if="source.coordinates.lat" class="coordinates">
                  ({{ source.coordinates.lat.toFixed(2) }}, {{ source.coordinates.lng.toFixed(2) }})
                </span>
              </div>

              <!-- 最后更新时间 -->
              <div class="source-update">
                <el-icon><Clock /></el-icon>
                最后更新: {{ formatTime(source.lastUpdate) }}
              </div>

              <!-- 实时数据预览 -->
              <div v-if="source.latestData" class="data-preview">
                <div class="preview-title">实时数据</div>
                <div class="preview-grid">
                  <div 
                    v-for="field in getDisplayFields(source)" 
                    :key="field.key"
                    class="preview-item"
                  >
                    <span class="field-label">{{ field.label }}:</span>
                    <span class="field-value">
                      {{ formatFieldValue(source.latestData[field.key], field.unit) }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-else class="no-data">
                暂无数据
              </div>

              <!-- 数据质量指标 -->
              <div v-if="source.status === 'online'" class="quality-metrics">
                <div class="metric">
                  <span class="metric-label">完整率</span>
                  <el-progress 
                    :percentage="source.dataQuality.completeness" 
                    :stroke-width="8"
                    :color="getProgressColor(source.dataQuality.completeness)"
                  />
                </div>
                <div class="metric">
                  <span class="metric-label">延迟</span>
                  <span class="metric-value">{{ source.dataQuality.latency }}s</span>
                </div>
              </div>
            </div>

            <!-- 卡片底部操作 -->
            <template #footer>
              <div class="card-footer">
                <el-button size="small" @click="showDetail(source)">
                  <el-icon><View /></el-icon>
                  详情
                </el-button>
                <el-button 
                  size="small" 
                  :type="source.status === 'online' ? 'warning' : 'success'"
                  @click="toggleSource(source)"
                >
                  <el-icon v-if="source.status === 'online'"><VideoPause /></el-icon>
                  <el-icon v-else><VideoPlay /></el-icon>
                  {{ source.status === 'online' ? '停用' : '启用' }}
                </el-button>
                <el-button size="small" @click="showConfig(source)">
                  <el-icon><Setting /></el-icon>
                  配置
                </el-button>
              </div>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 数据源详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="selectedSource?.name"
      width="700px"
      destroy-on-close
    >
      <div v-if="selectedSource" class="detail-content">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="数据源ID">{{ selectedSource.id }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ DATA_SOURCE_ICONS[selectedSource.type] }} {{ getTypeLabel(selectedSource.type) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedSource.status === 'online' ? 'success' : 'danger'">
              {{ selectedSource.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="运行时长">
            {{ selectedSource.uptime }}%
          </el-descriptions-item>
          <el-descriptions-item label="位置" :span="2">
            {{ selectedSource.location }}
            <span v-if="selectedSource.coordinates.lat">
              ({{ selectedSource.coordinates.lat.toFixed(4) }}, {{ selectedSource.coordinates.lng.toFixed(4) }})
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 实时数据 -->
        <div v-if="selectedSource.latestData" class="detail-section">
          <h4>📊 实时数据</h4>
          <el-descriptions :column="3" border>
            <el-descriptions-item 
              v-for="field in getDataFields(selectedSource.type)" 
              :key="field.key"
              :label="field.label"
            >
              {{ formatFieldValue(selectedSource.latestData[field.key], field.unit) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 数据质量 -->
        <div v-if="selectedSource.status === 'online'" class="detail-section">
          <h4>📈 数据质量</h4>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="quality-card">
                <div class="quality-value">{{ selectedSource.dataQuality.completeness }}%</div>
                <div class="quality-label">数据完整率</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="quality-card">
                <div class="quality-value">{{ selectedSource.dataQuality.latency }}s</div>
                <div class="quality-label">平均延迟</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="quality-card">
                <div class="quality-value">{{ selectedSource.dataQuality.errorRate }}%</div>
                <div class="quality-label">错误率</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 连接配置 -->
        <div class="detail-section">
          <h4>⚙️ 连接配置</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="端点">
              <code>{{ selectedSource.config.endpoint }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="协议">
              <el-tag size="small">{{ selectedSource.config.protocol }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="采集间隔">
              {{ selectedSource.config.interval / 1000 }}秒
            </el-descriptions-item>
            <el-descriptions-item label="最后更新">
              {{ formatTime(selectedSource.lastUpdate) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="showConfig(selectedSource)">修改配置</el-button>
      </template>
    </el-dialog>

    <!-- 配置编辑弹窗 -->
    <el-dialog
      v-model="configVisible"
      title="修改配置"
      width="500px"
      destroy-on-close
    >
      <el-form v-if="editConfig" :model="editConfig" label-width="100px">
        <el-form-item label="端点地址">
          <el-input v-model="editConfig.endpoint" placeholder="请输入端点地址" />
        </el-form-item>
        <el-form-item label="采集间隔">
          <el-input-number 
            v-model="editConfig.interval" 
            :min="5000" 
            :step="5000"
            controls-position="right"
          />
          <span class="form-tip">毫秒</span>
        </el-form-item>
        <el-form-item label="协议">
          <el-select v-model="editConfig.protocol">
            <el-option label="HTTP" value="HTTP" />
            <el-option label="MQTT" value="MQTT" />
            <el-option label="WebSocket" value="WebSocket" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh, Location, Clock, View, Setting, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import weatherSourceApi, { DATA_SOURCE_TYPES, DATA_SOURCE_LABELS, DATA_SOURCE_ICONS } from '@/api/weatherSource'

const { t } = useI18n()

const sources = ref([])
const stats = ref({
  total: 0,
  online: 0,
  offline: 0,
  uptime: 0,
  byType: {}
})
const selectedType = ref('')
const detailVisible = ref(false)
const configVisible = ref(false)
const selectedSource = ref(null)
const editConfig = ref(null)

// 筛选后的数据源
const filteredSources = computed(() => {
  if (!selectedType.value) return sources.value
  return sources.value.filter(s => s.type === selectedType.value)
})

// 获取类型标签
const getTypeLabel = (type) => {
  return DATA_SOURCE_LABELS[type] || type
}

// 获取显示字段（最多显示4个）
const getDisplayFields = (source) => {
  const fields = weatherSourceApi.getDataFields(source.type)
  return fields.slice(0, 4)
}

// 获取所有数据字段
const getDataFields = (type) => {
  return weatherSourceApi.getDataFields(type)
}

// 格式化字段值
const formatFieldValue = (value, unit) => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'number') {
    return unit ? `${value.toFixed(1)}${unit}` : value.toString()
  }
  return unit ? `${value}${unit}` : value
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  
  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 95) return '#67c23a'
  if (percentage >= 80) return '#e6a23c'
  return '#f56c6c'
}

// 加载数据
const loadData = async () => {
  try {
    const [sourceData, statsData] = await Promise.all([
      weatherSourceApi.getAllSources(),
      weatherSourceApi.getStats()
    ])
    sources.value = sourceData
    stats.value = statsData
  } catch (error) {
    ElMessage.error('加载数据失败：' + error.message)
  }
}

// 刷新数据
const refreshData = async () => {
  await loadData()
  if (sources.value.length > 0) {
    ElMessage.success('数据已刷新')
  }
}

// 类型筛选变化
const handleTypeChange = () => {
  // 筛选逻辑由计算属性处理
}

// 显示详情
const showDetail = (source) => {
  selectedSource.value = source
  detailVisible.value = true
}

// 显示配置
const showConfig = (source) => {
  selectedSource.value = source
  editConfig.value = { ...source.config }
  detailVisible.value = false
  configVisible.value = true
}

// 保存配置
const saveConfig = async () => {
  try {
    await weatherSourceApi.updateConfig(selectedSource.value.id, editConfig.value)
    ElMessage.success('配置保存成功')
    configVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('配置保存失败：' + error.message)
  }
}

// 切换数据源状态
const toggleSource = async (source) => {
  try {
    await weatherSourceApi.toggleSource(source.id)
    ElMessage.success(`数据源已${source.status === 'online' ? '停用' : '启用'}`)
    loadData()
  } catch (error) {
    ElMessage.error('操作失败：' + error.message)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.weather-source-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-card.stat-online {
  border-left: 4px solid #67c23a;
}

.stat-card.stat-offline {
  border-left: 4px solid #f56c6c;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  font-size: 32px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.filter-card {
  margin-bottom: 20px;
}

.type-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.source-list {
  margin-top: 20px;
}

.source-col {
  margin-bottom: 20px;
}

.source-card {
  height: 100%;
  transition: all 0.3s;
}

.source-card:hover {
  transform: translateY(-2px);
}

.source-card.source-offline {
  opacity: 0.7;
}

.source-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.source-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.source-icon {
  font-size: 20px;
}

.source-name {
  font-weight: 600;
  font-size: 15px;
}

.source-body {
  padding: 10px 0;
}

.source-location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.coordinates {
  color: #909399;
  font-size: 12px;
}

.source-update {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 12px;
  margin-bottom: 12px;
}

.data-preview {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.preview-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.preview-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.field-label {
  color: #606266;
}

.field-value {
  font-weight: 500;
  color: #303133;
}

.no-data {
  text-align: center;
  color: #909399;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 6px;
}

.quality-metrics {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-label {
  width: 50px;
  font-size: 12px;
  color: #909399;
}

.metric-value {
  font-size: 14px;
  font-weight: 500;
  color: #409eff;
}

.card-footer {
  display: flex;
  gap: 8px;
}

.detail-content {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-section {
  margin-top: 20px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #303133;
}

.quality-card {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.quality-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.quality-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
}

@media (max-width: 768px) {
  .source-col {
    :deep(.el-col) {
      width: 100%;
    }
  }
  
  .stats-row {
    :deep(.el-col) {
      margin-bottom: 12px;
    }
  }
}

/* ===== 暗色模式适配 ===== */
.is-dark .weather-source-container {
  background: var(--color-bg);
}

.is-dark .page-header h2 {
  color: var(--color-text);
}

.is-dark .stat-value {
  color: var(--color-text);
}

.is-dark .stat-label {
  color: var(--color-text-muted);
}

.is-dark .filter-card {
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-dark .source-card {
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.08);
}

.is-dark .source-card:hover {
  border-color: var(--color-primary);
}

.is-dark .source-name {
  color: var(--color-text);
}

.is-dark .source-location {
  color: var(--color-text-muted);
}

.is-dark .coordinates {
  color: var(--color-text-muted);
}

.is-dark .source-update {
  color: var(--color-text-muted);
}

.is-dark .data-preview {
  background: rgba(255, 255, 255, 0.05);
}

.is-dark .preview-title {
  color: var(--color-text-muted);
}

.is-dark .field-label {
  color: var(--color-text-muted);
}

.is-dark .field-value {
  color: var(--color-text);
}

.is-dark .no-data {
  background: rgba(255, 255, 255, 0.05);
  color: var(--color-text-muted);
}

.is-dark .metric-label {
  color: var(--color-text-muted);
}

.is-dark .detail-section h4 {
  color: var(--color-text);
}

.is-dark .quality-card {
  background: rgba(255, 255, 255, 0.05);
}

.is-dark .quality-label {
  color: var(--color-text-muted);
}
</style>