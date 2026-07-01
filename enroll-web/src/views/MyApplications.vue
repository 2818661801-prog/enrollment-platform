<!--
  MyApplications.vue · 我的报名页
  输入身份证 → 查看自己的报名记录 → 可撤回
-->
<template>
  <div class="myapps-page">
    <div class="myapps-body">
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>

      <el-card class="search-card">
        <h3 class="search-title">查询我的报名</h3>
        <div class="search-bar">
          <el-input
            v-model="idCard"
            placeholder="请输入身份证号"
            maxlength="18"
            style="max-width:260px"
            @keyup.enter="onSearch"
          />
          <el-button type="primary" :loading="loading" @click="onSearch" style="margin-left:8px">
            查询
          </el-button>
        </div>
      </el-card>

      <el-card v-if="records.length > 0" class="result-card">
        <template #header>
          <span>报名记录（共 {{ records.length }} 条）</span>
        </template>
        <el-table :data="records" border stripe>
          <el-table-column prop="name" label="姓名" width="80" />
          <el-table-column prop="className" label="申报班级" min-width="200" show-overflow-tooltip />
          <el-table-column prop="idCard" label="身份证号" width="160" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status==='1'?'success':'warning'" size="small">
                {{ row.status === '1' ? '已报名' : '已撤回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applyTime" label="报名时间" width="160" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
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

      <el-empty v-else-if="searched" description="暂无报名记录" />
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { fetchMyApplications, withdrawApplicationAPI } from '../utils/api.js'
import AppFooter from '../components/AppFooter.vue'

const router = useRouter()
const idCard = ref('')
const records = ref([])
const searched = ref(false)
const loading = ref(false)

function goBack() { router.push('/home') }

async function onSearch() {
  const id = idCard.value.trim()
  if (!/^\d{17}[\dXx]$/.test(id)) {
    ElMessage.warning('请输入正确的身份证号')
    return
  }
  loading.value = true
  try {
    records.value = await fetchMyApplications(id)
    searched.value = true
  } catch {
    ElMessage.error('查询失败')
  } finally { loading.value = false }
}

async function onWithdraw(row) {
  try {
    await ElMessageBox.confirm('确定撤回该报名吗？撤回后不可恢复。', '提示', { type: 'warning' })
    await withdrawApplicationAPI(row.id)
    ElMessage.success('已撤回')
    onSearch() // 刷新
  } catch {}
}
</script>

<style scoped>
.myapps-page { min-height: 100vh; display: flex; flex-direction: column; }
.myapps-body { max-width: 800px; margin: 24px auto; padding: 0 20px; flex: 1; width: 100%; }
.back-btn { margin-bottom: 12px; font-size: 14px; color: var(--text-secondary); }
.search-card { margin-bottom: 20px; }
.search-title { margin: 0 0 14px 0; font-size: 16px; }
.search-bar { display: flex; }
.result-card :deep(.el-card__header) { font-weight: 600; }
@media (max-width: 768px) {
  .myapps-body { margin: 12px auto; padding: 0 8px; }
}
</style>
