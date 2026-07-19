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
      <!-- 返回按钮 -->
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>

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
        <!-- 顶部：标题 + 退出登录 -->
        <div class="records-header">
          <span class="records-count">我的报名（共 {{ records.length }} 条）</span>
          <el-button text type="danger" size="small" @click="onLogout">
            <el-icon><SwitchButton /></el-icon> 退出登录
          </el-button>
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
                  v-if="record.status === '1' || record.status === '4'"
                  text type="primary" size="small"
                  @click="onEdit(record)"
                >修改</el-button>
                <el-button
                  v-if="record.status === '1' || record.status === '4'"
                  text type="danger" size="small"
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
              <div class="info-row">
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
      <el-dialog v-model="editDialogVisible" title="修改报名信息" width="90%" destroy-on-close>
        <el-form :model="editForm" label-width="100px" label-position="right">
          <el-form-item label="姓名">
            <el-input v-model="editForm.name" maxlength="10" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="editForm.phone" maxlength="11" />
          </el-form-item>
          <el-form-item label="选考物理">
            <el-radio-group v-model="editForm.hasPhysics">
              <el-radio-button value="是">是</el-radio-button>
              <el-radio-button value="否">否</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="选考英语">
            <el-radio-group v-model="editForm.hasEnglish">
              <el-radio-button value="是">是</el-radio-button>
              <el-radio-button value="否">否</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="editLoading" @click="onEditSubmit">保存</el-button>
        </template>
      </el-dialog>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, User, Loading, SwitchButton } from '@element-plus/icons-vue'
import { withdrawApplicationAPI, updateApplicationAPI } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'

const router = useRouter()
const isLoggedIn = ref(false)
const records = ref([])
const loading = ref(false)
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editingId = ref(null)

const editForm = reactive({
  name: '',
  phone: '',
  hasPhysics: '',
  hasEnglish: '',
})

function goBack() { router.push('/home') }
function goLogin() { router.push('/student-login') }

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
    router.push('/student-login')
  }).catch(() => {})
}

async function fetchMyRecords() {
  const token = localStorage.getItem('student_token')
  if (!token) {
    isLoggedIn.value = false
    return
  }
  isLoggedIn.value = true
  loading.value = true
  try {
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
  editForm.phone = row.phone
  editForm.hasPhysics = row.hasPhysics || '否'
  editForm.hasEnglish = row.hasEnglish || '否'
  editDialogVisible.value = true
}

async function onEditSubmit() {
  if (!editForm.name.trim()) {
    ElMessage.warning('姓名不能为空')
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

async function onWithdraw(row) {
  try {
    await ElMessageBox.confirm('确定撤回该报名吗？撤回后不可恢复。', '提示', { type: 'warning' })
    await withdrawApplicationAPI(row.id)
    ElMessage.success('已撤回')
    await fetchMyRecords()
  } catch {}
}

// 状态标签颜色
function statusTagType(status) {
  switch (status) {
    case '1': return 'success'   // 审核中 → 绿
    case '2': return 'info'      // 已撤回 → 灰
    case '3': return 'warning'   // 已录取 → 黄
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

// 格式化时间
function formatTime(applyTime) {
  if (!applyTime) return '-'
  return applyTime.replace('T', ' ')
}

onMounted(fetchMyRecords)
</script>

<style scoped>
.myapps-page { min-height: 100vh; display: flex; flex-direction: column; }
.myapps-body {
  max-width: 900px;
  margin: 24px auto;
  padding: 0 20px;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}
.back-btn { margin-bottom: 12px; font-size: 14px; color: var(--text-secondary); }

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
.status-1::before { background: #22c55e; }  /* 审核中-绿 */
.status-2::before { background: #94a3b8; }  /* 已撤回-灰 */
.status-3::before { background: #f59e0b; }  /* 已录取-黄 */
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

/* 轮次标签醒目样式 */
.round-tag {
  font-weight: 700 !important;
  font-size: 12px !important;
  border-color: #f59e0b !important;
  background: #fef3c7 !important;
  color: #b45309 !important;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
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
  .el-message-box__title { font-size: 16px !important; }
}
</style>
