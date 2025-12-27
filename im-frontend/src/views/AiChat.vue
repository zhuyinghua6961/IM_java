<template>
  <div class="ai-chat-container">
    <!-- 侧边栏：会话列表 -->
    <div class="conversation-sidebar" :class="{ 'sidebar-collapsed': !showSidebar }">
      <div class="sidebar-header">
        <span>历史对话</span>
        <el-button text @click="showNewConversation = true; showSidebar = false">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: currentConversationId === conv.id }"
          @click="loadConversation(conv.id)"
        >
          <div class="conv-title">{{ conv.title }}</div>
          <div class="conv-time">{{ formatDate(conv.updateTime) }}</div>
        </div>
        <div v-if="conversations.length === 0" class="no-conversation">
          暂无历史对话
        </div>
      </div>
    </div>

    <!-- 聊天窗口 -->
    <div class="ai-chat-window">
      <!-- 头部 -->
      <div class="ai-chat-header">
        <div class="header-left">
          <el-button text @click="showSidebar = !showSidebar">
            <el-icon><Fold /></el-icon>
          </el-button>
          <el-avatar :size="40" src="https://api.dicebear.com/7.x/bottts/svg?seed=ai-assistant">
            AI
          </el-avatar>
          <div class="header-info">
            <span class="ai-name">AI 聊天助手</span>
            <span class="ai-status">
              <span class="status-dot"></span>
              在线
            </span>
          </div>
        </div>
        <div class="header-right">
          <el-button text @click="clearChatHistory">
            <el-icon><Delete /></el-icon>
            清空聊天
          </el-button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="ai-messages" ref="messagesContainer">
        <!-- 欢迎语 -->
        <div v-if="messages.length === 0 && !loading" class="welcome-message">
          <el-avatar :size="60" src="https://api.dicebear.com/7.x/bottts/svg?seed=ai-assistant">
            AI
          </el-avatar>
          <h3>AI 聊天助手</h3>
          <p>您好！我是 AI 聊天助手，随时为您服务。</p>
          <div class="quick-questions">
            <p>您可以问我这些问题：</p>
            <el-tag
              v-for="q in quickQuestions"
              :key="q"
              class="quick-tag"
              @click="sendQuickQuestion(q)"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="message-item"
          :class="msg.role === 'user' ? 'user-message' : 'ai-message'"
        >
          <el-avatar v-if="msg.role === 'assistant'" :size="36" src="https://api.dicebear.com/7.x/bottts/svg?seed=ai-assistant">
            AI
          </el-avatar>
          <el-avatar v-else :size="36" :src="userInfo.avatar">
            {{ userInfo.nickname?.charAt(0) }}
          </el-avatar>
          <div class="message-content">
            <div class="message-bubble" v-html="formatMessage(msg.content)"></div>
            <span class="message-time">{{ formatTime(msg.createTime) }}</span>
          </div>
        </div>

        <!-- AI 正在输入 -->
        <div v-if="isTyping" class="message-item ai-message">
          <el-avatar :size="36" src="https://api.dicebear.com/7.x/bottts/svg?seed=ai-assistant">
            AI
          </el-avatar>
          <div class="message-content">
            <div class="typing-bubble">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="ai-input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="输入您的问题..."
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.ex.prevent="handleShiftEnter"
          />
          <el-button
            type="primary"
            :disabled="!inputMessage.trim() || isTyping"
            :loading="isTyping"
            @click="sendMessage"
          >
            <el-icon v-if="!isTyping"><Promotion /></el-icon>
            发送
          </el-button>
        </div>
        <div class="input-tips">
          <span>按 Enter 发送，Shift+Enter 换行</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { Delete, Promotion, Plus, Fold } from '@element-plus/icons-vue'
import { createConversation, sendChatMessage, getConversationList, getConversationMessages, deleteConversation } from '@/api/ai'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo || {})
const userId = computed(() => userStore.userInfo?.userId || null)

// 消息列表
const messages = ref([])
const inputMessage = ref('')
const isTyping = ref(false)
const messagesContainer = ref(null)
const loading = ref(false)

// 会话相关
const conversations = ref([])
const currentConversationId = ref(null)
const showSidebar = ref(false)
const showNewConversation = ref(false)

// 快捷问题
const quickQuestions = ref([
  '如何修改密码？',
  '如何添加好友？',
  '会员多少钱？',
  '如何联系人工客服？'
])

