<!--
  FormPage.vue · 报名表单页（移动端适配）
  el-steps 进度条 + el-form 8 字段 + 联动校验
  移动端：步骤条紧凑 + 表单字段纵向堆叠
-->
<template>
  <div class="form-page">
    <div class="form-body">
      <!-- 返回按钮 -->
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <!-- 表单容器 -->
      <el-card class="form-card">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          label-position="right"
          size="default"
        >
          <!-- ===== 第一行：姓名 + 身份证号 ===== -->
          <el-row :gutter="24" class="form-row">
            <el-col :xs="24" :sm="12">
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" placeholder="请输入中文姓名" maxlength="10" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="身份证号" prop="idCard">
                <el-input
                  v-model="form.idCard"
                  placeholder="请输入18位身份证号"
                  maxlength="18"
                  :suffix-icon="idCardValid ? SuccessFilled : undefined"
                  @blur="onIdCardBlur"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ===== 第二行：性别 + 联系电话 ===== -->
          <el-row :gutter="24" class="form-row">
            <el-col :xs="24" :sm="12">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="form.gender">
                  <el-radio-button value="男">男</el-radio-button>
                  <el-radio-button value="女">女</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="联系电话" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ===== 第三行：选考物理 + 选考英语 ===== -->
          <el-row :gutter="24" class="form-row">
            <el-col :xs="24" :sm="12">
              <el-form-item label="是否选考物理" prop="hasPhysics">
                <el-radio-group v-model="form.hasPhysics">
                  <el-radio-button value="是">是</el-radio-button>
                  <el-radio-button value="否">否</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="是否选考英语" prop="hasEnglish">
                <el-radio-group v-model="form.hasEnglish">
                  <el-radio-button value="是">是</el-radio-button>
                  <el-radio-button value="否">否</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ===== 第四行：申报班级 ===== -->
          <el-row :gutter="24" class="form-row">
            <el-col :xs="24" :sm="24">
              <el-form-item label="申报班级" prop="classId">
                <el-select v-model="form.classId" placeholder="请选择特色班" style="width: 100%;">
                  <el-option
                    v-for="c in classes"
                    :key="c.id"
                    :label="c.name"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ===== 第五行：杭电班类别（仅 classId===1 时显示） ===== -->
          <el-row v-if="form.classId === 1" :gutter="24" class="form-row">
            <el-col :xs="24" :sm="24">
              <el-form-item label="杭电班类别" prop="hdSubType">
                <el-radio-group v-model="form.hdSubType">
                  <el-radio-button value="电子信息类">电子信息类</el-radio-button>
                  <el-radio-button value="计算机类">计算机类</el-radio-button>
                  <el-radio-button value="自动化类">自动化类</el-radio-button>
                  <el-radio-button value="机械类">机械类</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ===== 班级时间提示 ===== -->
          <el-alert
            v-if="selectedClass"
            :title="`该班报名时间：${selectedClass.period}`"
            type="info"
            show-icon
            :closable="false"
            style="margin-bottom: 8px;"
          />

          <!-- 报名未开始提示 -->
          <el-alert
            v-if="timeStatus && timeStatus.status === 'not_started'"
            :title="`⚠️ 该班级尚未开放报名（${timeStatus.label}）`"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 8px;"
          />

          <!-- 报名已截止提示 -->
          <el-alert
            v-if="timeStatus && timeStatus.status === 'closed'"
            title="该班级报名已截止，无法提交"
            type="error"
            show-icon
            :closable="false"
            style="margin-bottom: 8px;"
          />

          <!-- 物理要求提示 -->
          <el-alert
            v-if="selectedClass && selectedClass.needPhysics && form.hasPhysics === '否'"
            title="该班级要求选考物理，请确认是否符合条件"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 20px;"
          />

          <!-- ===== 按钮组 ===== -->
          <el-form-item class="submit-btn-wrap">
            <el-button type="primary" size="large" class="submit-btn" @click="onSubmit" :loading="submitting">
              提交报名
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SuccessFilled, ArrowLeft } from '@element-plus/icons-vue'
import { initialForm, getClassTimeStatus } from '../utils/data.js'
import { fetchClasses } from '../utils/api.js'
import { validateIdCard, validatePhone, validateName, inferGender } from '../utils/validate.js'
import { useApplication } from '../composables/useApplication.js'
import AppFooter from '../components/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const { submitApplication } = useApplication()

const formRef = ref(null)
const activeStep = ref(0)
const submitting = ref(false)
const idCardValid = ref(false)
const loading = ref(false)

// 班级列表：从后端 API 拿
const classes = ref([])

const form = reactive(initialForm())

