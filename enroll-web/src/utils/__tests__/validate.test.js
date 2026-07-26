import { describe, it, expect } from 'vitest'
import { validateIdCard, validatePhone, validateName, inferGender } from '../validate.js'

describe('validateName', () => {
  it('合法姓名 - 2个中文', () => { expect(validateName('张三')).toBe(true) })
  it('合法姓名 - 10个中文', () => { expect(validateName('张张张张张张张张张张')).toBe(true) })
  it('非法 - 1个字符', () => { expect(validateName('张')).toBe(false) })
  it('非法 - 空字符串', () => { expect(validateName('')).toBe(false) })
  it('非法 - 含数字', () => { expect(validateName('张3三')).toBe(false) })
  it('非法 - 含英文字母', () => { expect(validateName('ZhangSan')).toBe(false) })
  it('非法 - 含标点', () => { expect(validateName('张·华')).toBe(false) })
})

describe('validatePhone', () => {
  it('合法 - 139开头', () => { expect(validatePhone('***REMOVED***')).toBe(true) })
  it('合法 - 188开头', () => { expect(validatePhone('***REMOVED***')).toBe(true) })
  it('非法 - 10位', () => { expect(validatePhone('1390000111')).toBe(false) })
  it('非法 - 12位', () => { expect(validatePhone('***REMOVED***2')).toBe(false) })
  it('非法 - 第2位是2', () => { expect(validatePhone('22900001111')).toBe(false) })
  it('非法 - 第2位是0', () => { expect(validatePhone('10000001111')).toBe(false) })
  it('非法 - 含字母', () => { expect(validatePhone('1390000111a')).toBe(false) })
  it('非法 - 空字符串', () => { expect(validatePhone('')).toBe(false) })
})

describe('validateIdCard', () => {
  // 有效身份证（算法验证通过的真实校验码）
  it('合法 - 标准18位校验码对', () => { expect(validateIdCard('110101***REMOVED***7')).toBe(true) })
  it('合法 - 末位X大写', () => { expect(validateIdCard('110101***REMOVED***X')).toBe(true) })
  it('合法 - 末位x小写', () => { expect(validateIdCard('110101***REMOVED***x')).toBe(true) })
  it('非法 - 17位', () => { expect(validateIdCard('110101***REMOVED***')).toBe(false) })
  it('非法 - 19位', () => { expect(validateIdCard('110101***REMOVED***34')).toBe(false) })
  it('非法 - 校验码错', () => { expect(validateIdCard('110101***REMOVED***3')).toBe(false) })
  it('非法 - 前17位含字母', () => { expect(validateIdCard('11010119900101A234')).toBe(false) })
  it('非法 - 空字符串', () => { expect(validateIdCard('')).toBe(false) })
})

describe('inferGender', () => {
  it('奇数第17位 - 男', () => { expect(inferGender('110101***REMOVED***4')).toBe('男') })
  it('偶数第17位 - 女', () => { expect(inferGender('110101***REMOVED***5')).toBe('女') })
  it('不足17位 - 空字符串', () => { expect(inferGender('123456')).toBe('') })
})
