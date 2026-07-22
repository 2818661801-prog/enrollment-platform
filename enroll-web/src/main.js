import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import './assets/global.css'
import './styles/theme.css'

// 全局 ElMessage zIndex 统一设 10000（高于 AppHeader 的 9999，解决移动端被遮挡问题）
ElMessage.config({ zIndex: 10000 })

// 创建 Vue 应用 → 注册路由 → 注册 Element Plus（中文） → 挂载
const app = createApp(App)
app.use(router)
app.use(ElementPlus, { locale: zhCn }) // 中文本地化
app.mount('#app')
