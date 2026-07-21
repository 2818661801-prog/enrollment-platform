<!--
  FormPage.vue · 报名表单页（V2.0 · 2 步流程）
  流程：
    Step 1: 手机号+验证码（未登录时）— 复用 usePhoneCode composable
    Step 2: 原表单详情（姓名/身份证/性别/选科/类别）— 提交即登录态
  设计要点：
    - 已登录用户（有 student_token）跳过 Step 1，直接进 Step 2
    - 提交成功后跳 /my-applications（已登录态，可直接看自己的报名）
    - 移动端：步骤指示器紧凑 + 表单字段纵向堆叠
-->
<template>
  <div class="form-page">
    <div class="form-body">
      <!-- 返回按钮 -->
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>

      <!-- 步骤指示器 -->
      <div class="step-indicator">
        <div class="step-item" :class="{ 'is-active': step === 1, 'is-done': step > 1 }">
          <div class="step-dot">1</div>
          <div class="step-label">手机验证</div>
        </div>
        <div class="step-line" :class="{ 'is-done': step > 1 }" />
        <div class="step-item" :class="{ 'is-active': step === 2 }">
          <div class="step-dot">2</div>
          <div class="step-label">填写报名</div>
        </div>
      </div>

      <!-- ==================== Step 1: 手机号+验证码 ==================== -->
      <el-card v-if="step === 1" class="step-card">
        <template #header>
          <div class="card-header">
            <el-icon size="20" color="#337eff"><Iphone /></el-icon>
            <span>请先验证手机号</span>
          </div>
        </template>
        <p class="step-desc">验证手机号后即可提交报名，并自动登录查看报名记录</p>

        <el-form
          ref="phoneFormRef"
          :model="{ phone: phoneCode.phone.value }"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="手机号">
            <el-input
              v-model="phoneCode.phone.value"
              placeholder="请输入11位手机号"
              maxlength="11"
              @keyup.enter="phoneCode.onSendCode"
            >
              <template #prefix>
                <span class="phone-prefix">+86</span>
              </template>
            </el-input>
          </el-form-item>

          <!-- 验证码输入 + 发送按钮并排，始终显示 -->
          <div class="code-row">
            <el-input
              v-model="phoneCode.code.value"
              placeholder="请输入6位验证码"
              maxlength="6"
              class="code-input"
              @keyup.enter="onVerifyAndNext"
            />
            <el-button
              type="primary"
              class="send-btn"
              :disabled="phoneCode.countdown.value > 0"
              :loading="phoneCode.sending.value"
              @click="phoneCode.onSendCode"
            >
              {{ phoneCode.countdown.value > 0 ? `${phoneCode.countdown.value}秒后重发` : '发送验证码' }}
            </el-button>
          </div>

          <el-button
            type="primary"
            class="next-btn"
            :loading="phoneCode.logging.value"
            @click="onVerifyAndNext"
          >
            下一步
          </el-button>
        </el-form>
      </el-card>

      <!-- ==================== Step 2: 表单详情 ==================== -->
      <el-card v-else-if="step === 2" class="form-card">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="default"
          validateOnMount="false"
          @submit.prevent
        >
          <!-- 当前报名班级 + 班级介绍按钮（同一行） -->
          <div v-if="selectedClass" class="class-desc-row">
            <el-alert
              :title="`当前报名：${selectedClass.name}`"
              type="info"
              show-icon
              :closable="false"
              class="class-desc-alert"
            />
            <button
              v-if="selectedClass.description"
              type="button"
              class="class-desc-btn"
              @click="showClassDesc"
            >
              <img :src="descIcon" alt="" class="class-desc-btn-icon" />
              班级介绍
            </button>
          </div>

          <!-- 班级介绍弹窗 -->
          <el-dialog
            v-model="descDialogVisible"
            width="90%"
            max-width="460px"
            destroy-on-close
            :show-close="false"
            class="class-desc-dialog"
          >
            <div class="class-desc-dialog-bar" />
            <div class="class-desc-dialog-head">
              <img :src="descIcon" alt="" class="class-desc-dialog-icon" />
              <span class="class-desc-dialog-title">{{ selectedClass?.name }}</span>
            </div>
            <div class="class-desc-dialog-subtitle">班级介绍</div>
            <div class="class-desc-dialog-body">
              <p class="class-desc-dialog-text">{{ selectedClass?.description }}</p>
            </div>
            <div class="class-desc-dialog-footer">
              <el-button class="class-desc-dialog-close" @click="descDialogVisible = false">我知道了</el-button>
            </div>
          </el-dialog>

          <!-- ===== 第一行：姓名 + 身份证号 ===== -->
          <el-row :gutter="24" class="form-row">
            <el-col :xs="24" :sm="12">
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" placeholder="请输入中文姓名" maxlength="10" :suffix-icon="nameValid ? SuccessFilled : undefined" @blur="onNameBlur" />
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

          <!-- ===== 第二行：性别 + 选考物理 + 选考英语 ===== -->
          <el-row :gutter="24" class="form-row">
            <el-col :xs="24" :sm="8">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="form.gender">
                  <el-radio-button value="男">男</el-radio-button>
                  <el-radio-button value="女">女</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="高考是否含物理/理综" prop="hasPhysics">
                <el-radio-group v-model="form.hasPhysics">
                  <el-radio-button value="是">是</el-radio-button>
                  <el-radio-button value="否">否</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="高考是否含英语" prop="hasEnglish">
                <el-radio-group v-model="form.hasEnglish">
                  <el-radio-button value="是">是</el-radio-button>
                  <el-radio-button value="否">否</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 申报班级由 URL 参数直接指定，无需下拉选择 -->

          <!-- ===== 班级类别 ===== -->
          <el-row v-if="(selectedClass?.categories?.length || selectedClass?.categoryNames?.length)" :gutter="24" class="form-row">
            <el-col :xs="24" :sm="24">
              <el-form-item label="班级类别" prop="appliedCategory">
                <el-radio-group v-model="form.appliedCategory">
                  <el-radio-button
                    v-for="cat in (selectedClass.categories || selectedClass.categoryNames || [])"
                    :key="cat"
                    :value="cat"
                  >{{ cat }}</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- ===== 班级时间提示 ===== -->
          <el-alert
            v-if="selectedClass"
            :title="`该班报名时间：${route.query.period || selectedClass.period}`"
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

          <!-- ===== 按钮组 ===== -->
          <div class="submit-btn-wrap">
            <el-button type="primary" size="large" class="submit-btn" @click="onSubmit" :loading="submitting" :disabled="submitting || !timeStatus?.canApply">
              提交报名
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { SuccessFilled, ArrowLeft, Iphone } from '@element-plus/icons-vue'
import { initialForm, getClassTimeStatus } from '../utils/data.js'
import { fetchClasses } from '../utils/api.js'
import { validateIdCard, validateName, inferGender } from '../utils/validate.js'
import { useApplication } from '../composables/useApplication.js'
import { usePhoneCode } from '../composables/usePhoneCode.js'
import descIcon from '../assets/images/description.svg'
import AppFooter from '../components/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const { submitApplication } = useApplication()
const phoneCode = usePhoneCode()  // 共享的"发码+验证+登录"逻辑

