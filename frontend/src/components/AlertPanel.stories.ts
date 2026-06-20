import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'

const meta: Meta = {
  title: 'Components/AlertPanel',
  argTypes: {
    severity: {
      control: { type: 'select' },
      options: ['INFO', 'WARNING', 'CRITICAL']
    }
  }
}

export default meta

const getColor = (severity: string) => {
  switch (severity) {
    case 'CRITICAL': return { bg: '#ffebee', border: '#ef5350', text: '#c62828' }
    case 'WARNING': return { bg: '#fff3e0', border: '#ff9800', text: '#e65100' }
    default: return { bg: '#e3f2fd', border: '#42a5f5', text: '#1565c0' }
  }
}

const defaultAlerts = [
  { severity: 'CRITICAL', message: '无人机 M300-01 电量低于 20%', time: '10:32:15' },
  { severity: 'WARNING', message: '区域 A 风速超过 12m/s', time: '10:30:00' },
  { severity: 'INFO', message: '任务 巡检-003 已完成', time: '10:25:30' },
]

const Template: StoryObj = (args: { alerts?: { severity: string; message: string; time?: string }[] }) => ({
  setup() {
    const alerts = ref(args.alerts || defaultAlerts)
    return { alerts, getColor }
  },
  template: `
    <div style="width: 100%; max-width: 400px; font-family: sans-serif;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <h3 style="margin: 0; font-size: 16px;">告警列表</h3>
        <span style="font-size: 12px; color: #666;">共 {{ alerts.length }} 条</span>
      </div>
      <div v-for="(alert, i) in alerts" :key="i"
        :style="{
          padding: '12px', marginBottom: '8px', borderRadius: '8px',
          border: '1px solid ' + getColor(alert.severity).border,
          background: getColor(alert.severity).bg
        }">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
          <span :style="{ fontSize: '13px', fontWeight: 600, color: getColor(alert.severity).text }">
            {{ alert.severity }}
          </span>
          <span style="font-size: 11px; color: #999;">{{ alert.time }}</span>
        </div>
        <div style="font-size: 13px; color: #333;">{{ alert.message }}</div>
      </div>
    </div>
  `
})

export const Default = Template.bind({})
Default.args = {}

export const AllCritical: StoryObj = Template.bind({})
AllCritical.args = {
  alerts: [
    { severity: 'CRITICAL', message: '无人机 M300-01 信号丢失', time: '10:35:00' },
    { severity: 'CRITICAL', message: '无人机 M300-02 电量低于 15%', time: '10:34:22' },
    { severity: 'CRITICAL', message: '区域 B 检测到禁飞区入侵', time: '10:33:45' },
  ]
}

export const Empty: StoryObj = Template.bind({})
Empty.args = { alerts: [] }
