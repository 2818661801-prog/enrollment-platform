/**
 * API 调用封装（Vite 代理已配 /api → http://localhost:8080）
 *
 * 接口列表：
 *   GET    /api/classes           — 查所有特色班
 *   GET    /api/classes/{id}      — 查单个班级
 *   GET    /api/classes?category= — 按类别筛选
 *   POST   /api/applications      — 提交报名
 *   GET    /api/applications/my?idCard= — 按身份证查我的报名
 *   DELETE /api/applications/{id}  — 撤回报名
 */

/**
 * 通用 fetch 封装
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
    // 401/403/500 等错误统一抛异常
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
    // 后端未启动时用兜底 mock 数据（时间全开放，供前端调试）
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

// ==================== 报名 API ====================

/**
 * 提交报名
 * @param {object} form - 表单数据
 * @returns {Promise<{success: boolean, message: string, id?: number}>}
 */
export const submitApplicationAPI = (form) =>
  request('/api/applications', {
    method: 'POST',
    body: JSON.stringify(form),
  })

/** 撤回报名 */
export const withdrawApplicationAPI = (id) =>
  request(`/api/applications/${id}/withdraw`, { method: 'PUT' })

/** 按身份证查我的报名（返 data 数组） */
export const fetchMyApplications = async (idCard) => {
  const res = await request(`/api/applications/my?idCard=${encodeURIComponent(idCard)}`)
  return res.data || []
}

// ==================== 认证 API ====================

/** 管理员账号密码登录 → 返 token */
export const adminLoginAPI = (username, password) =>
  request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })

// ==================== 管理端 API（需 JWT）====================

/** 通用带 JWT 的 fetch */
async function adminRequest(url, options = {}) {
  const token = localStorage.getItem('admin_token')
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...options,
  })
  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.hash = '#/admin/login'
    }
    const err = new Error(`HTTP ${res.status}: ${res.statusText}`)
    err.status = res.status
    throw err
  }
  return res.json()
}

/** 管理员：分页查询报名 */
export const fetchAdminApplications = (params) => {
  const qs = new URLSearchParams(params).toString()
  return adminRequest(`/api/admin/applications?${qs}`)
}

/** 管理员：批量删除报名 */
export const deleteAdminApplications = (ids) =>
  adminRequest(`/api/admin/applications/batch?ids=${ids.join(',')}`, { method: 'DELETE' })

/** 管理员：清空班级报名 */
export const clearAdminClass = (classId) =>
  adminRequest(`/api/admin/applications/clear?classId=${classId}`, { method: 'DELETE' })

/** 管理员：批量录取 */
export const admitAdminApplications = (ids) =>
  adminRequest(`/api/admin/applications/admit/batch?ids=${ids.join(',')}`, { method: 'PUT' })

/** 管理员：班级列表 */
export const fetchAdminClasses = () => adminRequest('/api/admin/classes')

/** 管理员：新增班级 */
export const createAdminClass = (data) =>
  adminRequest('/api/admin/classes', { method: 'POST', body: JSON.stringify(data) })

/** 管理员：更新班级 */
export const updateAdminClass = (id, data) =>
  adminRequest(`/api/admin/classes/${id}`, { method: 'PUT', body: JSON.stringify(data) })

/** 管理员：删除班级 */
export const deleteAdminClass = (id) =>
  adminRequest(`/api/admin/classes/${id}`, { method: 'DELETE' })

/** 管理员：读取配置 */
export const fetchAdminConfig = (key) => adminRequest(`/api/admin/config/${key}`)

/** 管理员：更新配置 */
export const updateAdminConfig = (key, cfgValue, updatedBy) =>
  adminRequest(`/api/admin/config/${key}`, {
    method: 'PUT',
    body: JSON.stringify({ cfgValue, updatedBy }),
  })

// ==================== 公开 API（学生端）====================

/** 获取报名须知（学生端）*/
export const fetchNotice = async () => {
  try {
    const res = await request('/api/config/notice')
    // res.data 是 JSON 字符串，需解析
    return typeof res.data === 'string' ? JSON.parse(res.data) : res.data
  } catch {
    return null
  }
}
