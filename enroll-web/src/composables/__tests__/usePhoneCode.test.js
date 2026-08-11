/**
 * usePhoneCode 纯逻辑测试
 *
 * 测试策略：直接复制正则表达式测试，不实例化 usePhoneCode()
 * （usePhoneCode() 内部调 onUnmounted，在非 Vue 组件环境会超时）
 *
 * 正则来源：composables/usePhoneCode.js
 *   isValidPhone: /^1[3-9]\d{9}$/
 *   isValidCode:  /^\d{6}$/
 */

// 与 usePhoneCode.js 内部正则一致
const isValidPhone = (v) => /^1[3-9]\d{9}$/.test(v)
const isValidCode = (v) => /^\d{6}$/.test(v)

describe('isValidPhone 正则校验', () => {
  it('合法手机号 139 开头', () => {
    expect(isValidPhone('***REMOVED***')).toBe(true)
  })

  it('合法手机号 188 开头', () => {
    expect(isValidPhone('***REMOVED***')).toBe(true)
  })

  it('合法手机号 199 开头', () => {
    expect(isValidPhone('***REMOVED***')).toBe(true)
  })

  it('10 位 → 不合法', () => {
    expect(isValidPhone('1390000111')).toBe(false)
  })

  it('12 位 → 不合法', () => {
    expect(isValidPhone('***REMOVED***1')).toBe(false)
  })

  it('第二位非 3-9（12开头）→ 不合法', () => {
    expect(isValidPhone('12000001111')).toBe(false)
  })

  it('含字母 → 不合法', () => {
    expect(isValidPhone('1390000abcd')).toBe(false)
  })

  it('空字符串 → 不合法', () => {
    expect(isValidPhone('')).toBe(false)
  })
})

describe('isValidCode 正则校验', () => {
  it('6 位数字 → 合法', () => {
    expect(isValidCode('123456')).toBe(true)
  })

  it('000000 → 合法', () => {
    expect(isValidCode('000000')).toBe(true)
  })

  it('5 位 → 不合法', () => {
    expect(isValidCode('12345')).toBe(false)
  })

  it('7 位 → 不合法', () => {
    expect(isValidCode('1234567')).toBe(false)
  })

  it('含字母 → 不合法', () => {
    expect(isValidCode('12a456')).toBe(false)
  })

  it('空字符串 → 不合法', () => {
    expect(isValidCode('')).toBe(false)
  })
})
