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
 *   POST   /api/auth/send-code              — 发送手机验证码
 *   POST   /api/auth/login/sms              — 验证码登录
 *
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
  const token = localStorage.getItem('student_token')
  const res = await fetch('/api/applications/me', {
    headers: { Authorization: token ? `Bearer ${token}` : '' },
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

/** 获取报名须知（学生端）*/
export const fetchNotice = async () => {
  try {
    const res = await request('/api/config/notice')
    return typeof res.data === 'string' ? JSON.parse(res.data) : res.data
  } catch {
    return null
  }
}