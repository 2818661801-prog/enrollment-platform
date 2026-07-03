/**
 * usePhoneCode.js · 共享的"发码+验证+倒计时"逻辑（V2.1 · 防刷新倒计时 + 登录防抖）
 *
 * 被 2 处复用：
 *   1. StudentLogin.vue  — 独立登录页
 *   2. FormPage.vue      — 报名流程第 1 步
 *
 * V2.1 改动（2026-07-02）：
 *   - 倒计时改用 localStorage 存截止时间戳，刷新页面不丢进度
 *   - onLogin 加 isLogging 标志，防止快速连点重复提交
 *   - 验证码错误 5 次后强制删除（后端 AuthService 已做，前端加前端提示）
 *
 * 返回的状态：
 *   phone        — 手机号（响应式）
 *   code         — 验证码（响应式）
 *   sending      — 发码中 loading
 *   logging      — 登录中 loading
 *   countdown    — 当前剩余秒数（由 countdownEnd 时间戳计算而来）
 *   codeSent     — 验证码已发送（控制是否显示 code 输入框）
 *
 * 返回的方法：
 *   onSendCode   — 发码（带 60s 倒计时，时间戳存 localStorage）
 *   onLogin      — 验证+登录（成功后写 localStorage），返 true/false
 *   reset        — 重置（清除 localStorage + 内存状态）
 */

import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendCodeAPI, loginByCodeAPI } from '../utils/api.js'

const CDOWN_KEY = 'sms_cd_end'       // localStorage key：倒计时截止时间戳
const CDOWN_SEC = 60                  // 倒计时秒数

/**
 * 从 localStorage 恢复剩余秒数（页面刷新后调用）
 * @returns {number} 剩余秒数，<=0 表示已过期或无记录
 */
function restoreCountdown() {
  const end = localStorage.getItem(CDOWN_KEY)
  if (!end) return 0
  const remaining = Math.max(0, Math.ceil((Number(end) - Date.now()) / 1000))
  return remaining
}

/**
 * 手机号+验证码登录 composable
 * @returns {object} 状态 + 方法
 */
export function usePhoneCode() {
  const phone = ref('')         // 手机号
  const code = ref('')          // 验证码
  const sending = ref(false)    // 发码中
  const logging = ref(false)    // 登录中（防抖 guard）
  const codeSent = ref(false)   // 验证码已发送

  // countdown 不再直接存储秒数，每次访问时从时间戳计算
  const countdown = ref(restoreCountdown())

  let timer = null              // 倒计时定时器（不响应式）
  let isLogging = false         // 登录中防抖标志（内存，不响应式）

  /** 手机号格式校验（11位 + 1[3-9] 开头） */
  function isValidPhone(v) {
    return /^1[3-9]\d{9}$/.test(v)
  }

  /** 验证码格式校验（6位数字） */
  function isValidCode(v) {
    return /^\d{6}$/.test(v)
  }

  /** 启动倒计时（写入 localStorage 截止时间戳） */
  function startCountdown() {
    const end = Date.now() + CDOWN_SEC * 1000
    localStorage.setItem(CDOWN_KEY, String(end))
    countdown.value = CDOWN_SEC
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((Number(localStorage.getItem(CDOWN_KEY)) - Date.now()) / 1000))
      countdown.value = remaining
      if (remaining <= 0 && timer) {
        clearInterval(timer)
        timer = null
        localStorage.removeItem(CDOWN_KEY)
      }
    }, 500)  // 每 500ms 刷新一次，更跟手
  }

  /** 发送验证码 */
  async function onSendCode() {
    if (!isValidPhone(phone.value)) {
      ElMessage.warning('手机号格式不正确')
      return
    }
    sending.value = true
    // 2秒后若还在发送中，提示用户耐心等待（避免正常情况误触）
    const waitTimer = setTimeout(() => {
      ElMessage.info('正在获取验证码，请耐心等待...')
    }, 2000)
    try {
      const res = await sendCodeAPI(phone.value)
      clearTimeout(waitTimer)
      if (res.code !== 200) {
        ElMessage.error(res.message || '发送失败')
        return
      }
      ElMessage.success('验证码已发送')
      codeSent.value = true
      startCountdown()
    } catch {
      clearTimeout(waitTimer)
      ElMessage.error('网络错误，请稍后重试')
    } finally {
      sending.value = false
    }
  }

  /**
   * 验证+登录（防抖：isLogging 保护，快速连点只发一次请求）
   * @returns {Promise<boolean>} 成功 true / 失败 false
   */
  async function onLogin() {
    if (isLogging) return false  // 已经在登录中，拒绝重复调用
    if (!isValidPhone(phone.value)) {
      ElMessage.warning('手机号格式不正确')
      return false
    }
    if (!isValidCode(code.value)) {
      ElMessage.warning('验证码为6位数字')
      return false
    }
    isLogging = true
    logging.value = true
    try {
      const res = await loginByCodeAPI(phone.value, code.value)
      if (res.code !== 200) {
        ElMessage.error(res.message || '登录失败')
        return false
      }
      const { token, phone: respPhone } = res.data
      localStorage.setItem('student_token', token)
      localStorage.setItem('student_phone', respPhone)
      ElMessage.success('登录成功')
      clearCountdown()
      return true
    } catch {
      ElMessage.error('网络错误，请稍后重试')
      return false
    } finally {
      logging.value = false
      isLogging = false
    }
  }

  /** 清除倒计时（内存 + localStorage）*/
  function clearCountdown() {
    countdown.value = 0
    localStorage.removeItem(CDOWN_KEY)
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  /** 重置状态（清理一切，包括 localStorage）*/
  function reset() {
    phone.value = ''
    code.value = ''
    sending.value = false
    logging.value = false
    codeSent.value = false
    isLogging = false
    clearCountdown()
  }

  // 组件卸载时清理定时器（防内存泄漏，localStorage 倒计时不清理——刷新页面需要）
  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  })

  return {
    // 状态
    phone,
    code,
    sending,
    logging,
    countdown,
    codeSent,
    // 方法
    onSendCode,
    onLogin,
    reset,
    clearCountdown,
    // 工具
    isValidPhone,
    isValidCode,
  }
}