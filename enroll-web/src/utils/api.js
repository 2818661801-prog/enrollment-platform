/**
 * API 调用封装（Vite 代理已配 /api → http://localhost:8080）
 *
 * ⚠️ 2026-07-02 重构原则（主人规则）：
 *   - 动数据库的接口 → POST（创建/修改/删除）
 *   - 查数据库的接口 → GET
 *   不再使用 PUT / DELETE / PATCH
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
 *   POST   /api/auth/login                  — 管理员账号密码登录
 *   POST   /api/auth/send-code              — 发送手机验证码
 *   POST   /api/auth/login/sms              — 验证码登录
 *
 * 管理端（需 JWT，路径 /api/admin/**）：
 *   读（GET）：
 *     GET  /api/admin/applications         — 分页查询
 *     GET  /api/admin/applications/{id}    — 报名详情
 *     GET  /api/admin/classes              — 班级列表（含已删）
 *     GET  /api/admin/categories           — 类别列表
 *     GET  /api/admin/notice               — 报名须知
 *     GET  /api/admin/config/{key}         — 读 sys_config
 *   写（POST）：
 *     POST /api/admin/applications/admit         — 批量录取
 *     POST /api/admin/applications/reject        — 批量未录取
 *     POST /api/admin/applications/delete        — 批量软删
 *     POST /api/admin/applications/clear         — 清空某班报名
 *     POST /api/admin/classes                    — 新建班级
 *     POST /api/admin/classes/update             — 更新班级
 *     POST /api/admin/classes/update-period      — 仅改时间段
 *     POST /api/admin/classes/update-quota       — 仅改配额
 *     POST /api/admin/classes/delete             — 软删班级
 *     POST /api/admin/categories                 — 新建类别
 *     POST /api/admin/categories/update          — 更新类别
 *     POST /api/admin/categories/delete          — 删除类别
 *     POST /api/admin/notice/update              — 改报名须知
 *     POST /api/admin/config/set                 — 改 sys_config
 */

/**
 * 通用 fetch 封装（学生端，无 JWT）
 * @param {string} url - API 路径（以 / 开头）
 * @param {object} options - fetch 选项（method/headers/body）
 * @returns {Promise<any>} 解析后的 JSON 数据
 */
