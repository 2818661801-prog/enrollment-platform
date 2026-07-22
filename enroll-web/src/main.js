import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import './assets/global.css'
import './styles/theme.css'

// 创建 Vue 应用 → 注册路由 → 注册 Element Plus（中文） → 挂载
const app = createApp(App)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  // 解决 ElMessage 被 AppHeader(9999) 遮挡，统一给所有 message 类组件提 zIndex
  message: { zIndex: 10000 },
}) // 中文本地化
app.mount('#app')
