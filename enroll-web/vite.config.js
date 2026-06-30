import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：Vue3 插件 + 开发服务器
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,    // 开发服务器端口
    open: true,    // 自动打开浏览器
    proxy: {
      // 所有 /api 请求转发到后端 Spring Boot (localhost:8080)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
