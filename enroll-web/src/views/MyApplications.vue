<!--
  MyApplications.vue · 我的报名页（卡片式纵向布局 · 2026-07-19）
  设计原则（来自 ui-ux-pro-max skill）：
  - 去横向滚动条：不用 el-table，改用纵向卡片流
  - 信息层次清晰：姓名/班级/状态/时间 分区展示
  - 状态标签彩色突出：审核中=绿，已录取=黄，已撤回=灰，未录取=红
  - 移动端单列，PC 端最多 2 列
  - 退出登录在卡片头部显眼位置
-->
<template>
  <div class="myapps-page">
    <div class="myapps-body">
      <!-- 顶部一行：返回 -->
      <div class="top-bar">
        <el-button text class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
      </div>

      <!-- 未登录提示卡 -->
      <el-card v-if="!isLoggedIn" class="login-card">
        <div class="login-tip">
          <el-icon size="40" color="#337ffe"><User /></el-icon>
          <div class="login-tip-text">
            <p class="login-tip-title">登录后查看我的报名</p>
            <p class="login-tip-desc">使用手机号 + 验证码登录，快速查询您的报名记录</p>
          </div>
        </div>
        <el-button type="primary" @click="goLogin">去登录</el-button>
      </el-card>

      <!-- 已登录：加载中 -->
      <div v-else-if="loading" class="loading-wrap">
        <el-icon class="is-loading" size="32" color="#337ffe"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- 已登录：报名记录卡片列表 -->
      <div v-else-if="records.length > 0" class="records-section">
        <!-- 顶部：标题 -->
        <div class="records-header">
          <span class="records-count">我的报名（共 {{ records.length }} 条）</span>
        </div>

        <!-- 卡片网格 -->
        <div class="record-cards">
          <div
            v-for="record in records"
            :key="record.id"
            class="record-card"
            :class="`status-${record.status}`"
          >
            <!-- 卡片顶部：姓名 + 状态标签 -->
            <div class="card-top">
              <div class="card-person">
                <span class="person-name">{{ record.name }}</span>
                <el-tag
                  size="small"
                  :type="statusTagType(record.status)"
                  class="status-tag"
                >
                  {{ statusLabel(record.status) }}
                </el-tag>
              </div>
              <div class="card-actions">
                <el-button
                  v-if="record.status === '1' && canWithdraw(record)"
                  text type="danger" size="small"
                  :loading="withdrawing"
                  :disabled="withdrawing"
                  @click="onWithdraw(record)"
                >撤回</el-button>
              </div>
            </div>

            <!-- 卡片中部：班级信息 -->
            <div class="card-info">
              <div class="info-row">
                <span class="info-label">申报班级</span>
                <span class="info-value">{{ record.className || '-' }}</span>
              </div>
              <div v-if="isMultiRoundClass(record) || Number(record.round) > 1" class="info-row">
                <span class="info-label">报名轮次</span>
                <span class="info-value">
                  <el-tag size="small" type="warning" class="round-tag">第{{ record.round }}轮</el-tag>
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">报名时间</span>
                <span class="info-value">{{ formatTime(record.applyTime) }}</span>
              </div>
              <div v-if="record.auditComment" class="info-row">
                <span class="info-label">审核意见</span>
                <span class="info-value" :class="record.status === '3' ? 'audit-comment-pass' : record.status === '4' ? 'audit-comment-reject' : ''">{{ record.auditComment }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 无记录 -->
      <el-empty v-else description="暂无报名记录" />

      <!-- 修改弹窗 -->
      <el-dialog
        v-model="editDialogVisible"
        width="90%"
        max-width="440px"
        destroy-on-close
        :show-close="false"
        class="edit-dialog"
      >
        <div class="edit-dialog-bar" />
        <div class="edit-dialog-head">
          <img :src="editToolsIcon" alt="" class="edit-dialog-icon" />
          <span class="edit-dialog-title">修改报名信息</span>
        </div>

        <div class="edit-dialog-body">
          <div class="edit-field">
            <label class="edit-label">姓名</label>
            <el-input v-model="editForm.name" maxlength="10" placeholder="请输入姓名" class="edit-input" />
          </div>
          <div class="edit-field">
            <label class="edit-label">身份证号</label>
            <el-input v-model="editForm.idCard" maxlength="18" placeholder="请输入18位身份证号" class="edit-input" />
          </div>
          <div class="edit-field">
            <label class="edit-label">性别</label>
            <el-radio-group v-model="editForm.gender" class="edit-radio-group">
              <el-radio-button value="男">男</el-radio-button>
              <el-radio-button value="女">女</el-radio-button>
            </el-radio-group>
          </div>
          <div class="edit-field">
            <label class="edit-label">选考物理</label>
            <el-radio-group v-model="editForm.hasPhysics" class="edit-radio-group">
              <el-radio-button value="是">是</el-radio-button>
              <el-radio-button value="否">否</el-radio-button>
            </el-radio-group>
          </div>
          <div class="edit-field">
            <label class="edit-label">选考英语</label>
            <el-radio-group v-model="editForm.hasEnglish" class="edit-radio-group">
              <el-radio-button value="是">是</el-radio-button>
              <el-radio-button value="否">否</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="edit-dialog-footer">
          <el-button class="edit-cancel-btn" @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="editLoading" class="edit-save-btn" @click="onEditSubmit">保存修改</el-button>
        </div>
      </el-dialog>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, User, Loading } from '@element-plus/icons-vue'
