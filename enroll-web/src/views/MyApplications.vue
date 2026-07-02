<!--
  MyApplications.vue · 我的报名页
  两种模式：
    1. 已登录（student_token）：自动调用 /me 展示报名记录
    2. 未登录：显示登录提示，点击跳转 StudentLogin
  支持查看/撤回/修改报名
-->
<template>
  <div class="myapps-page">
    <div class="myapps-body">
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

      <!-- 已登录：报名记录 -->
      <el-card v-else-if="records.length > 0" class="result-card">
        <template #header>
          <div class="result-header">
            <span>我的报名（共 {{ records.length }} 条）</span>
            <el-button text type="info" size="small" @click="onLogout">退出登录</el-button>
          </div>
        </template>
        <el-table :data="records" border stripe>
          <el-table-column prop="name" label="姓名" width="80" />
          <el-table-column prop="className" label="申报班级" min-width="200" show-overflow-tooltip />
          <el-table-column label="轮次" width="80">
            <template #default="{ row }">
              <el-tag size="small">第{{ getCurrentRound(row.classPeriods) }}轮</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="idCard" label="身份证号" width="160" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.status==='1'" type="success" size="small">已报名</el-tag>
              <el-tag v-else-if="row.status==='2'" type="info" size="small">已撤回</el-tag>
              <el-tag v-else-if="row.status==='3'" type="warning" size="small">已录取</el-tag>
              <el-tag v-else-if="row.status==='4'" type="danger" size="small">未录取</el-tag>
              <el-tag v-else type="info" size="small">未报名</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applyTime" label="报名时间" width="160" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button
                v-if="row.status === '1'"
                text type="primary" size="small"
                @click="onEdit(row)"
              >
                修改
              </el-button>
              <el-button
                v-if="row.status === '1'"
                text type="danger" size="small"
                @click="onWithdraw(row)"
              >
                撤回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

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
import { ArrowLeft, User, Loading } from '@element-plus/icons-vue'
import { withdrawApplicationAPI, updateApplicationAPI } from '../utils/api.js'
import { getCurrentRound } from '../utils/data.js'
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
  localStorage.removeItem('student_token')
  localStorage.removeItem('student_phone')
  isLoggedIn.value = false
  records.value = []
  router.push('/student-login')
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

onMounted(fetchMyRecords)
</script>

<style scoped>
.myapps-page { min-height: 100vh; display: flex; flex-direction: column; }
.myapps-body { max-width: 800px; margin: 24px auto; padding: 0 20px; flex: 1; width: 100%; }
.back-btn { margin-bottom: 12px; font-size: 14px; color: var(--text-secondary); }
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
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: #999;
}
.result-card { margin-bottom: 20px; }
.result-card :deep(.el-card__header) { font-weight: 600; }
.result-header { display: flex; justify-content: space-between; align-items: center; }
@media (max-width: 768px) {
  .myapps-body { margin: 12px auto; padding: 0 8px; }
  .login-tip { flex-direction: column; text-align: center; }
}
</style>
