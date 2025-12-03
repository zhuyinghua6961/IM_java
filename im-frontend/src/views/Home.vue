<template>
  <div class="home-container">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="user-avatar">
        <el-avatar :size="50" :src="userStore.userInfo.avatar">
          {{ userStore.userInfo.nickname?.charAt(0) }}
        </el-avatar>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>聊天</span>
          <el-badge v-if="totalUnread > 0" :value="totalUnread" class="badge" />
        </el-menu-item>
        
        <el-menu-item index="/contacts">
          <el-icon><User /></el-icon>
          <span>通讯录</span>
        </el-menu-item>
        
        <el-menu-item index="/moments">
          <el-icon><PictureFilled /></el-icon>
          <span>朋友圈</span>
        </el-menu-item>
        
        <el-menu-item index="/profile">
          <el-icon><UserFilled /></el-icon>
          <span>我的</span>
        </el-menu-item>
        
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </el-menu-item>
      </el-menu>
      
      <div class="logout-btn">
        <el-button text @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </div>
    
    <!-- 主内容区 -->
    <div class="main-content">
      <router-view />
    </div>
    
    <!-- 连接状态提示 -->
    <div v-if="!chatStore.connected" class="connection-status">
      <el-alert type="warning" :closable="false">
        <template #title>
          <el-icon class="is-loading"><Loading /></el-icon>
          正在连接服务器...
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import websocket from '@/utils/websocket'
import { getConversationList } from '@/api/message'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const chatStore = useChatStore()

const activeMenu = computed(() => route.path)

const totalUnread = computed(() => {
  // 统计非免打扰会话的未读数 + 免打扰但被@的会话的未读数
  return chatStore.conversations
    .filter(conv => !conv.muted || conv.hasAtMe)
    .reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
})

const handleMenuSelect = (index) => {
  router.push(index)
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

// 处理接收到的消息
const handleMessageReceived = (message) => {
  // 消息处理已由 Chat.vue 的 MessageSyncManager 统一处理
  // 这里只做简单日志记录，不重复处理
}

// 加载会话列表
const loadConversations = async () => {
  try {
    const res = await getConversationList()
    chatStore.setConversations(res.data)
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

onMounted(() => {
  // 连接WebSocket
  // 注意：需要先启动 im-message-service (8082端口)
  websocket.connect(userStore.token, handleMessageReceived)
  // 会话列表由 Chat.vue 统一加载和处理
  
  // 启动心跳
  const heartbeatInterval = setInterval(() => {
    websocket.sendHeartbeat()
  }, 30000)
  window.heartbeatInterval = heartbeatInterval
})

onUnmounted(() => {
  // 清除心跳定时器
  if (window.heartbeatInterval) {
    clearInterval(window.heartbeatInterval)
  }
})
</script>

<style scoped>
.home-container {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #f5f5f5;
}

.sidebar {
  width: 70px;
  background: #2c2c2c;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.user-avatar {
  margin-bottom: 30px;
  cursor: pointer;
}

.sidebar-menu {
  flex: 1;
  width: 100%;
  background: transparent;
  border: none;
}

.sidebar-menu .el-menu-item {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #999;
  position: relative;
}

.sidebar-menu .el-menu-item.is-active {
  background: #1a1a1a;
  color: #07c160;
}

.sidebar-menu .el-menu-item:hover {
  background: #1a1a1a;
  color: #fff;
}

.sidebar-menu .el-menu-item .el-icon {
  margin-right: 0;
  font-size: 24px;
}

.sidebar-menu .el-menu-item span {
  display: block;
  font-size: 12px;
  margin-top: 5px;
}

.badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

.logout-btn {
  margin-top: auto;
  padding: 10px 0;
}

.logout-btn .el-button {
  color: #999;
  font-size: 24px;
}

.logout-btn .el-button:hover {
  color: #fff;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.connection-status {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
}
</style>
