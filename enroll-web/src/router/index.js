import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 路由配置
 *
 * 学生端：
 *   /home            — 首页
 *   /form            — 报名表单
 *   /form/:classId   — 直达某班表单
 *   /my-applications — 我的报名（JWT 自动查询，已登录直接显示）
 *   /student-login   — 学生手机验证码登录
 */
const routes = [
  // 学生端
  { path: '/', redirect: '/home' },
  { path: '/home',              name: 'Home',             component: () => import('../views/HomePage.vue') },
  { path: '/form',              name: 'Form',             component: () => import('../views/FormPage.vue') },
  { path: '/form/:classId',     name: 'FormDirect',       component: () => import('../views/FormPage.vue') },
  { path: '/my-applications',   name: 'MyApplications',   component: () => import('../views/MyApplications.vue') },
  { path: '/student-login',     name: 'StudentLogin',      component: () => import('../views/StudentLogin.vue') },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router
