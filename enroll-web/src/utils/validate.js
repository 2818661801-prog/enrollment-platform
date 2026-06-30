/**
 * 校验工具函数
 * 包含：身份证校验（含校验码）、手机号校验、姓名校验、性别推断
 */

/**
 * 身份证号校验（18 位 + 校验码验证）
 * @param {string} id - 身份证号
 * @returns {boolean} 是否合法
 *
 * 算法说明：
 * ① 前 17 位分别乘以权重 [7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2]
 * ② 求和后 mod 11 → 映射到 ['1','0','X','9','8','7','6','5','4','3','2']
 * ③ 与第 18 位比对
 */
export function validateIdCard(id) {
  // 格式检查：18 位数字，末位可为 X/x
  if (!/^\d{17}[\dXx]$/.test(id)) return false

  // 前 17 位权重
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  // 校验码映射表
  const codes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

  let sum = 0
  for (let i = 0; i < 17; i++) sum += parseInt(id[i]) * weights[i] // 加权求和

  // 取模比对
  return codes[sum % 11] === id[17].toUpperCase()
}

/**
 * 手机号校验（中国大陆）
 * @param {string} p - 手机号
 * @returns {boolean}
 */
export function validatePhone(p) {
  return /^1[3-9]\d{9}$/.test(p)
}

/**
 * 姓名校验（2-10 个中文字符）
 * @param {string} n - 姓名
 * @returns {boolean}
 */
export function validateName(n) {
  return /^[一-龥]{2,10}$/.test(n)
}

/**
 * 从身份证号推断性别
 * @param {string} id - 身份证号
 * @returns {string} '男' | '女' | ''
 *
 * 规则：第 17 位（倒数第 2 位）为奇数 → 男，偶数 → 女
 */
export function inferGender(id) {
  if (id.length >= 17) {
    return parseInt(id[16]) % 2 === 1 ? '男' : '女'
  }
  return ''
}
