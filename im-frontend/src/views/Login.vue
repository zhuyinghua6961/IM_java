<template>
  <div class="login-container">
    <div class="login-box">
      <h1 class="title">IM Chat</h1>
      <p class="subtitle">即时通讯系统</p>
      
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
          >
            登录
          </el-button>
        </el-form-item>
        
        <div class="register-link">
          还没有账号？<a href="#" @click.prevent="showRegister = true">立即注册</a>
        </div>
      </el-form>
    </div>
    
    <!-- 注册对话框 -->
    <el-dialog v-model="showRegister" title="用户注册" width="400px">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" @click="handleRegister" :loading="registerLoading">
          注册
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const registerLoading = ref(false)
const showRegister = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const res = await login(form)
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.userInfo)
      ElMessage.success('登录成功')
      
      // 使用 nextTick 确保 store 更新完成后再跳转
      await new Promise(resolve => setTimeout(resolve, 100))
      router.push('/')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}

const handleRegister = async () => {
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    registerLoading.value = true
    try {
      await register(registerForm)
      ElMessage.success('注册成功，请登录')
      showRegister.value = false
      form.username = registerForm.username
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 30px;
}

.login-form {
  margin-top: 20px;
}

.login-btn {
  width: 100%;
}

.register-link {
  text-align: center;
  color: #999;
  font-size: 14px;
}

.register-link a {
  color: #667eea;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
