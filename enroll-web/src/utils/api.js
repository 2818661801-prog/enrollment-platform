/**
 * API 调用封装（Vite 代理已配 /api → http://localhost:8080）
 *
 * ⚠️ 2026-07-02 重构原则（主人规则）：
 *   - 动数据库的接口 → POST（创建/修改/删除）
 *   - 查数据库的接口 → GET
 *   不再使用 PUT / DELETE / PATCH
 *
 * ⚠️ 2026-08-07 重构（Task 18）：
 *   - request() 自动从 Pinia store 注入 JWT，接口不再各自塞 header
 *   - fetchClasses() 去掉 mock 兜底（后端挂了该抛错，不该给人看假班级）
 *   - 删除废弃接口 fetchMyApplications()（改用 JWT 版 fetchMyApplicationsMe）
 *
 * 接口列表：
 *
 * 学生端：
 *   POST   /api/applications                — 提交报名
 *   POST   /api/applications/update         — 修改报名信息
 *   POST   /api/applications/withdraw       — 撤回报名
 *   POST   /api/applications/my-verify      — 密码查询我的报名
 *   GET    /api/applications/my?idCard=     — 按身份证查我的报名
 *   GET    /api/applications/me             — JWT 自动查我的报名
 *   GET    /api/classes                     — 查所有特色班
 *   GET    /api/classes/{id}                — 查单个班级
 *   GET    /api/classes?category=           — 按类别筛选
 *   GET    /api/categories                  — 类别列表
 *   GET    /api/config/notice               — 报名须知
 *
 * 认证：
 *   POST   /api/auth/send-code              — 发送手机验证码
 *   POST   /api/auth/login/sms              — 验证码登录
 *
 */

import { useAuthStore } from '../stores/auth.js'

/**
 * 通用 fetch 封装（自动带 JWT）
 * @param {string} url - API 路径（以 / 开头）
 * @param {object} options - fetch 选项（method/headers/body）
 * @returns {Promise<any>} 解析后的 JSON 数据
 */
async function request(url, options = {}) {
  // useAuthStore 必须在 Pinia 激活后调用（api.js 只在组件生命周期内被调用，此时已激活）
  const auth = useAuthStore()
  const headers = {
    'Content-Type': 'application/json',
    ...(auth.token ? { Authorization: `Bearer ${auth.token}` } : {}),
    ...(options.headers || {}),
  }
  const res = await fetch(url, { ...options, headers })
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}: ${res.statusText}`)
    err.status = res.status
    throw err
  }
  return res.json()
}

// ==================== 班级 API ====================

/** 获取所有特色班（返 data 数组，后端挂了抛错 → 页面显示加载失败，不再兜底假数据） */
export const fetchClasses = async () => {
  const res = await request('/api/classes')
  return res.data || []
}

/** 按 ID 获取单个班级（返 data 对象） */
export const fetchClass = async (id) => {
  const res = await request(`/api/classes/${id}`)
  return res.data
}

// ==================== 报名 API（学生端）====================

/**
 * 提交报名
 * @param {object} form - 表单数据
 */
export const submitApplicationAPI = (form) =>
  request('/api/applications', {
    method: 'POST',
    body: JSON.stringify(form),
  })

/** 撤回报名（2026-07-02：PUT → POST） */
export const withdrawApplicationAPI = (id) =>
  request('/api/applications/withdraw', {
    method: 'POST',
    body: JSON.stringify({ id }),
  })

/** 修改报名信息（2026-07-02：PUT → POST） */
export const updateApplicationAPI = (id, data) =>
  request('/api/applications/update', {
    method: 'POST',
    body: JSON.stringify({ id, ...data }),
  })

/** JWT 版我的报名（token 由 request() 自动注入，无需手动读 localStorage） */
export const fetchMyApplicationsMe = async () => {
  const res = await request('/api/applications/me')
  return res.code === 200 ? (res.data || []) : []
}

/** 密码查询我的报名 */
export const fetchMyApplicationsWithPwd = async (idCard, password) => {
  const res = await request('/api/applications/my-verify', {
    method: 'POST',
    body: JSON.stringify({ idCard, password }),
  })
  if (res.code !== 200) {
    const err = new Error(res.message || '查询失败')
    err.code = res.code
    throw err
  }
  return res.data || []
}

// ==================== 认证 API ====================

/** 学生端：发送手机验证码 */
export const sendCodeAPI = (phone) =>
  request('/api/auth/send-code', {
    method: 'POST',
    body: JSON.stringify({ phone }),
  })

/** 学生端：验证码登录 */
export const loginByCodeAPI = (phone, code) =>
  request('/api/auth/login/sms', {
    method: 'POST',
    body: JSON.stringify({ phone, code }),
  })

/** 学生端：获取类别列表（供表单下拉） */
export const fetchCategories = () => request('/api/categories')

// ==================== 公开 API（学生端）====================

/** 获取服务器当前年份（无则 fallback 本地）*/
export const fetchServerYear = async () => {
  try {
    const res = await request('/api/year')
    return res.year != null ? res.year : null
  } catch {
    return null
  }
}

/** 获取报名须知（学生端）*/
export const fetchNotice = async () => {
  try {
    const res = await request('/api/config/notice')
    return typeof res.data === 'string' ? JSON.parse(res.data) : res.data
  } catch {
    return null
  }
}

/**
 * 报名查重接口（GET）
 * @param {string} phone - 手机号
 * @param {string} idCard - 身份证号
 * @param {number} classId - 班级ID
 * @returns {Promise<{hasPhoneConflict, phoneClassName, hasIdCardConflict, idCardClassName, hasSameClassConflict}>}
 */
export const checkDuplicateAPI = async (phone, idCard, classId) => {
  const res = await request(
    `/api/applications/check?phone=${encodeURIComponent(phone)}&idCard=${encodeURIComponent(idCard)}&classId=${classId}`
  )
  return res.code === 200 ? res.data : null
}