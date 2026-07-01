<!--
  AdminLogin.vue · 管理员登录页
  账号 + 密码 → JWT → 存 localStorage → 跳转 /admin
-->
<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">管理后台登录</h2>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="账号"
            prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width:100%"
            :loading="loading"
            :disabled="loading"
            @click="onSubmit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLoginAPI } from '../utils/api.js'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请输入账号')) : cb(), trigger: 'blur' },
  ],
  password: [
    { validator: (_r, v, cb) => !v ? cb(new Error('请输入密码')) : cb(), trigger: 'blur' },
  ],
}

async function onSubmit() {
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    const res = await adminLoginAPI(form.username, form.password)
    if (res.code !== 200) {
      ElMessage.error(res.message || '登录失败')
      return
    }
    localStorage.setItem('admin_token', res.data.token)
    localStorage.setItem('admin_username', res.data.username)
    ElMessage.success('登录成功')
    router.push('/admin')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #337eff 0%, #5a9fff 100%);
}
.login-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  width: 380px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.title {
  text-align: center;
  margin-bottom: 28px;
  font-size: 22px;
  color: #333;
}
</style>