// 发送消息
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content || isTyping.value) return

  // 如果没有会话，先创建
  if (!currentConversationId.value) {
    try {
      const createRes = await createConversation(userId.value, content)
      if (createRes.success) {
        currentConversationId.value = createRes.data.conversationId
        // 刷新会话列表
        await loadConversationList()
      } else {
        ElMessage.error(createRes.message || '创建会话失败')
        return
      }
    } catch (error) {
      console.error('创建会话失败:', error)
      ElMessage.error('创建会话失败')
      return
    }
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: content,
    createTime: new Date().toISOString()
  })

  inputMessage.value = ''
  isTyping.value = true

  await nextTick()
  scrollToBottom()

  try {
    const response = await sendChatMessage(currentConversationId.value, content)
    if (response.success) {
      await simulateTyping(response.reply)
      // 刷新会话列表（更新标题）
      await loadConversationList()
    } else {
      ElMessage.error(response.message || 'AI 服务暂时不可用')
      messages.value.push({
        role: 'assistant',
        content: '抱歉，我现在有点忙，请稍后再试。',
        createTime: new Date().toISOString()
      })
    }
  } catch (error) {
    console.error('AI 客服请求失败:', error)
    ElMessage.error('AI 服务暂时不可用，请稍后再试')
    messages.value.push({
      role: 'assistant',
      content: '抱歉，服务器出了点问题，请稍后再试。',
      createTime: new Date().toISOString()
    })
  } finally {
    isTyping.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 模拟打字机效果
const simulateTyping = async (fullText) => {
  const words = fullText.split('')
  let currentText = ''
  const typingSpeed = 30

  for (let i = 0; i < words.length; i++) {
    currentText += words[i]
    messages.value.push({
      role: 'assistant',
      content: currentText + (i < words.length - 1 ? '|' : ''),
      createTime: new Date().toISOString(),
      typing: true
    })
    await nextTick()
    scrollToBottom()

    if (i < words.length - 1) {
      messages.value.pop()
    }

    await new Promise(resolve => setTimeout(resolve, typingSpeed))
  }

  messages.value.pop()
  messages.value.push({
    role: 'assistant',
    content: fullText,
    createTime: new Date().toISOString()
  })
}

// 发送快捷问题
const sendQuickQuestion = async (question) => {
  inputMessage.value = question
  await sendMessage()
}

// 加载会话列表
const loadConversationList = async () => {
  if (!userId.value) return
  try {
    const response = await getConversationList(userId.value)
    if (response.success) {
      conversations.value = response.data || []
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 加载会话消息
const loadConversation = async (conversationId) => {
  loading.value = true
  currentConversationId.value = conversationId
  showSidebar.value = false

  try {
    const response = await getConversationMessages(conversationId)
    if (response.success) {
      messages.value = response.data || []
      await nextTick()
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载会话消息失败:', error)
    ElMessage.error('加载会话消息失败')
  } finally {
    loading.value = false
  }
}

// 清除聊天记录
const clearChatHistory = async () => {
  if (!currentConversationId.value) {
    messages.value = []
    return
  }
  try {
    await deleteConversation(currentConversationId.value)
    messages.value = []
    await loadConversationList()
    ElMessage.success('聊天记录已清空')
  } catch (error) {
    console.error('清空聊天记录失败:', error)
    ElMessage.error('清空聊天记录失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 格式化消息
const formatMessage = (content) => {
  if (!content) return ''
  let formatted = content.replace(/\n/g, '<br>')
  formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  return formatted
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()

  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}

// Shift+Enter 换行
const handleShiftEnter = (e) => {}

// 初始化
onMounted(() => {
  loadConversationList()
})
</script>

<style lang="scss" scoped>
.ai-chat-container {
  width: 100%;
  height: 100%;
  display: flex;
  background: #f5f5f5;
}

// 会话侧边栏
.conversation-sidebar {
  width: 260px;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;

  &.sidebar-collapsed {
    width: 0;
    overflow: hidden;
    border-right: none;
  }
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.3s;

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: #ecf5ff;
    border-left: 3px solid #409eff;
  }
}

.conv-title {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.no-conversation {
  padding: 20px;
  text-align: center;
  color: #909399;
}

// 聊天窗口
.ai-chat-window {
  display: flex;
  flex-direction: column;
  flex: 1;
  background: #fff;
}

// 头部
.ai-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.ai-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.ai-status {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #67c23a;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #67c23a;
  border-radius: 50%;
  margin-right: 4px;
}

// 消息区域
.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
}

// 欢迎语
.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #606266;

  h3 {
    margin: 16px 0 8px;
    font-size: 20px;
    color: #303133;
  }

  p {
    margin: 0;
    color: #909399;
  }
}

.quick-questions {
  margin-top: 24px;

  p {
    font-size: 13px;
    margin-bottom: 12px;
  }
}

.quick-tag {
  margin: 0 8px 8px 0;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: scale(1.05);
    background: #409eff;
    color: #fff;
  }
}

// 消息项
.message-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;

  &.user-message {
    flex-direction: row-reverse;

    .message-bubble {
      background: #409eff;
      color: #fff;
      border-radius: 18px 18px 4px 18px;
    }

    .message-time {
      text-align: right;
    }
  }

  &.ai-message {
    .message-bubble {
      background: #fff;
      border: 1px solid #ebeef5;
      border-radius: 18px 18px 18px 4px;
    }
  }
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-bubble {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  padding: 0 4px;
}

// 正在输入
.typing-bubble {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 18px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: #909399;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;

  &:nth-child(1) {
    animation-delay: 0s;
  }
  &:nth-child(2) {
    animation-delay: 0.2s;
  }
  &:nth-child(3) {
    animation-delay: 0.4s;
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

// 输入区域
.ai-input-area {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.input-wrapper {
  display: flex;
  gap: 12px;

  :deep(.el-textarea__inner) {
    border-radius: 8px;
    resize: none;
  }

  .el-button {
    align-self: flex-end;
    height: 40px;
  }
}

.input-tips {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
</style>
