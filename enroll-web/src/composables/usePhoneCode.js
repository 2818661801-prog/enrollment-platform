/**
 * usePhoneCode.js · 共享的"发码+验证+倒计时"逻辑
 *
 * 被 2 处复用：
 *   1. StudentLogin.vue  — 独立登录页
 *   2. FormPage.vue      — 报名流程第 1 步
 *
 * 为什么不直接复用 StudentLogin 页？
 *   - StudentLogin 是独立路由（/student-login）
 *   - FormPage 是表单页内的子步骤（不跳页）
 *   - 业务逻辑一样，但交互位置不同
 *   - 抽 composable 共享逻辑，UI 各做各的
 *
 * 返回的状态：
 *   phone        — 手机号（响应式）
 *   code         — 验证码（响应式）
 *   sending      — 发码中 loading
 *   logging      — 登录中 loading
 *   countdown    — 60s 倒计时
 *   codeSent     — 验证码已发送（控制是否显示 code 输入框）
 *
 * 返回的方法：
 *   onSendCode   — 发码（带 60s 倒计时）
 *   onLogin      — 验证+登录（成功后写 localStorage），返 true/false
 *   reset        — 重置（页面卸载时调用，避免 timer 泄漏）
 */

import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { sendCodeAPI, loginByCodeAPI } from '../utils/api.js'

/**
 * 手机号+验证码登录 composable
 * @returns {object} 状态 + 方法
 */
export function usePhoneCode() {
  const phone = ref('')         // 手机号
  const code = ref('')          // 验证码
  const sending = ref(false)    // 发码中
  const logging = ref(false)    // 登录中
  const countdown = ref(0)      // 倒计时秒数（0=可发，>0=禁用）
  const codeSent = ref(false)   // 验证码已发送

  let timer = null              // 倒计时定时器（不响应式）

  /** 手机号格式校验（11位 + 1[3-9] 开头） */
  function isValidPhone(v) {
    return /^1[3-9]\d{9}$/.test(v)
  }

  /** 验证码格式校验（6位数字） */
  function isValidCode(v) {
    return /^\d{6}$/.test(v)
  }

  /** 发送验证码 */
  async function onSendCode() {
    // 前端先校验，避免无意义请求
    if (!isValidPhone(phone.value)) {
      ElMessage.warning('手机号格式不正确')
      return
    }
    sending.value = true
    try {
      const res = await sendCodeAPI(phone.value)
      if (res.code !== 200) {
        ElMessage.error(res.message || '发送失败')
        return
      }
      ElMessage.success('验证码已发送')
      codeSent.value = true
      // 启动 60s 倒计时（防刷）
      countdown.value = 60
      if (timer) clearInterval(timer)
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0 && timer) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    } catch {
      ElMessage.error('网络错误，请稍后重试')
    } finally {
      sending.value = false
    }
  }

  /**
   * 验证+登录
   * @returns {Promise<boolean>} 成功 true / 失败 false
   */
  async function onLogin() {
    if (!isValidPhone(phone.value)) {
      ElMessage.warning('手机号格式不正确')
      return false
    }
    if (!isValidCode(code.value)) {
      ElMessage.warning('验证码为6位数字')
      return false
    }
    logging.value = true
    try {
      const res = await loginByCodeAPI(phone.value, code.value)
      if (res.code !== 200) {
        ElMessage.error(res.message || '登录失败')
        return false
      }
      const { token, phone: respPhone } = res.data
      // 写 localStorage（与 StudentLogin 保持一致）
      localStorage.setItem('student_token', token)
      localStorage.setItem('student_phone', respPhone)
      ElMessage.success('登录成功')
      // 清理定时器
      if (timer) {
        clearInterval(timer)
        timer = null
      }
      return true
    } catch {
      ElMessage.error('网络错误，请稍后重试')
      return false
    } finally {
      logging.value = false
    }
  }

  /** 重置状态（页面卸载时调用，避免 timer 泄漏） */
  function reset() {
    phone.value = ''
    code.value = ''
    sending.value = false
    logging.value = false
    countdown.value = 0
    codeSent.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 组件卸载时自动清理定时器（防内存泄漏）
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
    // 工具
    isValidPhone,
    isValidCode,
  }
}