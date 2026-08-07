/**
 * stores/auth.js · 学生端认证状态（Pinia store）
 *
 * 为什么用 Pinia 而不是"localStorage + window.dispatchEvent('login_changed')"：
 *   1. 单一数据源：token / phone / isLoggedIn 只存 store 一份，组件不再各自读 localStorage
 *   2. 响应式传播：login()/logout() 之后，所有引用该 store 的组件自动更新视图，
 *      不需要再手动派发 login_changed 事件 + 逐个 addEventListener
 *   3. 持久化：token 同时写 localStorage，刷新页面后 state 从 localStorage 恢复
 *
 * 现实类比：localStorage 像"写在纸上的会员卡号"，Pinia 像"会员系统"——
 * 登录 = 在系统里登记（store 响应式更新所有页面）；纸只是系统重启后找回记录用。
 */
import { defineStore } from 'pinia'

const TOKEN_KEY = 'student_token'
const PHONE_KEY = 'student_phone'

export const useAuthStore = defineStore('auth', {
  // 初始化时从 localStorage 恢复（刷新页面不丢登录态）
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    phone: localStorage.getItem(PHONE_KEY) || '',
  }),

  getters: {
    // 有 token 即视为已登录
    isLoggedIn: (state) => !!state.token,
  },

  actions: {
    /** 登录成功：写 store（响应式）+ 写 localStorage（持久化）+ 派发事件通知其他页面 */
    login(token, phone) {
      this.token = token
      this.phone = phone || ''
      localStorage.setItem(TOKEN_KEY, token)
      if (phone) localStorage.setItem(PHONE_KEY, phone)
      // 通知其他页面（FormPage 预填手机号 / HomePage 重刷已报名标记）
      window.dispatchEvent(new Event('login_changed'))
    },
    /** 退出登录：清 store + 清 localStorage + 派发事件 */
    logout() {
      this.token = ''
      this.phone = ''
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(PHONE_KEY)
      // 通知其他页面：登录态变化 + 报名数据需重刷
      window.dispatchEvent(new Event('login_changed'))
      window.dispatchEvent(new Event('application_changed'))
    },
  },
})

/** 跨标签页同步：另一标签页登录/退出后，本标签页 store 自动更新（在 main.js 调一次即可） */
export function syncAuthAcrossTabs() {
  window.addEventListener('storage', (e) => {
    if (e.key !== TOKEN_KEY && e.key !== PHONE_KEY) return
    const auth = useAuthStore()
    if (e.key === TOKEN_KEY) auth.token = e.newValue || ''
    if (e.key === PHONE_KEY) auth.phone = e.newValue || ''
  })
}