import { withdrawApplicationAPI, updateApplicationAPI } from '../utils/api.js'
import { validateIdCard } from '../utils/validate.js'
import { syncServerTime, trustedNow } from '../utils/data.js'
import editToolsIcon from '../assets/images/edit-tools.svg'
import AppFooter from '../components/AppFooter.vue'

const router = useRouter()
const isLoggedIn = ref(false)
const records = ref([])
const loading = ref(false)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editingId = ref(null)
const withdrawing = ref(false)  // 撤回按钮防抖

const editForm = reactive({
  name: '',
  idCard: '',
  gender: '',
  hasPhysics: '',
  hasEnglish: '',
})

function goBack() { router.push('/home') }
function goLogin() { router.push('/student-login') }

/** 判断报名记录是否为多轮班级（通过 classPeriods JSON 判断） */
function isMultiRoundClass(record) {
  try {
    const periods = JSON.parse(record.classPeriods || '[]')
    return Array.isArray(periods) && periods.length > 1
  } catch {
    return false
  }
}

function onLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    alignCenter: true,
    roundButton: true,
  }).then(() => {
    localStorage.removeItem('student_token')
    localStorage.removeItem('student_phone')
    isLoggedIn.value = false
    records.value = []
    ElMessage.success('已退出登录')
    window.dispatchEvent(new Event('login_changed'))
    router.push('/student-login')
  }).catch(() => {})
}

// 监听其他页面触发的退出登录（跨页面通知）
function onLoginChanged() {
  if (!localStorage.getItem('student_token')) {
    records.value = []
    isLoggedIn.value = false
    router.push('/student-login')
  }
}

onMounted(() => {
  window.addEventListener('login_changed', onLoginChanged)
  fetchMyRecords()
})
onUnmounted(() => window.removeEventListener('login_changed', onLoginChanged))

async function fetchMyRecords() {
  const token = localStorage.getItem('student_token')
  if (!token) {
    isLoggedIn.value = false
    return
  }
  isLoggedIn.value = true
  loading.value = true
  try {
    await syncServerTime()
    const res = await fetch('/api/applications/me', {
      headers: { Authorization: `Bearer ${token}` },
    })
    const data = await res.json()
    if (data.code !== 200) {
      if (res.status === 401 || data.message?.includes('登录')) {
        localStorage.removeItem('student_token')
        localStorage.removeItem('student_phone')
        router.push('/student-login')
        return
      }
      ElMessage.error(data.message || '获取报名记录失败')
      return
    }
    records.value = data.data || []
  } catch {
    ElMessage.error('网络错误，无法加载报名记录')
  } finally {
    loading.value = false
  }
}

