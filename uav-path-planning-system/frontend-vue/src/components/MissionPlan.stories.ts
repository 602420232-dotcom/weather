import type { Meta, StoryObj } from '@storybook/vue3'

interface MissionPlanProps {
  missionName: string
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
  totalDistance: number
  totalTime: number
  taskCount: number
  droneCount: number
  riskScore: number
}

const MissionPlan = (args: MissionPlanProps) => ({
  template: `
    <div style="
      border: 1px solid #d9d9d9;
      border-radius: 8px;
      padding: 16px;
      max-width: 360px;
      background: #fff;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    ">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <span style="font-size: 16px; font-weight: 600;">{{ missionName }}</span>
        <span :style="{
          padding: '2px 8px',
          borderRadius: '4px',
          fontSize: '12px',
          fontWeight: 500,
          background: statusBg,
          color: statusColor
        }">{{ statusText }}</span>
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 13px;">
        <div><span style="color: #999;">距离:</span> {{ totalDistance }} km</div>
        <div><span style="color: #999;">时长:</span> {{ totalTime }} min</div>
        <div><span style="color: #999;">任务数:</span> {{ taskCount }}</div>
        <div><span style="color: #999;">无人机:</span> {{ droneCount }}</div>
      </div>
      <div style="margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0;">
        <div style="display: flex; align-items: center; gap: 8px;">
          <span style="font-size: 13px; color: #999;">风险评分:</span>
          <div style="flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden;">
            <div :style="{ width: riskScore + '%', height: '100%', background: riskColor, borderRadius: '4px' }"></div>
          </div>
          <span :style="{ fontSize: '13px', fontWeight: 500, color: riskColor }">{{ riskScore }}</span>
        </div>
      </div>
    </div>
  `,
  computed: {
    statusText() {
      return { PENDING: '待执行', IN_PROGRESS: '执行中', COMPLETED: '已完成', FAILED: '失败' }[this.status]
    },
    statusBg() {
      return { PENDING: '#f6ffed', IN_PROGRESS: '#e6f7ff', COMPLETED: '#f6ffed', FAILED: '#fff2f0' }[this.status]
    },
    statusColor() {
      return { PENDING: '#52c41a', IN_PROGRESS: '#1890ff', COMPLETED: '#52c41a', FAILED: '#ff4d4f' }[this.status]
    },
    riskColor() {
      return this.riskScore > 70 ? '#ff4d4f' : this.riskScore > 40 ? '#fa8c16' : '#52c41a'
    }
  },
  props: {
    missionName: String,
    status: String,
    totalDistance: Number,
    totalTime: Number,
    taskCount: Number,
    droneCount: Number,
    riskScore: Number
  }
})

const meta: Meta<typeof MissionPlan> = {
  title: 'Components/MissionPlan',
  component: MissionPlan as any,
  argTypes: {
    status: { control: 'select', options: ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'] },
    riskScore: { control: { type: 'range', min: 0, max: 100 } }
  }
}

export default meta
type Story = StoryObj<typeof MissionPlan>

export const Pending: Story = {
  args: { missionName: '日常巡检任务-01', status: 'PENDING', totalDistance: 15.6, totalTime: 45, taskCount: 8, droneCount: 2, riskScore: 25 }
}

export const InProgress: Story = {
  args: { missionName: '应急勘察-03', status: 'IN_PROGRESS', totalDistance: 8.2, totalTime: 30, taskCount: 5, droneCount: 1, riskScore: 55 }
}

export const Completed: Story = {
  args: { missionName: '植保作业-07', status: 'COMPLETED', totalDistance: 120.0, totalTime: 180, taskCount: 24, droneCount: 4, riskScore: 15 }
}

export const HighRisk: Story = {
  args: { missionName: '台风巡查-01', status: 'IN_PROGRESS', totalDistance: 25.0, totalTime: 90, taskCount: 12, droneCount: 3, riskScore: 82 }
}
