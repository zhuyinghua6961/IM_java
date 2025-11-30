<template>
  <div class="login-container">
    <div class="login-box">
      <h1 class="title">IM Chat</h1>
      <p class="subtitle">即时通讯系统</p>
      
      <!-- 登录方式切换 -->
      <el-tabs v-model="loginType" class="login-tabs">
        <el-tab-pane label="密码登录" name="password"></el-tab-pane>
        <el-tab-pane label="验证码登录" name="sms"></el-tab-pane>
      </el-tabs>
      
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <!-- 密码登录 -->
        <template v-if="loginType === 'password'">
          <el-form-item prop="account">
            <el-input
              v-model="form.account"
              placeholder="请输入用户名或手机号"
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
        </template>
        
        <!-- 验证码登录 -->
        <template v-else>
          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              prefix-icon="Iphone"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="code">
            <div class="code-input-group">
              <el-input
                v-model="form.code"
                placeholder="请输入验证码"
                prefix-icon="Message"
                size="large"
                @keyup.enter="handleLogin"
              />
              <el-button
                size="large"
                :disabled="countdown > 0"
                @click="handleSendCode"
                class="code-btn"
              >
                {{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </template>
        
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
    <el-dialog v-model="showRegister" title="用户注册" width="450px">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <div class="code-input-group">
            <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
            <el-button
              :disabled="regCountdown > 0"
              @click="handleSendRegCode"
            >
              {{ regCountdown > 0 ? `${regCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="registerForm.code" placeholder="请输入短信验证码" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
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
import { login, register, sendSmsCode } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const registerLoading = ref(false)
const showRegister = ref(false)
const loginType = ref('password') // 登录方式：password-密码登录, sms-验证码登录
const countdown = ref(0) // 登录验证码倒计时
const regCountdown = ref(0) // 注册验证码倒计时

const form = reactive({
  account: '', // 用户名或手机号
  username: '',
  phone: '',
  password: '',
  code: ''
})

const registerForm = reactive({
  phone: '',
  code: '',
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const rules = {
  account: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const registerRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
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
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      // 构造登录数据
      const loginData = {
        loginType: loginType.value
      }
      
      if (loginType.value === 'password') {
        // 密码登录：判断是用户名还是手机号
        const isPhone = /^1[3-9]\d{9}$/.test(form.account)
        if (isPhone) {
          loginData.phone = form.account
        } else {
          loginData.username = form.account
        }
        loginData.password = form.password
      } else {
        // 验证码登录
        loginData.phone = form.phone
        loginData.code = form.code
      }
      
      const res = await login(loginData)
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
      // 自动填充用户名到登录表单
      loginType.value = 'password'
      form.account = registerForm.username
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      registerLoading.value = false
    }
  })
}

// 发送登录验证码
const handleSendCode = async () => {
  if (!form.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  
  try {
    await sendSmsCode(form.phone)
    ElMessage.success('验证码已发送，请查看控制台日志')
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

// 发送注册验证码
const handleSendRegCode = async () => {
  if (!registerForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(registerForm.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  
  try {
    await sendSmsCode(registerForm.phone)
    ElMessage.success('验证码已发送，请查看控制台日志')
    
    // 开始倒计时
    regCountdown.value = 60
    const timer = setInterval(() => {
      regCountdown.value--
      if (regCountdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
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

.login-tabs {
  margin-bottom: 20px;
}

.code-input-group {
  display: flex;
  gap: 10px;
}

.code-input-group .el-input {
  flex: 1;
}

.code-btn {
  min-width: 120px;
  white-space: nowrap;
}
</style>
