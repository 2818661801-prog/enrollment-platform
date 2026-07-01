<!--
  MyApplications.vue · 我的报名页
  身份证 + 查询密码 双因子验证 → 查看/撤回/修改报名
-->
<template>
  <div class="myapps-page">
    <div class="myapps-body">
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>

      <!-- 查询卡 -->
      <el-card class="search-card">
        <h3 class="search-title">查询我的报名</h3>
        <div class="search-bar">
          <el-input
            v-model="idCard"
            placeholder="请输入身份证号"
            maxlength="18"
            style="max-width:200px"
            @keyup.enter="onSearch"
          />
          <el-input
            v-model="password"
            placeholder="查询密码"
            maxlength="20"
            style="max-width:160px; margin-left:8px"
            @keyup.enter="onSearch"
            show-password
          />
          <el-button type="primary" :loading="loading" @click="onSearch" style="margin-left:8px">
            查询
          </el-button>
        </div>
      </el-card>

      <!-- 报名记录 -->
      <el-card v-if="records.length > 0" class="result-card">
        <template #header>
          <span>报名记录（共 {{ records.length }} 条）</span>
        </template>
        <el-table :data="records" border stripe>
          <el-table-column prop="name" label="姓名" width="80" />
          <el-table-column prop="className" label="申报班级" min-width="200" show-overflow-tooltip />
          <el-table-column prop="idCard" label="身份证号" width="160" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status==='1'?'success':'warning'" size="small">
                {{ row.status === '1' ? '已报名' : '已撤回' }}
              </el-tag>
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

      <el-empty v-else-if="searched" description="暂无报名记录或密码错误" />

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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { fetchMyApplicationsWithPwd, withdrawApplicationAPI, updateApplicationAPI } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'

const router = useRouter()
const idCard = ref('')
const password = ref('')
const records = ref([])
const searched = ref(false)
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

async function onSearch() {
  const id = idCard.value.trim()
  if (!/^\d{17}[\dXx]$/.test(id)) {
    ElMessage.warning('请输入正确的身份证号')
    return
  }
  if (!password.value.trim()) {
    ElMessage.warning('请输入查询密码')
    return
  }
  loading.value = true
  try {
    records.value = await fetchMyApplicationsWithPwd(id, password.value.trim())
    searched.value = true
  } catch (e) {
    // 后端返回业务异常（密码错误等）
    ElMessage.error(e.message || '查询失败，密码或身份证有误')
    records.value = []
    searched.value = true
  } finally { loading.value = false }
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
    // 重新查询刷新列表
    await onSearch()
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
    await onSearch()
  } catch {}
}
</script>

<style scoped>
.myapps-page { min-height: 100vh; display: flex; flex-direction: column; }
.myapps-body { max-width: 800px; margin: 24px auto; padding: 0 20px; flex: 1; width: 100%; }
.back-btn { margin-bottom: 12px; font-size: 14px; color: var(--text-secondary); }
.search-card { margin-bottom: 20px; }
.search-title { margin: 0 0 14px 0; font-size: 16px; }
.search-bar { display: flex; align-items: center; }
.result-card :deep(.el-card__header) { font-weight: 600; }
@media (max-width: 768px) {
  .myapps-body { margin: 12px auto; padding: 0 8px; }
  .search-bar { flex-wrap: wrap; }
}
</style>
