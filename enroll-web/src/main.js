import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { createPinia } from 'pinia'
import { syncAuthAcrossTabs } from './stores/auth.js'
import './assets/global.css'
import './styles/theme.css'

// 创建 Vue 应用 → 注册路由 → 注册 Pinia（必须先于组件用 store） → Element Plus（中文） → 挂载
const app = createApp(App)
app.use(createPinia())                    // Pinia 必须先于组件使用 store
app.use(router)
app.use(ElementPlus, { locale: zhCn }) // 中文本地化
syncAuthAcrossTabs()                      // 跨标签页登录态同步（只注册一次）
app.mount('#app')
