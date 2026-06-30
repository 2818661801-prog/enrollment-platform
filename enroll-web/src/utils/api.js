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
      { id: 1, name: '2026级拔尖创新人才实验班（杭电班）', category: '经管类', period: '2026/01/01 - 2026/12/31', needPhysics: false },
      { id: 2, name: '2026级计算机科学与技术（成电联合培养）', category: '理工类', period: '2026/01/01 - 2026/12/31', needPhysics: true },
      { id: 3, name: '2026级电子信息工程（成电联合培养）', category: '理工类', period: '2026/01/01 - 2026/12/31', needPhysics: true },
      { id: 4, name: '2026级会计学ACCA班', category: '经管类', period: '2026/01/01 - 2026/12/31', needPhysics: false },
      { id: 5, name: '2026级金融学CFA班', category: '经管类', period: '2026/01/01 - 2026/12/31', needPhysics: false },
      { id: 6, name: '2026级会计学（智能财务）特色方向班', category: '经管类', period: '2026/01/01 - 2026/12/31', needPhysics: false },
      { id: 7, name: '2026级湖畔实验班（计算机）', category: '理工类', period: '2026/01/01 - 2026/12/31', needPhysics: true },
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
  request(`/api/applications/${id}`, { method: 'DELETE' })

/** 按身份证查我的报名（返 data 数组） */
export const fetchMyApplications = async (idCard) => {
  const res = await request(`/api/applications/my?idCard=${encodeURIComponent(idCard)}`)
  return res.data || []
}