const formRef = ref(null)
const submitting = ref(false)
const idCardValid = ref(false)
const nameValid = ref(false)
const loading = ref(false)
const descDialogVisible = ref(false)

function showClassDesc() {
  descDialogVisible.value = true
}

// 步骤：1=验证码 / 2=表单详情
const step = ref(1)

// 班级列表：从后端 API 拿
const classes = ref([])

const form = reactive(initialForm())

onMounted(async () => {
  // 拉取班级列表
  loading.value = true
  try {
    classes.value = await fetchClasses()
  } catch {
    ElMessage.error('网络错误，无法加载数据')
  } finally {
    loading.value = false
  }

  // 检查是否已登录（已有 student_token）→ 跳过 Step 1
  const existingToken = localStorage.getItem('student_token')
  const existingPhone = localStorage.getItem('student_phone')
  if (existingToken && existingPhone) {
    // 已登录：直接进 Step 2，且预填手机号
    step.value = 2
    form.phone = existingPhone
  }

  // 进入页面时清除所有校验提示（不自动校验）
  nextTick(() => formRef.value?.clearValidate())
})

// 监听班级列表 + 路由 classId，两者都就绪才预填表单
// 多轮班：period 从 URL query param 传入（第2轮时间），而非 cls.period（第一轮时间）
watch(
  [() => classes.value.length, () => route.params.classId],
  ([len, cid]) => {
    if (!len || !cid) return
    const classId = Number(cid)
    if (!classId || form.classId === classId) return
    const cls = classes.value.find(c => c.id === classId)
    if (!cls) return
    // 优先用 URL 传入的 period（多轮班第2轮时间），否则降级用 cls.period（单轮班）
    const periodStr = route.query.period || cls.period
    const { canApply, label } = getClassTimeStatus({ period: periodStr })
    if (!canApply) {
      ElMessage.warning(`「${cls.name}」${label}，无法报名`)
      router.replace('/home')
      return
    }
    form.classId = classId
  },
  { immediate: true }
)

