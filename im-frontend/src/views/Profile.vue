<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <h2>个人中心</h2>
      </div>
      
      <div class="profile-info">
        <div class="avatar-section">
          <el-avatar :size="100" :src="userStore.userInfo.avatar">
            {{ userStore.userInfo.nickname?.charAt(0) }}
          </el-avatar>
          <el-button text class="change-avatar">更换头像</el-button>
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
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/user'

const userStore = useUserStore()

const form = reactive({
  username: '',
  nickname: '',
  gender: 0,
  phone: '',
  email: '',
  signature: ''
})

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


