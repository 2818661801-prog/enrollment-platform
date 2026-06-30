import { ref } from 'vue'
import { submitApplicationAPI, withdrawApplicationAPI } from '../utils/api.js'

/**
 * 报名状态管理 composable（学生端）
 *
 * 重构后：
 *   - 调用真实后端 API（不再用 localStorage mock）
 *   - 适配后端 ApiResponse 格式 {code, message, data}
 *   - 失败抛 BusinessException（后端），前端 catch 后用 ElMessage 显示
 *
 * 适用场景：问卷式报名，学生扫码填表就完事
 *   - 提交：FormPage.vue 调 submitApplication
 *   - 撤回：当前已删"我的报名"页，预留以备后用
 */

// 单例状态：报名列表（问卷式场景下基本用不到，保留以备扩展）
const applications = ref([])

export function useApplication() {
  /**
   * 提交报名
   * @param {object} form 表单数据
   * @returns {Promise<{success: boolean, message: string, data?: object}>}
   *
   * 调用流程：
   *   1. POST /api/applications
   *   2. 后端校验（防重复、名额、字段）
   *   3. 落库 + 班级 enrolled +1
   *   4. 返 {code:200, message, data:{id, name, ...}}
   */
  async function submitApplication(form) {
    try {
      const res = await submitApplicationAPI(form)
      // res = {code:200, message:"提交成功", data:{id:1, name, ...}}
      if (res.code === 200) {
        return { success: true, message: res.message, data: res.data }
      }
      return { success: false, message: res.message || '提交失败' }
    } catch (err) {
      // 网络错误或 500
      return { success: false, message: '网络错误，请稍后重试' }
    }
  }

  /**
   * 撤回报名
   * @param {number} id 报名 ID
   * @returns {Promise<{success: boolean, message: string}>}
   */
  async function withdrawApplication(id) {
    try {
      const res = await withdrawApplicationAPI(id)
      if (res.code === 200) {
        return { success: true, message: res.message }
      }
      return { success: false, message: res.message || '撤回失败' }
    } catch (err) {
      return { success: false, message: '网络错误，请稍后重试' }
    }
  }

  return {
    applications,
    submitApplication,
    withdrawApplication,
  }
}
