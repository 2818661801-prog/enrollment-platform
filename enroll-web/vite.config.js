import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// Vite 配置：Vue3 插件 + 开发服务器
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  assetsInclude: ['**/*.svg'],
  server: {
    port: 5173,    // 开发服务器端口
    open: true,    // 自动打开浏览器
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // timeout: 10000,  // 超时断开，避免请求挂起
      },
    },
  },
  test: {
    globals: true,
    environment: 'node',  // data.js 是纯工具函数，用 node 环境即可
  },
})