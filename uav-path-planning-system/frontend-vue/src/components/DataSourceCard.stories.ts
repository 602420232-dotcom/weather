import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta = {
  title: 'Components/DataSourceCard',
  argTypes: {
    status: {
      control: { type: 'select' },
      options: ['online', 'offline', 'error']
    }
  }
}

export default meta

const Template = (args: Record<string, unknown>) => ({
  setup() { return { args } },
  template: `
    <div style="width: 240px; border: 1px solid #e0e0e0; border-radius: 12px; padding: 16px; font-family: sans-serif; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
      <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
        <div :style="{
          width: 40, height: 40, borderRadius: 8,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 20,
          background: args.status === 'online' ? '#e8f5e9' : args.status === 'offline' ? '#f5f5f5' : '#ffebee'
        }">{{ args.icon || '🌤' }}</div>
        <div>
          <div style="font-size: 14px; font-weight: 600;">{{ args.name || 'WRF 气象站' }}</div>
          <div :style="{
            fontSize: 12,
            color: args.status === 'online' ? '#2e7d32' : args.status === 'offline' ? '#999' : '#c62828'
          }">
            {{ args.status === 'online' ? '在线' : args.status === 'offline' ? '离线' : '异常' }}
          </div>
        </div>
      </div>
      <div style="font-size: 13px; color: #666; margin-bottom: 8px;">
        <span>最后更新: {{ args.lastUpdate || '2026-06-03 14:30' }}</span>
      </div>
      <div v-if="args.status === 'online'" style="font-size: 13px;">
        <div style="display: flex; justify-content: space-between; padding: 2px 0;">
          <span style="color: #666;">数据延迟</span><span>{{ args.latency || '< 1s' }}</span>
        </div>
        <div style="display: flex; justify-content: space-between; padding: 2px 0;">
          <span style="color: #666;">更新频率</span><span>{{ args.frequency || '5min' }}</span>
        </div>
      </div>
    </div>
  `
})

export const Online = Template.bind({})
Online.args = { status: 'online', name: 'WRF 数值预报', icon: '🌤', latency: '< 1s', frequency: '30min' }

export const RadarOnline = Template.bind({})
RadarOnline.args = { status: 'online', name: '天气雷达', icon: '📡', latency: '< 2s', frequency: '6min' }

export const Offline = Template.bind({})
Offline.args = { status: 'offline', name: '探空仪', icon: '🎈', lastUpdate: '2026-06-03 08:00' }

export const Error = Template.bind({})
Error.args = { status: 'error', name: '北斗 GNSS', icon: '🛰', lastUpdate: '2026-06-02 22:15' }
