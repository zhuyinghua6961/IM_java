<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <h2>个人中心</h2>
      </div>
      
      <div class="profile-info">
        <div class="avatar-section">
          <el-avatar :size="100" :src="form.avatar || userStore.userInfo.avatar">
            {{ (form.nickname || userStore.userInfo.nickname)?.charAt(0) }}
          </el-avatar>
          <el-button text class="change-avatar" @click="handleChangeAvatarClick">更换头像</el-button>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleAvatarChange"
          />
        </div>
        
        <el-form :model="form" label-width="80px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model="form.username" disabled />
          </el-form-item>
          
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" />
          </el-form-item>
          
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :label="0">未知</el-radio>
              <el-radio :label="1">男</el-radio>
              <el-radio :label="2">女</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
          
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          
          <el-form-item label="个性签名">
            <el-input
              v-model="form.signature"
              type="textarea"
              :rows="3"
              placeholder="写点什么吧..."
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="handleSave">保存</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button text type="primary" @click="openPasswordDialog">修改密码</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
  
  <el-dialog
    v-model="showPasswordDialog"
    title="修改密码"
    width="420px"
  >
    <el-form :model="passwordForm" label-width="90px">
      <el-form-item label="手机号">
        <el-input :model-value="userStore.userInfo.phone" disabled />
      </el-form-item>
      <el-form-item label="原密码">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="验证码">
        <el-input v-model="passwordForm.code" maxlength="6">
          <template #append>
            <el-button :disabled="smsCountdown > 0" @click="handleSendSmsCode">
              {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
            </el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showPasswordDialog = false">取消</el-button>
      <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
        确认修改
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, onMounted, ref, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, changePassword, sendSmsCode } from '@/api/user'
import request from '@/utils/request'

const userStore = useUserStore()

const avatarInputRef = ref(null)

const form = reactive({
  username: '',
  nickname: '',
  gender: 0,
  phone: '',
  email: '',
  signature: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)
const showPasswordDialog = ref(false)
const smsCountdown = ref(0)
let smsTimer = null

const openPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.code = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPasswordDialog.value = true
}

const handleChangeAvatarClick = () => {
  if (avatarInputRef.value) {
    avatarInputRef.value.click()
  }
}

const handleAvatarChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  try {
    const MAX_SIZE = 2 * 1024 * 1024 // 2MB
    if (file.size > MAX_SIZE) {
      ElMessage.warning('头像图片过大，请选择 2MB 以下的图片')
      return
    }

    // 使用 canvas 进行中心裁剪并缩放为正方形
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = (e) => resolve(e.target.result)
      reader.onerror = reject
      reader.readAsDataURL(file)
    })

    const img = await new Promise((resolve, reject) => {
      const image = new Image()
      image.onload = () => resolve(image)
      image.onerror = reject
      image.src = dataUrl
    })

    const minSide = Math.min(img.width, img.height)
    const sx = (img.width - minSide) / 2
    const sy = (img.height - minSide) / 2
    const TARGET_SIZE = 256

    const canvas = document.createElement('canvas')
    canvas.width = TARGET_SIZE
    canvas.height = TARGET_SIZE
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, sx, sy, minSide, minSide, 0, 0, TARGET_SIZE, TARGET_SIZE)

    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob((b) => {
        if (!b) reject(new Error('裁剪头像失败'))
        else resolve(b)
      }, 'image/jpeg', 0.9)
    })

    if (blob.size > MAX_SIZE) {
      ElMessage.warning('裁剪后的头像仍然过大，请选择更小的图片')
      return
    }

    const uploadFile = new File([blob], (file.name || 'avatar') + '.jpg', { type: 'image/jpeg' })
    const formData = new FormData()
    formData.append('file', uploadFile)

    const res = await request.post('/files/upload/image', formData)
    const { url } = res.data || {}
    if (!url) {
      throw new Error('上传头像失败：未返回URL')
    }
    form.avatar = url
    ElMessage.success('头像上传成功，请点击保存同步到资料')
  } catch (error) {
    console.error('上传头像失败:', error)
    ElMessage.error('上传头像失败')
  }
}

const handleSave = async () => {
  try {
    await updateUserInfo(form)
    userStore.setUserInfo({ ...userStore.userInfo, ...form })
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const handleReset = () => {
  Object.assign(form, userStore.userInfo)
}

onMounted(() => {
  Object.assign(form, userStore.userInfo)
})

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.code) {
    ElMessage.warning('请输入短信验证码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能小于6位')
    return
  }
  try {
    passwordLoading.value = true
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      code: passwordForm.code
    })
    ElMessage.success('密码修改成功，请使用新密码登录')
    showPasswordDialog.value = false
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error(error.response?.data?.message || '修改密码失败')
  } finally {
    passwordLoading.value = false
  }
}

const handleSendSmsCode = async () => {
  const phone = userStore.userInfo.phone
  if (!phone) {
    ElMessage.warning('请先在个人资料中绑定手机号')
    return
  }
  if (smsCountdown.value > 0) return
  try {
    await sendSmsCode(phone)
    ElMessage.success('验证码已发送，请注意查收')
    smsCountdown.value = 60
    smsTimer = setInterval(() => {
      if (smsCountdown.value > 0) {
        smsCountdown.value -= 1
      } else if (smsTimer) {
        clearInterval(smsTimer)
        smsTimer = null
      }
    }, 1000)
  } catch (error) {
    console.error('发送验证码失败:', error)
    ElMessage.error(error.response?.data?.message || '发送验证码失败')
  }
}

onBeforeUnmount(() => {
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
})
</script>

<style scoped>
.profile-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.profile-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.profile-header {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.avatar-section {
  text-align: center;
  margin-bottom: 30px;
}

.change-avatar {
  display: block;
  margin: 10px auto 0;
}

.profile-form {
  max-width: 500px;
  margin: 0 auto;
}
</style>


