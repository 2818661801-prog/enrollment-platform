/**
 * 数据文件（仅保留纯工具函数，不再含任何班级/报名数据）
 *
 * 班级数据全部从后端 API 拿（详见 src/utils/api.js）
 */

// ==================== 网络时间同步 ====================
// 用于解决浏览器本地时间可被篡改的问题
// 前端从后端 /api/time 拿服务器时间戳，计算"本地时间-服务器时间"偏差
// 然后在所有时间判断中用 trustedNow() 替代 new Date()

// ⚠️ 测试模式：true = 所有班级强制开放报名，测完改回 false
const isTestMode = false

let _serverTimeOffset = 0   // 本地时间 - 服务器时间的差值（毫秒）
let _lastFetchTime = 0      // 上次拉取时间戳

/**
 * 从后端拉取服务器时间，刷新偏差补偿量
 * 防频刷：距离上次拉取不足 30 秒直接返回（不发请求）
 * @returns {Promise<void>}
 */
export async function syncServerTime() {
  const now = Date.now()
  if (now - _lastFetchTime < 30_000) return  // 30秒防抖
  _lastFetchTime = now
  try {
    const resp = await fetch('/api/time')
    if (!resp.ok) throw new Error('网络响应异常')
    const data = await resp.json()
    const serverTs = Number(data.serverTime)
    if (!serverTs) throw new Error('时间戳无效')
    _serverTimeOffset = now - serverTs
  } catch (e) {
    console.warn('[data] 服务器时间同步失败，使用本地时间：', e)
    // 偏差归零，降级为本地时间
    _serverTimeOffset = 0
  }
}

/**
 * 获取"可信的当前时间"（经过服务器偏差校正）
 * 如果同步失败，降级为本地时间
 * @returns {Date}
 */
export function trustedNow() {
  return new Date(Date.now() - _serverTimeOffset)
}

/**
 * 学生申报表初始值（与移动端一致）
 * @returns {object} 空白报名表单
 */
export const initialForm = () => ({
  name: '',        // 姓名
  idCard: '',      // 身份证号
  gender: '',      // 性别（身份证自动推断，也可手动选）
  hasPhysics: '',  // 是否选考物理
  hasEnglish: '',  // 是否选考英语
  classId: null,   // 所选班级 ID
  appliedCategory: '',   // 班级类别（学生报名时选择）
})

/**
 * 解析班级报名时间段，返回开始/结束 Date 对象
 * period 格式：
 *   - "2026/09/01 - 09/13"（结束日期无年份）
 *   - "2026/09/01 - 2026/09/13"（结束日期完整）
 *   - "2026/09/15 08:00 - 2026/09/16 23:59"（结束日期带时分）
 *
 * 为什么这样设计：
 * ① 从 period 字符串提取起止日期 — 单一数据源，避免 startDate/endDate 不一致
 * ② 结束日期缺失年份时自动从开始日期推断（跨年场景：如 12/20 - 01/05 则结束年份+1）
 * ③ 结束日期带时分时，先去掉时间部分再解析
 *
 * @param {string} period - 如 "2026/09/01 - 09/13"
 * @returns {{ start: Date, end: Date }}
 */
export function parsePeriod(period) {
  const [startStr, endStr] = period.split(' - ')
  // 开始日期：去掉时间后缀，再按 / 分割（格式 YYYY/MM/DD 或 YYYY/MM/DD HH:MM）
  const startDateStr = startStr.split(' ')[0]  // "2026/09/15 08:00" → "2026/09/15"
  const [sYear, sMonth, sDay] = startDateStr.split('/').map(Number)

  // 结束日期：去掉时间后缀（如 "16 23:59" → "16"），再按 / 分割
  const endDateStr = endStr.split(' ')[0]  // "2026/09/16 23:59" → "2026/09/16"
  const endParts = endDateStr.split('/').map(Number)
  let eYear, eMonth, eDay
  if (endParts.length === 3) {
    ;[eYear, eMonth, eDay] = endParts
  } else {
    // 只有月/日，年份从开始日期推断
    ;[eMonth, eDay] = endParts
    eYear = sYear
    // 跨年判断：如 12月→1月，说明结束在下一年
    if (eMonth < sMonth) eYear++
  }
  return {
    start: new Date(sYear, sMonth - 1, sDay), // Date 月份从 0 开始
    end: new Date(eYear, eMonth - 1, eDay, 23, 59, 59), // 截止日当天 23:59:59
  }
}