function onEdit(row) {
  editingId.value = row.id
  editForm.name = row.name
  editForm.idCard = row.idCardRaw || row.idCard
  editForm.gender = row.gender || ''
  editForm.hasPhysics = row.hasPhysics || '否'
  editForm.hasEnglish = row.hasEnglish || '否'
  editDialogVisible.value = true
}

async function onEditSubmit() {
  // 防抖：正在提交中，拒绝重复调用
  if (editLoading.value) return
  // 姓名校验
  const name = editForm.name.trim()
  if (!name) {
    ElMessage.warning('姓名不能为空')
    return
  }
  if (name.length < 2) {
    ElMessage.warning('姓名至少2个字符')
    return
  }
  // 身份证校验
  const idCard = editForm.idCard.trim()
  if (!idCard) {
    ElMessage.warning('身份证号不能为空')
    return
  }
  if (!validateIdCard(idCard)) {
    ElMessage.warning('身份证号格式不正确（18位，末位可为X）')
    return
  }
  editLoading.value = true
  try {
    await updateApplicationAPI(editingId.value, { ...editForm })
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    await fetchMyRecords()
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    editLoading.value = false
  }
}

/**
 * 判断能否撤回：status=1 且 当前服务器时间 ≤ 该轮截止时间
 * 精确到秒：超过截止时间则不能撤
 */
function canWithdraw(record) {
  if (record.status !== '1') return false
  try {
    const periods = JSON.parse(record.classPeriods || '[]')
    const target = periods.find(p => p.round === Number(record.round))
    if (!target || !target.period) return false
    // period 格式："2025-07-01T00:00 - 2027-12-31T23:59"，取" - "后面的结束时间
    const endStr = target.period.split(' - ')[1]?.trim()
    if (!endStr) return false
    const endTime = new Date(endStr).getTime()
    return trustedNow().getTime() <= endTime
  } catch {
    return false
  }
}

async function onWithdraw(row) {
  // 防抖：正在撤回中，拒绝重复调用
  if (withdrawing.value) return
  withdrawing.value = true
  try {
    await ElMessageBox.confirm('确定撤回该报名吗？撤回后不可恢复。', '提示', { type: 'warning' })
    await withdrawApplicationAPI(row.id)
    ElMessage.success('已撤回')
    await fetchMyRecords()
  } catch {} finally {
    withdrawing.value = false
  }
}

// 状态标签颜色
function statusTagType(status) {
  switch (status) {
    case '1': return 'warning'   // 审核中 → 黄
    case '2': return 'info'      // 已撤回 → 灰
    case '3': return 'success'   // 已录取 → 绿
    case '4': return 'danger'    // 未录取 → 红
    default:  return 'info'
  }
}

// 状态文字
function statusLabel(status) {
  switch (status) {
    case '1': return '审核中'
    case '2': return '已撤回'
    case '3': return '已录取'
    case '4': return '未录取'
    default:  return '未知'
  }
}

// 格式化时间：去掉 T 和纳秒小数部分 → "yyyy-MM-dd HH:mm:ss"
function formatTime(applyTime) {
  if (!applyTime) return '-'
  // 去掉 ISO 的 'T'，去掉小数点后的纳秒（如 .9991521）
  return applyTime.replace('T', ' ').replace(/\.\d+$/, '')
}
</script>

