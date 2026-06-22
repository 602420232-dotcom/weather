import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta = {
  title: 'Components/MissionPlanCard',
  argTypes: {
    status: {
      control: { type: 'select' },
      options: ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED']
    }
  }
}

export default meta

const Template = (args: Record<string, unknown>) => ({
  setup() { return { args } },
  template: `
    <div style="width: 320px; border: 1px solid #e0e0e0; border-radius: 12px; padding: 16px; font-family: sans-serif; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <h3 style="margin: 0; font-size: 16px;">{{ args.name || '巡检任务-001' }}</h3>
        <span :style="{
          padding: '2px 8px', borderRadius: '4px', fontSize: '12px',
          background: args.status === 'COMPLETED' ? '#e8f5e9' : args.status === 'IN_PROGRESS' ? '#e3f2fd' : args.status === 'PENDING' ? '#fff3e0' : '#ffebee',
          color: args.status === 'COMPLETED' ? '#2e7d32' : args.status === 'IN_PROGRESS' ? '#1565c0' : args.status === 'PENDING' ? '#e65100' : '#c62828'
        }">{{ args.status || 'PENDING' }}</span>
      </div>
      <div style="font-size: 13px; color: #333; margin-bottom: 12px;">
        <div style="display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #f5f5f5;">
          <span style="color: #666;">无人机</span><span>{{ args.drone || 'M300-01' }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #f5f5f5;">
          <span style="color: #666;">任务点</span><span>{{ args.waypoints || 8 }} 个</span>
        </div>
        <div style="display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid #f5f5f5;">
          <span style="color: #666;">总距离</span><span>{{ args.distance || '12.5' }} km</span>
        </div>
        <div style="display: flex; justify-content: space-between; padding: 4px 0;">
          <span style="color: #666;">预计耗时</span><span>{{ args.duration || '45' }} min</span>
        </div>
      </div>
      <div v-if="args.status === 'IN_PROGRESS'" style="margin-bottom: 8px;">
        <div style="font-size: 12px; color: #666; margin-bottom: 4px;">进度</div>
        <div style="height: 6px; background: #eee; border-radius: 3px;">
          <div style="width: 60%; height: 100%; background: #1976d2; border-radius: 3px;"></div>
        </div>
      </div>
      <div v-if="args.showActions" style="display: flex; gap: 8px; margin-top: 12px;">
        <button style="flex: 1; padding: 6px; border: none; border-radius: 6px; background: #1976d2; color: white; cursor: pointer;">开始</button>
        <button style="flex: 1; padding: 6px; border: 1px solid #e0e0e0; border-radius: 6px; background: white; cursor: pointer;">详情</button>
      </div>
    </div>
  `
})

export const Pending = Template.bind({})
Pending.args = { status: 'PENDING', showActions: true, name: '线路巡检-003', waypoints: 12, distance: '18.2', duration: '60' }

export const InProgress = Template.bind({})
InProgress.args = { status: 'IN_PROGRESS', showActions: false, name: '应急巡查-001', waypoints: 5, distance: '8.5', duration: '30' }

export const Completed = Template.bind({})
Completed.args = { status: 'COMPLETED', showActions: false, name: '晨检任务-日常', waypoints: 10, distance: '15.0', duration: '50' }

export const Failed = Template.bind({})
Failed.args = { status: 'FAILED', showActions: true, name: 'Payload-003', waypoints: 6, distance: '22.0', duration: '75' }
