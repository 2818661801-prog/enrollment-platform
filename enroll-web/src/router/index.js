import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

/**
 * 路由配置
 *
 * 学生端：
 *   /home            — 首页
 *   /form            — 报名表单
 *   /form/:classId   — 直达某班表单
 *   /my-applications — 我的报名（JWT 自动查询，已登录直接显示）
 *   /student-login   — 学生手机验证码登录
 *
 * ⚠️ 守卫范围（Task 19）：只拦 /my-applications，不拦 /form ——
 *    FormPage 的登录发生在提交表单过程中（先填表后登录），硬守卫会阻断正常流程。
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
  // 每次导航都重置滚动位置，防止从其他页返回时继承旧滚动高度
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

// 全局前置守卫：未登录访问"我的报名" → 跳登录页，登录后回到原页面
router.beforeEach((to) => {
  if (to.path === '/my-applications') {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      // query 里的 redirect 让 StudentLogin 登录成功后跳回来
      return { path: '/student-login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router