onMounted(async () => {
  // 拉取班级列表
  loading.value = true
  try {
    classes.value = await fetchClasses()
  } catch {
    ElMessage.error('网络错误，无法加载班级')
  } finally {
    loading.value = false
  }

  // 处理 URL 里的 classId
  const cid = Number(route.params.classId)
  if (cid && classes.value.some(c => c.id === cid)) {
    const cls = classes.value.find(c => c.id === cid)
    const { canApply, label } = getClassTimeStatus(cls)
    if (!canApply) {
      ElMessage.warning(`「${cls.name}」${label}，无法报名`)
      router.replace('/home')
      return
    }
    form.classId = cid
    activeStep.value = 1
  }
})

const selectedClass = computed(() =>
  classes.value.find(c => c.id === form.classId) || null
)

const timeStatus = computed(() => {
  if (!selectedClass.value) return null
  return getClassTimeStatus(selectedClass.value)
})

function onIdCardBlur() {
  const id = form.idCard.trim()
  idCardValid.value = validateIdCard(id)
  if (idCardValid.value && !form.gender) {
    form.gender = inferGender(id)
  }
}

const rules = computed(() => ({
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { validator: (_r, v, cb) => validateName(v) ? cb() : cb(new Error('请输入2-10个中文字符')), trigger: 'blur' },
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { validator: (_r, v, cb) => validateIdCard(v) ? cb() : cb(new Error('身份证号格式不正确')), trigger: 'blur' },
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' },
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { validator: (_r, v, cb) => validatePhone(v) ? cb() : cb(new Error('手机号格式不正确')), trigger: 'blur' },
  ],
  hasPhysics: [
    { required: true, message: '请选择是否选考物理', trigger: 'change' },
  ],
  hasEnglish: [
    { required: true, message: '请选择是否选考英语', trigger: 'change' },
  ],
  classId: [
    { required: true, message: '请选择申报班级', trigger: 'change' },
  ],
  hdSubType: [
    { required: form.classId === 1, message: '请选择杭电班类别', trigger: 'change' },
  ],
}))

function goBack() {
  router.push('/home')
}

function prevStep() {
  if (activeStep.value > 0) activeStep.value--
}

function nextStep() {
  if (activeStep.value < 2) activeStep.value++
}

async function onSubmit() {
  if (activeStep.value === 0) {
    if (!form.classId) {
      ElMessage.warning('请先选择申报班级')
      return
    }
    if (!timeStatus.value?.canApply) {
      ElMessage.warning(`该班级${timeStatus.value?.label || '不在报名期内'}`)
      return
    }
    activeStep.value = 1
    return
  }

  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请检查表单填写是否正确')
    return
  }

  if (activeStep.value === 1) {
    activeStep.value = 2
    return
  }

  if (!timeStatus.value?.canApply) {
    ElMessage.error(`该班级${timeStatus.value?.label || '已不在报名期内'}，无法提交`)
    return
  }

  submitting.value = true
  await new Promise(resolve => setTimeout(resolve, 800))

  const result = await submitApplication({ ...form })
  submitting.value = false

  if (result.success) {
    ElMessage.success('🎉 提交成功！感谢你的报名，请等待录取结果通知。')
    router.push('/home') // 提交后回到首页
  } else {
    ElMessage.error(result.message || '提交失败')
  }
}
</script>

<style scoped>
.form-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.form-body {
  max-width: 860px;
  margin: 24px auto;
  padding: 0 20px;
  flex: 1;
  width: 100%;
}
.back-btn {
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--text-secondary);
}
.back-btn:hover {
  color: var(--brand-primary);
}
.submit-btn-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.submit-btn {
  width: 240px;
  height: 44px;
  font-size: 16px;
}
.form-card {
  padding: 8px;
  border-radius: var(--radius-lg);
}
.form-card :deep(.el-card__body) {
  padding: 32px;
}

/* ==================== 平板适配 ==================== */
@media (max-width: 900px) {
  .form-body {
    margin: 16px auto;
    padding: 0 12px;
  }
  .form-card :deep(.el-card__body) {
    padding: 20px;
  }
}

/* ==================== 手机端适配 ==================== */
@media (max-width: 768px) {
  .form-page { padding-top: 52px; }  /* 移动端头部高度 */
  .form-body {
    margin: 12px auto;
    padding: 0 8px;
  }
  .form-card {
    padding: 0;
  }
  .form-card :deep(.el-card__body) {
    padding: 14px;
  }
  /* 表单项标签改为顶部对齐（省横向空间） */
  .form-card :deep(.el-form-item__label) {
    float: none;
    display: block;
    text-align: left;
    padding-bottom: 4px;
    width: auto !important;
  }
  .form-card :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
  /* 杭电班类别 radio-button 纵向排列 */
  .form-row :deep(.el-radio-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
  .form-row :deep(.el-radio-button) {
    margin: 0;
  }
}
</style>
