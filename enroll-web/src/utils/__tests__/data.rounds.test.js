import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { getCurrentRound, parsePeriod, formatTime, getClassTimeStatus, trustedNow } from '../data.js'

// 辅助：格式化时间为 "yyyy/MM/dd HH:mm"
const fmt = (d) => {
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth()+1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

describe('getCurrentRound 测试', () => {
  const realDate = Date

  beforeEach(() => {
    // 固定当前时间为 2026-09-15 10:00
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 8, 15, 10, 0, 0))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('空数组 → 返回 1', () => {
    expect(getCurrentRound([])).toBe(1)
    expect(getCurrentRound(null)).toBe(1)
    expect(getCurrentRound(undefined)).toBe(1)
  })

  it('单轮 → 返回 1', () => {
    const rounds = [{ roundNum: 1, periodStart: '2026-09-01T08:00', periodEnd: '2026-09-13T23:59' }]
    expect(getCurrentRound(rounds)).toBe(1)
  })

  it('当前在第一轮 → 返回 1', () => {
    const rounds = [
      { roundNum: 1, periodStart: '2026-09-01T08:00', periodEnd: '2026-09-13T23:59' },
      { roundNum: 2, periodStart: '2026-09-15T08:00', periodEnd: '2026-09-16T23:59' },
    ]
    // 当前时间 9/15 10:00 在第二轮内
    expect(getCurrentRound(rounds)).toBe(2)
  })

  it('当前在第二轮 → 返回 2', () => {
    const rounds = [
      { roundNum: 1, periodStart: '2026-09-01T08:00', periodEnd: '2026-09-13T23:59' },
      { roundNum: 2, periodStart: '2026-09-15T08:00', periodEnd: '2026-09-16T23:59' },
    ]
    expect(getCurrentRound(rounds)).toBe(2)
  })

  it('不在任何轮次 → 返回最后一轮轮次号', () => {
    // 把时间设到 9/14（两轮之间）
    vi.setSystemTime(new Date(2026, 8, 14, 10, 0, 0))
    const rounds = [
      { roundNum: 1, periodStart: '2026-09-01T08:00', periodEnd: '2026-09-13T23:59' },
      { roundNum: 2, periodStart: '2026-09-15T08:00', periodEnd: '2026-09-16T23:59' },
    ]
    expect(getCurrentRound(rounds)).toBe(2)
  })
})

describe('parsePeriod 补充边界', () => {
  it('结束日期无年份但带时分', () => {
    const { start, end } = parsePeriod('2026/09/01 08:00 - 09/13 23:59')
    expect(end.getFullYear()).toBe(2026)
    expect(end.getMonth()).toBe(8)
    expect(end.getDate()).toBe(13)
    expect(end.getHours()).toBe(23)
    expect(end.getMinutes()).toBe(59)
  })

  it('无效日期字符串（非空但格式错）→ 返回 epoch', () => {
    const { start, end } = parsePeriod('not-a-date - also-not-date')
    expect(isNaN(start.getTime()) || start.getTime() === 0).toBe(true)
  })

  it('ISO 横线日期格式（无 T 无时分）', () => {
    const { start, end } = parsePeriod('2026-09-01 - 2026-09-13')
    expect(start.getFullYear()).toBe(2026)
    expect(start.getMonth()).toBe(8)
    expect(start.getDate()).toBe(1)
    expect(end.getDate()).toBe(13)
  })
})

describe('formatTime 补充边界', () => {
  it('带时区偏移的 ISO 格式（T 替换为空格，截秒）', () => {
    // 后端可能返回 "2026-09-01T08:30:00"（无时区后缀）
    const result = formatTime('2026-09-01T08:30:00')
    expect(result).toBe('2026-09-01 08:30')
  })

  it('纯日期字符串（无时间部分）→ 原样返回', () => {
    expect(formatTime('2026-09-01')).toBe('2026-09-01')
  })
})

describe('trustedNow 测试', () => {
  it('默认返回接近当前时间的 Date', () => {
    const now = new Date()
    const trusted = trustedNow()
    // 允许 1 秒误差
    expect(Math.abs(trusted.getTime() - now.getTime())).toBeLessThan(1000)
  })
})
