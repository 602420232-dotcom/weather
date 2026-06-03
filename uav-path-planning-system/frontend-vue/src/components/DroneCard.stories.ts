import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta = {
  title: 'Components/DroneCard',
  argTypes: {
    status: {
      control: { type: 'select' },
      options: ['IDLE', 'BUSY', 'MAINTENANCE', 'FAILED']
    },
    batteryLevel: {
      control: { type: 'range', min: 0, max: 100 }
    }
  }
}

export default meta

const Template = (args: Record<string, unknown>) => ({
  components: {},
  setup() { return { args } },
  template: `
    <div style="width: 280px; border: 1px solid #e0e0e0; border-radius: 12px; padding: 16px; font-family: sans-serif; box-shadow: 0 2px 8px rgba(0,0,0,0.08);">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <h3 style="margin: 0; font-size: 16px;">{{ args.name || 'DJI M300 RTK' }}</h3>
        <span :style="{
          padding: '2px 8px', borderRadius: '4px', fontSize: '12px',
          background: args.status === 'IDLE' ? '#e8f5e9' : args.status === 'BUSY' ? '#fff3e0' : '#ffebee',
          color: args.status === 'IDLE' ? '#2e7d32' : args.status === 'BUSY' ? '#e65100' : '#c62828'
        }">{{ args.status || 'IDLE' }}</span>
      </div>
      <div style="margin-bottom: 8px;">
        <div style="font-size: 12px; color: #666;">电量</div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <div style="flex: 1; height: 6px; background: #eee; border-radius: 3px;">
            <div :style="{
              width: (args.batteryLevel || 85) + '%', height: '100%',
              background: (args.batteryLevel || 85) > 30 ? '#4caf50' : '#f44336',
              borderRadius: '3px', transition: 'width 0.3s'
            }"></div>
          </div>
          <span style="font-size: 14px; font-weight: 600;">{{ args.batteryLevel || 85 }}%</span>
        </div>
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 13px;">
        <div><span style="color: #666;">速度</span><br/>{{ args.speed || '12.5' }} m/s</div>
        <div><span style="color: #666;">高度</span><br/>{{ args.altitude || '120' }} m</div>
      </div>
    </div>
  `
})

export const Idle = Template.bind({})
Idle.args = { status: 'IDLE', batteryLevel: 95, name: 'DJI M300 RTK', speed: '0.0', altitude: '0' }

export const Busy = Template.bind({})
Busy.args = { status: 'BUSY', batteryLevel: 62, name: 'DJI M300 RTK', speed: '12.5', altitude: '120' }

export const LowBattery = Template.bind({})
LowBattery.args = { status: 'BUSY', batteryLevel: 18, name: 'DJI M300 RTK', speed: '8.3', altitude: '80' }

export const Maintenance = Template.bind({})
Maintenance.args = { status: 'MAINTENANCE', batteryLevel: 0, name: 'DJI M300 RTK', speed: '0.0', altitude: '0' }
