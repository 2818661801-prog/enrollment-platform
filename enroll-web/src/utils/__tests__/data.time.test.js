import { describe, it, expect } from 'vitest'
import { getClassTimeStatus, parsePeriod } from '../data.js'

// 辅助：格式化时间为 "yyyy/MM/dd HH:mm:ss"
const fmt = (d) => {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth()+1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

describe('getClassTimeStatus 边界测试', () => {

  // 场景A：now 在窗口内（start <= now <= end）→ canApply: true
  it('报名时间窗口内 - 应 canApply=true', () => {
    const now = new Date()
    const start = new Date(now.getTime() - 30 * 1000)  // 30秒前
    const end = new Date(now.getTime() + 3600 * 1000)   // 1小时后
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, now)
    expect(result.canApply).toBe(true)
    expect(result.status).toBe('open')
  })

  // 场景C：now > end（截止时间已过）→ canApply: false
  it('已超过截止时间 - 应 canApply=false', () => {
    const now = new Date()
    const start = new Date(now.getTime() - 120 * 1000)  // 2分钟前
    const end = new Date(now.getTime() - 60 * 1000)    // 1分钟前（now > end）
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, now)
    expect(result.canApply).toBe(false)
    expect(result.status).toBe('closed')
  })

  // 场景B：now < start（报名尚未开始）→ canApply: false
  it('早于报名开始时间 - 应 canApply=false', () => {
    const now = new Date()
    const start = new Date(now.getTime() + 3600 * 1000)  // 1小时后
    const end = new Date(now.getTime() + 7200 * 1000)    // 2小时后
    const period = `${fmt(start)} - ${fmt(end)}`
    const result = getClassTimeStatus({}, period, now)
    expect(result.canApply).toBe(false)
    expect(result.status).toBe('not_started')
  })
})

describe('parsePeriod 解析测试', () => {
  it('解析完整格式（含时间）', () => {
    const { start, end } = parsePeriod('2026/09/01 08:00:00 - 2026/09/13 23:59:59')
    expect(start.getFullYear()).toBe(2026)
    expect(start.getMonth()).toBe(8)  // 0-indexed, 8 = September
    expect(start.getDate()).toBe(1)
    expect(start.getHours()).toBe(8)
    expect(end.getDate()).toBe(13)
    expect(end.getHours()).toBe(23)
    expect(end.getMinutes()).toBe(59)
  })

  it('解析日期格式（无时间，默认 00:00:00 - 23:59:59）', () => {
    const { start, end } = parsePeriod('2026/09/01 - 2026/09/13')
    expect(start.getHours()).toBe(0)
    expect(start.getMinutes()).toBe(0)
    expect(start.getSeconds()).toBe(0)
    expect(end.getHours()).toBe(23)
    expect(end.getMinutes()).toBe(59)
    expect(end.getSeconds()).toBe(59)
  })
})