<style scoped>
.myapps-page { min-height: 100vh; display: flex; flex-direction: column; padding-top: 56px; }
.myapps-body {
  max-width: 900px;
  margin: 24px auto;
  padding: 0 20px;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.back-btn { font-size: 14px; color: var(--text-secondary); }

/* 登录卡片 */
.login-card { margin-bottom: 20px; }
.login-tip {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.login-tip-text { flex: 1; }
.login-tip-title { margin: 0 0 4px; font-size: 15px; font-weight: 600; color: #333; }
.login-tip-desc { margin: 0; font-size: 13px; color: #999; }

/* 加载 */
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: #999;
}

/* 记录区域头部 */
.records-section { margin-bottom: 20px; }
.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.records-count {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 卡片网格 */
.record-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

/* 单张卡片 */
.record-card {
  background: #fff;
  border: 1px solid #e8ecf2;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.record-card:hover {
  border-color: #337ffe;
  box-shadow: 0 4px 12px rgba(51, 126, 255, 0.1);
}

/* 状态色条 */
.record-card::before {
  content: '';
  display: block;
  height: 3px;
  border-radius: 3px 3px 0 0;
  margin: -16px -16px 0 -16px;
}
.status-1::before { background: #f59e0b; }  /* 审核中-黄 */
.status-2::before { background: #94a3b8; }  /* 已撤回-灰 */
.status-3::before { background: #22c55e; }  /* 已录取-绿 */
.status-4::before { background: #ef4444; }  /* 未录取-红 */

/* 卡片顶部：姓名+操作 */
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.card-person {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.person-name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.status-tag { flex-shrink: 0; }
.card-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* 卡片中部：信息行 */
.card-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}
.info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}
.info-label {
  color: #94a3b8;
  flex-shrink: 0;
  width: 68px;
  line-height: 1.5;
}
.info-value {
  color: #334155;
  font-weight: 500;
  line-height: 1.5;
  flex: 1;
  min-width: 0;
}
.audit-comment-pass   { color: #16a34a; }   /* 已录取-绿色 */
.audit-comment-reject { color: #ef4444; }   /* 未录取-红色 */

/* 轮次标签醒目样式（主题蓝） */
.round-tag {
  font-weight: 700 !important;
  font-size: 12px !important;
  border-color: #bfdbfe !important;
  background: #eaf2ff !important;
  color: #1d4ed8 !important;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  .myapps-page { padding-top: 52px; }
  .myapps-body { margin: 12px auto; padding: 0 12px; }
  .record-cards { grid-template-columns: 1fr; gap: 12px; }
  .record-card { padding: 14px; }
  .info-label { width: 60px; font-size: 12px; }
  .info-value { font-size: 12px; }
  .person-name { font-size: 15px; }
  .card-actions { gap: 2px; }
}

/* 退出登录弹窗移动端适配 */
@media (max-width: 768px) {
  .el-message-box {
    width: 85vw !important;
    max-width: 320px !important;
  }
  .el-message-box__message { font-size: 14px !important; line-height: 1.5 !important; }
  
}

/* ===== 修改弹窗样式 ===== */
.edit-dialog :deep(.el-dialog__header) { display: none; }
.edit-dialog :deep(.el-dialog__body)   { padding: 0; }
.edit-dialog :deep(.el-dialog__footer) { display: none; }

.edit-dialog-bar {
  height: 4px;
  background: linear-gradient(90deg, #337eff, #5b9bff);
}

.edit-dialog-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.edit-dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 24px 0;
}
.edit-dialog-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.edit-dialog-body {
  padding: 20px 24px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.edit-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.edit-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0;
}
.edit-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #337eff;
}

.edit-radio-group :deep(.el-radio-button__inner) {
  border-radius: 6px;
}

.edit-dialog-footer {
  display: flex;
  gap: 12px;
  padding: 8px 24px 24px;
}
.edit-cancel-btn {
  flex: 1;
  height: 42px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  background: #fff;
  color: #64748b;
  border: 1px solid #e2e8f0;
}
.edit-cancel-btn:hover {
  border-color: #337eff;
  color: #337eff;
}
.edit-save-btn {
  flex: 1;
  height: 42px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .edit-dialog :deep(.el-dialog) { max-width: 92vw !important; }
  .edit-dialog-head { padding: 20px 16px 0; }
  .edit-dialog-body { padding: 16px 16px 4px; gap: 14px; }
  .edit-dialog-footer { padding: 4px 16px 20px; }
}
</style>