const selectedClass = computed(() =>
  classes.value.find(c => c.id === form.classId) || null
)

// 每秒刷新的"可信当前时间"，驱动 timeStatus 实时重新计算
const now = ref(Date.now())
let _timer = null
onMounted(() => { _timer = setInterval(() => { now.value = Date.now() }, 1000) })
onUnmounted(() => clearInterval(_timer))

const timeStatus = computed(() => {
  now.value  // 强制依赖：每秒触发重新计算
  if (!selectedClass.value) return null
  // 优先用 URL 传入的 period，保持多轮班时间判断一致
  const periodStr = route.query.period || selectedClass.value.period
  return getClassTimeStatus({ period: periodStr })
})

function onNameBlur() {
  const name = form.name.trim()
  nameValid.value = validateName(name)
}

function onIdCardBlur() {
  const id = form.idCard.trim()
  idCardValid.value = validateIdCard(id)
  if (idCardValid.value && !form.gender) {
    form.gender = inferGender(id)
  }
}

const rules = computed(() => ({
  name: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请输入姓名')) : validateName(v) ? cb() : cb(new Error('请输入2-10个中文字符')), trigger: 'blur' },
  ],
  idCard: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请输入身份证号')) : validateIdCard(v) ? cb() : cb(new Error('身份证号格式不正确')), trigger: 'blur' },
  ],
  gender: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请选择性别')) : cb(), trigger: 'change' },
  ],
  hasPhysics: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请选择是否选考物理')) : cb(), trigger: 'change' },
  ],
  hasEnglish: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请选择是否选考英语')) : cb(), trigger: 'change' },
  ],
  appliedCategory: [
    { validator: (_r, v, cb) => !(selectedClass.value?.categories?.length || selectedClass.value?.categoryNames?.length) || v ? cb() : cb(new Error('请选择班级类别')), trigger: 'change' },
  ],
}))

function goBack() {
  // Step 2 → Step 1：返回上一步
  if (step.value === 2 && !localStorage.getItem('student_token')) {
    step.value = 1
    return
  }
  router.push('/home')
}

/**
 * Step 1 → Step 2：验证手机号成功后进入表单
 */
async function onVerifyAndNext() {
  const ok = await phoneCode.onLogin()
  if (!ok) return
  // 把验证过的手机号回填到表单（用户可改）
  form.phone = phoneCode.phone.value
  // 跳到 Step 2
  step.value = 2
  nextTick(() => formRef.value?.clearValidate())
}

async function onSubmit() {
  // 防抖：正在提交中，拒绝重复调用
  if (submitting.value) return

  // 1) 校验班级选择
  if (!selectedClass.value) {
    ElMessage.warning('未指定申报班级，请从首页选择班级进入')
    return
  }
  if (!timeStatus.value?.canApply) {
    ElMessage.warning(`该班级${timeStatus.value?.label || '不在报名期内'}`)
    return
  }

  // 2) 校验表单
  if (!formRef.value) return
  let valid = false
  try {
    valid = await formRef.value.validate()
  } catch {
    valid = false
  }
  if (!valid) {
    ElMessage.warning('请检查表单填写是否正确')
    return
  }

  // 3) 实际提交
  submitting.value = true
  try {
    // 注入报名须知同意状态（localStorage 有记录则同意，否则不同意）
    const payload = { ...form, noticeAgreed: !!localStorage.getItem('student_notice_agreed') }
    const result = await submitApplication(payload)
    if (result.success) {
      ElMessage.success('报名提交成功！')
      window.dispatchEvent(new Event('application_changed'))
      router.push('/my-applications')
    } else {
      ElMessage.error(result.message || '提交失败')
    }
  } catch {
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding-top: 56px;   /* fixed header 高度，防止内容被遮挡 */
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

/* ==================== 步骤指示器 ==================== */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin: 0 auto 24px;
  max-width: 360px;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.step-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  border: 2px solid #e2e8f0;
  transition: all 0.2s;
}
.step-item.is-active .step-dot {
  background: #337eff;
  color: #fff;
  border-color: #337eff;
  box-shadow: 0 0 0 4px rgba(51, 126, 255, 0.12);
}
.step-item.is-done .step-dot {
  background: #22c55e;
  border-color: #22c55e;
  color: #fff;
}
.step-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}
.step-item.is-active .step-label,
.step-item.is-done .step-label {
  color: #0f172a;
}
.step-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
  margin: 0 12px;
  margin-bottom: 22px;
  transition: background 0.2s;
}
.step-line.is-done {
  background: #22c55e;
}

