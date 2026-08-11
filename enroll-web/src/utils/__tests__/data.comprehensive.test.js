import { describe, it, expect } from 'vitest'
import { parsePeriod, formatTime, getClassTimeStatus } from '../data.js'

// 辅助：格式化时间为 "yyyy/MM/dd HH:mm"
const fmt = (d) => {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth()+1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

describe('parsePeriod 边界测试', () => {
  it('完整格式含时分秒', () => {
    const { start, end } = parsePeriod('2026/09/01 08:00:00 - 2026/09/13 23:59:59')
    expect(start.getFullYear()).toBe(2026)
    expect(start.getMonth()).toBe(8)
    expect(start.getDate()).toBe(1)
    expect(start.getHours()).toBe(8)
    expect(end.getDate()).toBe(13)
    expect(end.getHours()).toBe(23)
    expect(end.getMinutes()).toBe(59)
  })

  it('日期格式无时间（默认 00:00 - 23:59:59）', () => {
    const { start, end } = parsePeriod('2026/09/01 - 2026/09/13')
    expect(start.getHours()).toBe(0)
    expect(start.getMinutes()).toBe(0)
    expect(end.getHours()).toBe(23)
    expect(end.getMinutes()).toBe(59)
  })

  it('结束日期无年份（从开始日期推断）', () => {
    const { start, end } = parsePeriod('2026/09/01 - 09/13')
    expect(end.getFullYear()).toBe(2026)
    expect(end.getMonth()).toBe(8)
    expect(end.getDate()).toBe(13)
  })

  it('跨年推断（12月→1月，结束年份+1）', () => {
    const { start, end } = parsePeriod('2026/12/20 - 01/05')
    expect(end.getFullYear()).toBe(2027)
    expect(end.getMonth()).toBe(0)
    expect(end.getDate()).toBe(5)
  })

  it('横线格式（ISO 日期）', () => {
    const { start, end } = parsePeriod('2026-09-01 - 2026-09-13')
    expect(start.getFullYear()).toBe(2026)
    expect(start.getMonth()).toBe(8)
    expect(end.getDate()).toBe(13)
  })

  it('空值/非法类型返回 epoch', () => {
    const r1 = parsePeriod(null)
    expect(r1.start.getTime()).toBe(0)
    const r2 = parsePeriod(undefined)
    expect(r2.start.getTime()).toBe(0)
    const r3 = parsePeriod(123)
    expect(r3.start.getTime()).toBe(0)
  })

  it('含时分（无秒）', () => {
    const { start, end } = parsePeriod('2026/09/15 08:00 - 2026/09/16 23:59')
    expect(start.getHours()).toBe(8)
    expect(start.getMinutes()).toBe(0)
    expect(end.getHours()).toBe(23)
    expect(end.getMinutes()).toBe(59)
  })
})

describe('formatTime 测试', () => {
  it('ISO 格式转 yyyy/MM/dd HH:mm', () => {
    expect(formatTime('2026-09-01T08:00:00')).toBe('2026-09-01 08:00')
  })

  it('含纳秒小数截断', () => {
    expect(formatTime('2026-09-01T08:00:00.999')).toBe('2026-09-01 08:00')
  })

  it('空值返回空字符串', () => {
    expect(formatTime(null)).toBe('')
    expect(formatTime('')).toBe('')
    expect(formatTime(undefined)).toBe('')
  })

  it('无 T 分隔符直接截秒', () => {
    expect(formatTime('2026-09-01 08:30:45')).toBe('2026-09-01 08:30')
  })
})

describe('getClassTimeStatus 边界测试', () => {
  it('报名时间窗口内 → canApply=true', () => {
    const now = new Date()
    const start = new Date(now.getTime() - 30 * 1000)
    const end = new Date(now.getTime() + 3600 * 1000)
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, now)
    expect(result.canApply).toBe(true)
    expect(result.status).toBe('open')
  })

  it('已超过截止时间 → canApply=false', () => {
    const now = new Date()
    const start = new Date(now.getTime() - 120 * 1000)
    const end = new Date(now.getTime() - 60 * 1000)
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, now)
    expect(result.canApply).toBe(false)
    expect(result.status).toBe('closed')
  })

  it('早于报名开始时间 → canApply=false', () => {
    const now = new Date()
    const start = new Date(now.getTime() + 3600 * 1000)
    const end = new Date(now.getTime() + 7200 * 1000)
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, now)
    expect(result.canApply).toBe(false)
    expect(result.status).toBe('not_started')
  })

  it('异常 period（epoch）→ canApply=false', () => {
    const result = getClassTimeStatus({}, null, new Date())
    expect(result.canApply).toBe(false)
    expect(result.status).toBe('closed')
  })

  it('精确边界：now === start → canApply=true', () => {
    const start = new Date(2026, 8, 1, 8, 0, 0)
    const end = new Date(2026, 8, 13, 23, 59, 59)
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, start)
    expect(result.canApply).toBe(true)
  })

  it('精确边界：now === end → canApply=true', () => {
    const start = new Date(2026, 8, 1, 8, 0, 0)
    const end = new Date(2026, 8, 13, 23, 59, 59)
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, end)
    expect(result.canApply).toBe(true)
  })
})