/**
 * 判断班级当前报名状态
 *
 * 类比：就像奶茶店的营业时间——
 * ① 没到开门时间 → 显示"X月X日开放"（not_started）
 * ② 正在营业 → 可以下单（open）
 * ③ 打烊了 → 显示"已截止"（closed）
 *
 * @param {object} classInfo - 班级对象（含 period 字段）
 * @param {string} [periodStr] - 可选：直接传入时间字符串（如 "2026/09/01 - 2026/09/13"），用于多轮卡片按单轮判断
 * @param {Date} [now] - 当前时间（默认 new Date()，测试时可传 mock 时间）
 * @returns {{ status: 'not_started'|'open'|'closed', label: string, canApply: boolean }}
 */
export function getClassTimeStatus(classInfo, periodStr = null, now = null) {
  // 测试模式：强制所有班级开放
  if (isTestMode) {
    return { status: 'open', label: '测试模式', canApply: true }
  }

  const current = now || trustedNow() // 用经服务器校正的可信时间
  const period = periodStr || classInfo.period
  const { start, end } = parsePeriod(period)

  if (current < start) {
    const month = start.getMonth() + 1
    const day = start.getDate()
    return {
      status: 'not_started',
      label: `${month}月${day}日开放报名`,
      canApply: false,
    }
  }

  if (current > end) {
    return {
      status: 'closed',
      label: '已截止',
      canApply: false,
    }
  }

  const month = end.getMonth() + 1
  const day = end.getDate()
  return {
    status: 'open',
    label: `报名中（${month}月${day}日截止）`,
    canApply: true,
  }
}

/**
 * 根据当前时间从 classRounds 数组中算出"第几轮"
 * @param {Array} classRounds - 后端返回的 [{roundNum, periodStart, periodEnd}, ...]
 * @returns {number} 轮次号（1, 2, ...），无匹配或单轮返回 1
 */
export function getCurrentRound(classRounds) {
  if (!classRounds || !Array.isArray(classRounds) || classRounds.length === 0) return 1
  const now = new Date()
  for (const r of classRounds) {
    const start = parseDate(r.periodStart)
    const end = parseDate(r.periodEnd)
    if (now >= start && now <= end) return r.roundNum || 1
  }
  // 不在任何一轮时，返回最后一个轮次号
  return classRounds[classRounds.length - 1].roundNum || 1
}

function parseDate(val) {
  if (!val) return new Date(0)
  const d = new Date(val)
  return isNaN(d.getTime()) ? new Date(0) : d
}

/**
 * 格式化时间字符串（ISO "2026-09-01T08:00:00" → "2026/09/01 08:00"）
 * @param {string} val - ISO 格式时间
 * @returns {string} 格式化后的字符串
 */
export function formatTime(val) {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return val  // 不识别的格式直接返回原值
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}/${m}/${day} ${h}:${min}`
}

/**
 * @deprecated v2.2 已废弃，由后端 ClassRoundRepository.findCurrentRound() 计算轮次
 * 此函数保留仅供兼容，旧代码清理前不要删
 */
export function getCurrentRound_JSON(periodsJson) {
  if (!periodsJson) return 1
  try {
    const list = JSON.parse(periodsJson)
    if (!Array.isArray(list) || list.length === 0) return 1
    const now = new Date()
    for (const item of list) {
      const { start, end } = parsePeriod(item.period)
      if (now >= start && now <= end) return item.round || 1
    }
    return list[list.length - 1].round || 1
  } catch {
    return 1
  }
}