/* ==================== Step 1: 手机验证 ==================== */
.step-card {
  border-radius: 8px;
}
.step-card :deep(.el-card__header) {
  padding: 16px 20px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.step-desc {
  margin: 0 0 20px;
  font-size: 13px;
  color: #64748b;
}
.phone-prefix {
  font-size: 14px;
  color: #94a3b8;
  padding: 0 4px;
}
.send-btn {
  width: auto;
  flex-shrink: 0;
}
.code-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.code-input {
  flex: 1;
}
.next-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 8px;
}

/* ==================== Step 2: 表单详情 ==================== */
.submit-btn-wrap {
  margin-top: 24px;
  text-align: center;
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
  padding: 40px 48px;
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
    margin-top: 0;
    padding: 0 8px;
  }
  .form-card,
  .step-card {
    padding: 0;
  }
  .form-card :deep(.el-card__body),
  .step-card :deep(.el-card__body) {
    padding: 14px;
  }
  /* 表单项标签改为顶部对齐（省横向空间） */
  .form-card :deep(.el-form-item__label) {
    float: none;
    display: block;
    text-align: left;
    padding-bottom: 4px;
    width: auto !important;
    font-size: 13px;
    line-height: 1.4;
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

.form-category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.form-category-tags .el-tag {
  font-size: 14px;
  padding: 4px 12px;
}

/* 校验通过-绿色勾 */
:deep(.el-input__suffix .el-icon) {
  color: #22c55e;
}

/* ==================== 班级介绍按钮（同行右侧） ==================== */
.class-desc-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.class-desc-alert {
  flex: 1;
  min-width: 0;
}

.class-desc-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  background: rgba(51, 126, 255, 0.1);
  color: #337ffe;
  border: 1.5px solid rgba(51, 126, 255, 0.3);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s, border-color 0.2s;
}

.class-desc-btn:hover {
  background: rgba(51, 126, 255, 0.18);
  border-color: rgba(51, 126, 255, 0.5);
}

.class-desc-btn-icon {
  width: 16px;
  height: 16px;
}

/* 移动端 */
@media (max-width: 768px) {
  .class-desc-row { gap: 8px; }
  .class-desc-btn { padding: 0 10px; font-size: 12px; }
}

/* ==================== 班级介绍弹窗 ==================== */
.class-desc-dialog :deep(.el-dialog__header) { display: none; }
.class-desc-dialog :deep(.el-dialog__body)   { padding: 0; }
.class-desc-dialog :deep(.el-dialog__footer) { display: none; }
.class-desc-dialog :deep(.el-dialog) { border-radius: 12px; overflow: hidden; }

.class-desc-dialog-bar {
  height: 4px;
  background: linear-gradient(90deg, #337ffe, #5b9bff);
}

.class-desc-dialog-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 0;
}

.class-desc-dialog-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.class-desc-dialog-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.class-desc-dialog-subtitle {
  display: inline-block;
  margin: 6px 20px 0;
  padding: 3px 10px;
  font-size: 12px;
  color: #337ffe;
  background: #eaf2ff;
  border-radius: 4px;
  letter-spacing: 0.04em;
}

.class-desc-dialog-body {
  margin: 12px 16px 16px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e8ecf2;
}

.class-desc-dialog-text {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.class-desc-dialog-footer {
  padding: 0 20px 20px;
}

.class-desc-dialog-close {
  width: 100%;
  height: 40px;
  background: #337ffe;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}

.class-desc-dialog-close:hover { background: #2563eb; color: #fff; }

@media (max-width: 768px) {
  .class-desc-dialog :deep(.el-dialog) { max-width: 92vw !important; }
  .class-desc-dialog-head { padding: 16px 16px 0; }
  .class-desc-dialog-subtitle { margin: 6px 16px 0; }
  .class-desc-dialog-body { margin: 10px 12px 14px; padding: 12px; }
}
</style>