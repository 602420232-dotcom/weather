import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  build: {
    // 构建优化
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },
    // 代码分割
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          ui: ['ant-design-vue'],
          chart: ['echarts'],
          map: ['leaflet'],
          http: ['axios']
        }
      }
    },
    // 缓存
    cacheDir: './node_modules/.vite-cache'
  },
  // 开发环境优化
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios', 'leaflet', 'echarts', 'ant-design-vue'],
    exclude: []
  }
})