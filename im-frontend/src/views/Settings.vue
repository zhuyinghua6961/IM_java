<template>
  <div class="settings-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
      <h2>设置</h2>
      <div></div>
    </div>

    <!-- 设置列表 -->
    <div class="settings-content">
      <!-- 隐私与安全 -->
      <div class="setting-section">
        <h3 class="section-title">隐私与安全</h3>
        <div class="setting-card">
          <div class="setting-item clickable" @click="goToWhitelist">
            <div class="setting-left">
              <el-icon class="setting-icon"><Lock /></el-icon>
              <span>白名单管理</span>
            </div>
            <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="setting-section">
        <h3 class="section-title">通知设置</h3>
        <div class="setting-card">
          <div class="setting-item">
            <div class="setting-left">
              <el-icon class="setting-icon"><Bell /></el-icon>
              <span>消息通知</span>
            </div>
            <el-switch v-model="settings.notification" />
          </div>
          
          <el-divider style="margin: 0" />
          
          <div class="setting-item">
            <div class="setting-left">
              <el-icon class="setting-icon"><MuteNotification /></el-icon>
              <span>声音提示</span>
            </div>
            <el-switch v-model="settings.sound" />
          </div>
        </div>
      </div>

      <!-- 账号设置 -->
      <div class="setting-section">
        <h3 class="section-title">账号</h3>
        <div class="setting-card">
          <div class="setting-item">
            <div class="setting-left">
              <el-icon class="setting-icon"><Key /></el-icon>
              <span>自动登录</span>
            </div>
            <el-switch v-model="settings.autoLogin" />
          </div>
        </div>
      </div>

      <!-- 关于 -->
      <div class="setting-section">
        <h3 class="section-title">关于</h3>
        <div class="setting-card">
          <div class="setting-item clickable">
            <div class="setting-left">
              <el-icon class="setting-icon"><InfoFilled /></el-icon>
              <span>关于我们</span>
            </div>
            <span class="version-text">v1.0.0</span>
          </div>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="logout-section">
        <el-button type="danger" plain size="large" @click="handleLogout">
          退出登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Lock,
  ArrowRight,
  Bell,
  MuteNotification,
  Key,
  InfoFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import websocket from '@/utils/websocket'

const router = useRouter()
const userStore = useUserStore()

const settings = reactive({
  notification: true,
  sound: true,
  autoLogin: false
})

const goBack = () => {
  router.back()
}

const goToWhitelist = () => {
  router.push('/whitelist')
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    websocket.disconnect()
    userStore.logout()
    router.push('/login')
  })
}
</script>

<style scoped>
.settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background: #f5f5f5;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 0 10px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

/* 设置内容 */
.settings-content {
  max-width: 600px;
  margin: 0 auto;
}

/* 设置分组 */
.setting-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #909399;
  margin: 0 0 10px 12px;
  text-transform: uppercase;
}

/* 设置卡片 */
.setting-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 设置项 */
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  transition: background 0.2s;
}

.setting-item.clickable {
  cursor: pointer;
}

.setting-item.clickable:hover {
  background: #f5f7fa;
}

.setting-item.clickable:active {
  background: #e8eaed;
}

.setting-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.setting-icon {
  font-size: 20px;
  color: #409eff;
}

.setting-left span {
  font-size: 15px;
  color: #303133;
}

.arrow-icon {
  color: #c0c4cc;
  font-size: 16px;
}

.version-text {
  font-size: 14px;
  color: #909399;
}

/* 退出登录 */
.logout-section {
  margin-top: 40px;
  padding: 20px;
}

.logout-section .el-button {
  width: 100%;
}
</style>
