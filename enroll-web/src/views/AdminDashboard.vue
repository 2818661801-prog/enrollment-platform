<!--
  AdminDashboard.vue · 管理后台
  4个Tab：报名须知 / 班级管理 / 报名查询 / 录取配置
-->
<template>
  <div class="admin-page">
    <!-- 顶部导航 -->
    <div class="admin-header">
      <span class="logo">管理后台</span>
      <div class="header-right">
        <span class="admin-username">{{ adminUsername }}</span>
        <el-button text @click="onLogout">退出</el-button>
      </div>
    </div>

    <!-- 4个Tab -->
    <el-tabs v-model="activeTab" class="admin-tabs" type="border-card">

      <!-- ========== Tab1: 报名须知 ========== -->
      <el-tab-pane label="报名须知" name="notice">
        <div class="tab-body">
          <h3>编辑报名须知</h3>
          <el-form label-width="90px" style="max-width:600px">
            <el-form-item label="标题">
              <el-input v-model="notice.title" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="申报条件">
              <el-input
                v-model="noticeCondText"
                type="textarea"
                :rows="3"
                placeholder="每行一条条件"
              />
            </el-form-item>
            <el-form-item label="注意事项">
              <el-input
                v-model="noticeNoticesText"
                type="textarea"
                :rows="3"
                placeholder="每行一条注意事项"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="noticeSaving" @click="onSaveNotice">
                保存报名须知
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- ========== Tab2: 班级管理 ========== -->
      <el-tab-pane label="班级管理" name="classes">
        <div class="tab-body">
          <div class="toolbar">
            <el-button type="primary" @click="showClassDialog(null)">新增班级</el-button>
          </div>
          <el-table :data="classes" border stripe style="width:100%" :scroll-x="true">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="班级名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="period" label="报名时间段" min-width="160" />
            <el-table-column prop="quota" label="名额" width="70" />
            <el-table-column prop="enrolled" label="已报名" width="80" />
            <el-table-column prop="category" label="班级类别" width="120">
              <template #default="{ row }">{{ row.category || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="showClassDialog(row)">编辑</el-button>
                <el-button text type="danger" size="small" @click="onDeleteClass(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ========== Tab3: 报名查询 ========== -->
      <el-tab-pane label="报名查询" name="query">
        <div class="tab-body">
          <!-- 筛选区 -->
          <div class="filter-bar">
            <el-select v-model="query.classId" placeholder="按班级" clearable size="default" style="width:200px">
              <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-select v-model="query.status" placeholder="按状态" clearable size="default" style="width:140px">
              <el-option label="已报名" :value="1" />
              <el-option label="已撤回" :value="0" />
              <el-option label="已录取" :value="2" />
            </el-select>
            <el-input v-model="query.idCard" placeholder="身份证号" clearable size="default" style="width:160px" />
            <el-input v-model="query.name" placeholder="姓名" clearable size="default" style="width:120px" />
            <el-button type="primary" @click="loadApplications">查询</el-button>
            <el-button @click="query={classId:null,status:null,idCard:'',name:''};loadApplications()">重置</el-button>
          </div>

          <el-table :data="appList" border stripe @selection-change="sel=>selectedApps=sel"
            style="width:100%" :scroll-x="true">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="姓名" width="80" />
            <el-table-column prop="idCard" label="身份证号" width="160" />
            <el-table-column prop="gender" label="性别" width="60" />
            <el-table-column prop="phone" label="手机号" width="120" />
            <el-table-column prop="className" label="班级" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.status==='1'" type="success" size="small">已报名</el-tag>
                <el-tag v-else-if="row.status==='0'" type="info" size="small">已撤回</el-tag>
                <el-tag v-else type="warning" size="small">已录取</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applyTime" label="报名时间" width="160" />
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="queryPage"
            :page-size="20"
            :total="appTotal"
            layout="prev,pager,next,total"
            style="margin-top:12px"
            @current-change="loadApplications"
          />

          <!-- 批量操作 -->
          <div class="batch-bar" v-if="selectedApps.length">
            <span>已选 {{ selectedApps.length }} 条</span>
            <el-button type="danger" size="small" @click="onBatchDelete">批量删除</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- ========== Tab4: 录取配置 ========== -->
      <el-tab-pane label="录取配置" name="admit">
        <div class="tab-body">
          <div class="filter-bar">
            <el-select v-model="admitQuery.classId" placeholder="按班级" clearable style="width:200px">
              <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
            <el-button type="primary" @click="loadAdmitList">查询</el-button>
          </div>
          <el-table :data="admitList" border stripe @selection-change="sel=>selectedAdmit=sel"
            style="width:100%" :scroll-x="true">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="姓名" width="80" />
            <el-table-column prop="idCard" label="身份证号" width="160" />
            <el-table-column prop="className" label="班级" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status==='2'?'warning':'success'" size="small">
                  {{ row.status==='2'?'已录取':'已报名' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="batch-bar" v-if="selectedAdmit.length">
            <span>已选 {{ selectedAdmit.length }} 名学生</span>
            <el-button type="success" @click="onBatchAdmit">批量录取</el-button>
          </div>
        </div>
      </el-tab-pane>

    </el-tabs>

    <!-- 班级编辑弹窗 -->
    <el-dialog v-model="classDialogVisible" :title="classDialogTitle" width="500px">
      <el-form label-width="100px">
        <el-form-item label="班级名称">
          <el-input v-model="classForm.name" />
        </el-form-item>
        <el-form-item label="报名时间段">
          <el-date-picker
            v-model="periodRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY/MM/DD"
            value-format="YYYY/MM/DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="名额上限">
          <el-input-number v-model="classForm.quota" :min="-1" :step="10" />
          <span class="field-tip">-1 表示不限</span>
        </el-form-item>
        <el-form-item label="班级说明">
          <el-input v-model="classForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="班级类别">
          <el-input v-model="classForm.category" placeholder="选填，如：理工类/经管类等" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="onSaveClass" :loading="classSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fetchAdminClasses, createAdminClass, updateAdminClass, deleteAdminClass,
  fetchAdminApplications, deleteAdminApplications, admitAdminApplications,
  fetchAdminConfig, updateAdminConfig,
} from '../utils/api.js'

const router = useRouter()
const activeTab = ref('notice')
const adminUsername = localStorage.getItem('admin_username') || ''

// ==================== 报名须知 ====================
const notice = reactive({ title: '' })
const noticeCondText = ref('')
const noticeNoticesText = ref('')
const noticeSaving = ref(false)

async function loadNotice() {
  try {
    const res = await fetchAdminConfig('notice')
    if (res.code === 200 && res.data) {
      const json = JSON.parse(res.data)
      notice.title = json.title || ''
      noticeCondText.value = (json.conditions || []).join('\n')
      noticeNoticesText.value = (json.notices || []).join('\n')
    }
  } catch {}
}
async function onSaveNotice() {
  noticeSaving.value = true
  try {
    const json = JSON.stringify({
      title: notice.title,
      conditions: noticeCondText.value.split('\n').filter(s => s.trim()),
      notices: noticeNoticesText.value.split('\n').filter(s => s.trim()),
    })
    await updateAdminConfig('notice', json, adminUsername)
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { noticeSaving.value = false }
}

// ==================== 班级管理 ====================
const classes = ref([])
async function loadClasses() {
  const res = await fetchAdminClasses()
  classes.value = res.data || []
}

const classDialogVisible = ref(false)
const classDialogTitle = ref('')
const classForm = reactive({ id: null, name: '', period: '', quota: 0, description: '', category: '' })
const periodRange = ref([])
const classSaving = ref(false)

/** 解析 "2026/09/01 - 2026/09/13" → ["2026/09/01", "2026/09/13"] */
function parsePeriod(period) {
  if (!period) return []
  return period.split(' - ').map(s => s.trim())
}

function showClassDialog(row) {
  if (row) {
    classDialogTitle.value = '编辑班级'
    Object.assign(classForm, { id: row.id, name: row.name, period: row.period, quota: row.quota, description: row.description, category: row.category || '' })
    periodRange.value = parsePeriod(row.period)
  } else {
    classDialogTitle.value = '新增班级'
    Object.assign(classForm, { id: null, name: '', period: '', quota: 0, description: '', category: '' })
    periodRange.value = []
  }
  classDialogVisible.value = true
}
async function onSaveClass() {
  classSaving.value = true
  try {
    // 日期范围选择器 → period 字符串
    if (periodRange.value?.length === 2) {
      classForm.period = periodRange.value.join(' - ')
    }
    // period 不能为空（由前端或后端强制校验）
    if (!classForm.period) {
      ElMessage.warning('请选择报名时间段')
      return
    }
    if (classForm.id) {
      await updateAdminClass(classForm.id, { ...classForm })
    } else {
      await createAdminClass({ ...classForm })
    }
    classDialogVisible.value = false
    loadClasses()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
  finally { classSaving.value = false }
}
async function onDeleteClass(id) {
  try {
    await ElMessageBox.confirm('删除后学生端不可见，确定删除？', '提示', { type: 'warning' })
    await deleteAdminClass(id)
    loadClasses()
    ElMessage.success('已删除')
  } catch {}
}

// ==================== 报名查询 ====================
const query = reactive({ classId: null, status: null, idCard: '', name: '' })
const queryPage = ref(1)
const appList = ref([])
const appTotal = ref(0)
const selectedApps = ref([])

async function loadApplications() {
  const params = { page: queryPage.value - 1, size: 20 }
  if (query.classId)  params.classId = query.classId
  if (query.status != null) params.status = query.status
  if (query.idCard)    params.idCard = query.idCard
  if (query.name)      params.name = query.name
  try {
    const res = await fetchAdminApplications(params)
    appList.value = res.data.list || []
    appTotal.value = res.data.total || 0
  } catch {}
}
async function onBatchDelete() {
  const ids = selectedApps.value.map(s => s.id)
  try {
    await deleteAdminApplications(ids)
    selectedApps.value = []
    loadApplications()
    ElMessage.success('已删除')
  } catch { ElMessage.error('删除失败') }
}

// ==================== 录取配置 ====================
const admitQuery = reactive({ classId: null })
const admitList = ref([])
const selectedAdmit = ref([])

async function loadAdmitList() {
  const params = { page: 0, size: 200, status: 1 }
  if (admitQuery.classId) params.classId = admitQuery.classId
  try {
    const res = await fetchAdminApplications(params)
    admitList.value = (res.data.list || []).filter(a => a.status === '1')
  } catch {}
}
async function onBatchAdmit() {
  const ids = selectedAdmit.value.map(s => s.id)
  try {
    await admitAdminApplications(ids)
    selectedAdmit.value = []
    loadAdmitList()
    ElMessage.success('已录取')
  } catch { ElMessage.error('操作失败') }
}

// ==================== 退出 ====================
function onLogout() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_username')
  router.push('/admin/login')
}

onMounted(() => {
  // 简单鉴权：没 token 就跳转
  if (!localStorage.getItem('admin_token')) {
    router.push('/admin/login')
    return
  }
  loadNotice()
  loadClasses()
  loadApplications()
})
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f0f2f5; }
.admin-header {
  background: #337eff;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  margin-bottom: 0;
}
.logo { font-size: 18px; font-weight: 600; color: #fff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.admin-username { font-size: 13px; color: rgba(255,255,255,0.85); }
:deep(.header-right .el-button) { color: #fff; }
.admin-tabs { min-height: calc(100vh - 56px); }
.tab-body { padding: 20px; }
.toolbar { margin-bottom: 14px; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.batch-bar { margin-top: 12px; display: flex; align-items: center; gap: 12px; font-size: 14px; color: #666; }
.field-tip { margin-left: 8px; font-size: 12px; color: #999; }
</style>
