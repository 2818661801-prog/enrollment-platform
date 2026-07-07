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
 *
 * 管理端（需重新登录）：
 *   /admin/login     — 管理员登录
 *   /admin           — 管理后台
 */
const routes = [
  // 学生端
  { path: '/', redirect: '/home' },
  { path: '/home',              name: 'Home',             component: () => import('../views/HomePage.vue') },
  { path: '/form',              name: 'Form',             component: () => import('../views/FormPage.vue') },
  { path: '/form/:classId',     name: 'FormDirect',       component: () => import('../views/FormPage.vue') },
  { path: '/my-applications',   name: 'MyApplications',   component: () => import('../views/MyApplications.vue') },
  { path: '/student-login',     name: 'StudentLogin',      component: () => import('../views/StudentLogin.vue') },
  // 管理端
  { path: '/admin/login',       name: 'AdminLogin',       component: () => import('../views/AdminLogin.vue') },
  { path: '/admin',             name: 'AdminDashboard',   component: () => import('../views/AdminDashboard.vue') },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

// 路由守卫：管理端页面需 admin JWT（⚠️ S9 修复：加 role=admin 校验）
router.beforeEach((to, from) => {
  const isAdminRoute = to.path.startsWith('/admin')
  if (!isAdminRoute) return true

  // /admin/login 不需要 token，直接放行（避免死循环）
  if (to.path === '/admin/login') return true

  const token = localStorage.getItem('admin_token')
  if (!token) {
    return '/admin/login'
  }

  // 解析 JWT payload（base64），校验 role=admin
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    if (payload.role !== 'admin') {
      localStorage.removeItem('admin_token')
      return '/admin/login'
    }
  } catch {
    localStorage.removeItem('admin_token')
    return '/admin/login'
  }

  return true
})

export default router
