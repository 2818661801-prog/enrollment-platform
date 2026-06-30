import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 路由配置（hash 模式 · 问卷式）
 *
 * 学生端仅 2 个路由：
 *   /home      — 首页（班级浏览 + 报名须知弹窗）
 *   /form      — 报名表单
 *   /form/:id  — 直达某班表单
 *
 * 管理端已移除 — 主人直接看数据库
 * 「我的报名」已移除 — 问卷式收集，学生不需查状态
 */
const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/HomePage.vue'),
  },
  {
    path: '/form',
    name: 'Form',
    component: () => import('../views/FormPage.vue'),
  },
  {
    path: '/form/:classId', // 携带班级 ID
    name: 'FormDirect',
    component: () => import('../views/FormPage.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(), // hash 模式
  routes,
})

export default router
