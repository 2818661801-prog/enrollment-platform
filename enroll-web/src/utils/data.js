/**
 * 数据文件（仅保留纯工具函数，不再含任何班级/报名数据）
 *
 * 班级数据全部从后端 API 拿（详见 src/utils/api.js）
 */

/**
 * 学生申报表初始值（与移动端一致）
 * @returns {object} 空白报名表单
 */
export const initialForm = () => ({
  name: '',        // 姓名
  idCard: '',      // 身份证号
  gender: '',      // 性别（身份证自动推断，也可手动选）
  phone: '',       // 联系电话
  hasPhysics: '',  // 是否选考物理
  hasEnglish: '',  // 是否选考英语
  classId: null,   // 所选班级 ID
  hdSubType: '',   // 杭电班子类别（仅 classId===1 时出现）
})

/**
 * 解析班级报名时间段，返回开始/结束 Date 对象
 * period 格式："2026/09/01 - 09/13" 或 "2026/08/15 - 09/16"
 *
 * 为什么这样设计：
 * ① 从 period 字符串提取起止日期 — 单一数据源，避免 startDate/endDate 不一致
 * ② 结束日期缺失年份时自动从开始日期推断（跨年场景：如 12/20 - 01/05 则结束年份+1）
 *
 * @param {string} period - 如 "2026/09/01 - 09/13"
 * @returns {{ start: Date, end: Date }}
 */
export function parsePeriod(period) {
  const [startStr, endStr] = period.split(' - ')
  // 开始日期：完整年月日
  const [sYear, sMonth, sDay] = startStr.split('/').map(Number)
  // 结束日期：可能缺少年份
  const endParts = endStr.split('/').map(Number)
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
 * @param {Date} [now] - 当前时间（默认 new Date()，测试时可传 mock 时间）
 * @returns {{ status: 'not_started'|'open'|'closed', label: string, canApply: boolean }}
 */
export function getClassTimeStatus(classInfo, now = null) {
  // ===== 测试开关 ===== 临时开放报名，测完改回 false
  const isTestMode = false
  if (isTestMode) {
    return { status: 'open', label: '报名中（测试模式）', canApply: true }
  }
  const current = now || new Date() // 用浏览器当前时间，不依赖网络
  const { start, end } = parsePeriod(classInfo.period)

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