async function request(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}: ${res.statusText}`)
    err.status = res.status
    throw err
  }
  return res.json()
}

// ==================== 班级 API ====================

/** 获取所有特色班（返 data 数组） */
export const fetchClasses = async () => {
  try {
    const res = await request('/api/classes')
    return res.data || []
  } catch {
    // 后端未启动时用兜底 mock 数据
    return [
      { id: 1, name: '2026级拔尖创新人才实验班（杭电班）', period: '2026/01/01 - 2026/12/31', round: 0 },
      { id: 2, name: '2026级计算机科学与技术（成电联合培养）', period: '2026/01/01 - 2026/12/31', round: 1 },
      { id: 3, name: '2026级电子信息工程（成电联合培养）', period: '2026/01/01 - 2026/12/31', round: 1 },
      { id: 4, name: '2026级会计学ACCA班', period: '2026/01/01 - 2026/12/31', round: 0 },
      { id: 5, name: '2026级金融学CFA班', period: '2026/01/01 - 2026/12/31', round: 0 },
      { id: 6, name: '2026级会计学（智能财务）特色方向班', period: '2026/01/01 - 2026/12/31', round: 0 },
      { id: 7, name: '2026级湖畔实验班（计算机）', period: '2026/01/01 - 2026/12/31', round: 0 },
    ]
  }
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

/** 按身份证查我的报名（⚠️ S7 修复：此接口已废弃，改用 JWT 版本 fetchMyApplicationsMe） */
export const fetchMyApplications = async (idCard) => {
  // 兼容旧调用，但实际应该用 fetchMyApplicationsMe
  const token = localStorage.getItem('student_token')
  if (!token) return []
  const res = await fetch('/api/applications/my', {
    headers: { Authorization: `Bearer ${token}` },
  })
  const data = await res.json()
  return data.code === 200 ? (data.data || []) : []
}

/** JWT 版我的报名（⚠️ S8 修复：改用 httpOnly Cookie + credentials:include） */
export const fetchMyApplicationsMe = async () => {
  const res = await fetch('/api/applications/me', {
    credentials: 'include',  // 自动带上 httpOnly Cookie
  })
  const data = await res.json()
  return data.code === 200 ? (data.data || []) : []
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

/** 管理员账号密码登录 → 返 token */
export const adminLoginAPI = (username, password) =>
  request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })

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

// ==================== 管理端 API（需 JWT）====================

/** 通用带 JWT 的 fetch（⚠️ S8 修复：优先 httpOnly Cookie，credentials:include 自动带） */
async function adminRequest(url, options = {}) {
  const res = await fetch(url, {
    credentials: 'include',  // 自动带上 httpOnly Cookie
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    if (res.status === 401) {
      window.location.hash = '#/admin/login'
    }
    const err = new Error(`HTTP ${res.status}: ${res.statusText}`)
    err.status = res.status
    throw err
  }
  return res.json()
}

// ========== 读（GET） ==========

/** 管理员：分页查询报名 */
export const fetchAdminApplications = (params) => {
  const qs = new URLSearchParams(params).toString()
  return adminRequest(`/api/admin/applications?${qs}`)
}

/** 管理员：报名详情（2026-07-02 新增） */
export const fetchAdminApplication = (id) =>
  adminRequest(`/api/admin/applications/${id}`)

/** 管理员：班级列表 */
export const fetchAdminClasses = () => adminRequest('/api/admin/classes')

/** 管理员：读取 sys_config（单 key） */
export const fetchAdminConfig = (key) => adminRequest(`/api/admin/config/${key}`)

/** 管理员：读取报名须知（2026-07-02 新增显式 API） */
export const fetchAdminNotice = () => adminRequest('/api/admin/notice')

/** 管理员：类别列表（2026-07-02 GET 化） */
export const fetchAdminCategories = () => adminRequest('/api/admin/categories')

// ========== 写（POST） ==========

/** 管理员：批量录取（2026-07-02：PUT → POST） */
export const admitAdminApplications = (ids, auditComment = '') =>
  adminRequest('/api/admin/applications/admit', {
    method: 'POST',
    body: JSON.stringify({ ids, auditComment }),
  })

/** 管理员：批量未录取（2026-07-02：PUT → POST） */
export const rejectAdminApplications = (ids, auditComment = '') =>
  adminRequest('/api/admin/applications/reject', {
    method: 'POST',
    body: JSON.stringify({ ids, auditComment }),
  })

/** 管理员：批量撤回报名（2026-07-02：DELETE → POST） */
export const withdrawAdminApplications = (ids) =>
  adminRequest('/api/admin/applications/delete', {
    method: 'POST',
    body: JSON.stringify({ ids }),
  })

/** 管理员：清空班级报名（2026-07-02：DELETE → POST） */
export const clearAdminClass = (classId) =>
  adminRequest('/api/admin/applications/clear', {
    method: 'POST',
    body: JSON.stringify({ classId }),
  })

/** 管理员：新增班级 */
export const createAdminClass = (data) =>
  adminRequest('/api/admin/classes', { method: 'POST', body: JSON.stringify(data) })

/** 管理员：更新班级（2026-07-02：PUT → POST） */
export const updateAdminClass = (id, data) =>
  adminRequest('/api/admin/classes/update', {
    method: 'POST',
    body: JSON.stringify({ id, ...data }),
  })

/** 管理员：仅改时间段（2026-07-02：PUT → POST） */
export const updateAdminClassPeriod = (id, period) =>
  adminRequest('/api/admin/classes/update-period', {
    method: 'POST',
    body: JSON.stringify({ id, period }),
  })

/** 管理员：仅改配额（2026-07-02：PUT → POST） */
export const updateAdminClassQuota = (id, quota) =>
  adminRequest('/api/admin/classes/update-quota', {
    method: 'POST',
    body: JSON.stringify({ id, quota }),
  })

/** 管理员：软删班级（2026-07-02：DELETE → POST） */
export const deleteAdminClass = (id) =>
  adminRequest('/api/admin/classes/delete', {
    method: 'POST',
    body: JSON.stringify({ id }),
  })

/** 管理员：恢复已删除班级（复用 update） */
export const restoreAdminClass = (id) =>
  updateAdminClass(id, { isDeleted: 0 })

/** 管理员：改 sys_config（2026-07-02：PUT → POST） */
export const updateAdminConfig = (key, cfgValue, updatedBy) =>
  adminRequest('/api/admin/config/set', {
    method: 'POST',
    body: JSON.stringify({ key, cfgValue, updatedBy }),
  })

/** 管理员：改报名须知（2026-07-02 新增） */
export const updateAdminNotice = (notice) =>
  adminRequest('/api/admin/notice/update', {
    method: 'POST',
    body: JSON.stringify(notice),
  })

/** 管理员：新增类别（2026-07-02 新增） */
export const createAdminCategory = (name) =>
  adminRequest('/api/admin/categories', {
    method: 'POST',
    body: JSON.stringify({ name }),
  })

/** 管理员：更新类别（2026-07-02：PUT → POST） */
export const updateAdminCategory = (id, name) =>
  adminRequest('/api/admin/categories/update', {
    method: 'POST',
    body: JSON.stringify({ id, name }),
  })

/** 管理员：删除类别（2026-07-02：DELETE → POST） */
export const deleteAdminCategory = (id) =>
  adminRequest('/api/admin/categories/delete', {
    method: 'POST',
    body: JSON.stringify({ id }),
  })

/** 学生端：获取类别列表（供表单下拉） */
export const fetchCategories = () => request('/api/categories')

// ==================== 公开 API（学生端）====================

/** 获取报名须知（学生端）*/
export const fetchNotice = async () => {
  try {
    const res = await request('/api/config/notice')
    return typeof res.data === 'string' ? JSON.parse(res.data) : res.data
  } catch {
    return null
  }
}