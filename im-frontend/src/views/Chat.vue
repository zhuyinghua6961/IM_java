<template>
  <div class="chat-container">
    <!-- 会话列表 -->
    <div class="conversation-list" :style="{ width: conversationListWidth + 'px' }">
      <div class="list-header">
        <h3>消息</h3>
        <el-badge :value="totalUnread" :hidden="totalUnread === 0" class="badge" />
      </div>
      
      <el-scrollbar class="conv-scrollbar">
        <!-- 空状态 -->
        <div v-if="conversations.length === 0" class="empty-state">
          <el-empty description="暂无会话">
            <el-button type="primary" @click="goToContacts">去添加好友</el-button>
          </el-empty>
        </div>
        
        <!-- 会话列表 -->
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: selectedConv?.id === conv.id, pinned: conv.isPinned, hidden: conv.isHidden }"
          @click="selectConversation(conv)"
          @contextmenu.prevent="showContextMenu($event, conv)"
        >
          <el-badge :value="conv.unreadCount" :hidden="conv.unreadCount === 0">
            <el-avatar :size="48" :src="conv.targetAvatar">
              {{ conv.targetName?.charAt(0) }}
            </el-avatar>
          </el-badge>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">
                <el-icon v-if="conv.isPinned" class="pin-icon"><Top /></el-icon>
                {{ conv.targetName }}
              </span>
              <span class="conv-time">{{ formatTime(conv.lastMsgTime) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="last-message">{{ conv.lastMessage }}</span>
            </div>
          </div>
          
          <!-- 悬停操作按钮 -->
          <div class="conv-actions">
            <el-dropdown trigger="click" @command="handleConvAction">
              <el-button text :icon="MoreFilled" class="action-btn" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'pin', conv }" :disabled="conv.isPinned">
                    <el-icon><Top /></el-icon>
                    {{ conv.isPinned ? '已置顶' : '置顶' }}
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'unpin', conv }" v-if="conv.isPinned">
                    <el-icon><Bottom /></el-icon>
                    取消置顶
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'hide', conv }">
                    <el-icon><Hide /></el-icon>
                    隐藏会话
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', conv }" divided>
                    <el-icon><Delete /></el-icon>
                    删除会话
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-scrollbar>
    </div>
    
    <!-- 可拖拽分割线 -->
    <div 
      class="resizer" 
      @mousedown="startResize"
      @dblclick="resetWidth"
      title="拖拽调整宽度，双击重置"
    ></div>
    
    <!-- 聊天窗口 -->
    <div class="chat-window" style="flex: 1; min-width: 0;">
      <!-- 未选择会话 -->
      <div v-if="!selectedConv" class="no-chat-selected">
        <el-empty description="选择一个会话开始聊天" />
      </div>
      
      <!-- 已选择会话 -->
      <template v-else>
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="header-left">
            <el-avatar :size="36" :src="selectedConv.targetAvatar">
              {{ selectedConv.targetName?.charAt(0) }}
            </el-avatar>
            <span class="target-name">{{ selectedConv.targetName }}</span>
          </div>
          <div class="header-right">
            <el-button text :icon="MoreFilled" />
          </div>
        </div>
        
        <!-- 消息列表 -->
        <el-scrollbar class="message-list" ref="messageScrollbar">
          <div class="message-content">
            <!-- 日期分隔 -->
            <div class="date-divider">
              <span>今天</span>
            </div>
            
            <!-- 消息项 -->
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ 'is-mine': msg.fromUserId === currentUserId }"
              @mouseenter="hoveredMessageId = msg.id"
              @mouseleave="hoveredMessageId = null"
            >
              <el-avatar :size="40" :src="msg.avatar">
                {{ msg.nickname?.charAt(0) }}
              </el-avatar>
              <div class="message-wrapper">
                <div class="message-info">
                  <span class="message-name">{{ msg.nickname }}</span>
                  <span class="message-time">{{ formatTime(msg.sendTime) }}</span>
                </div>
                <div class="message-bubble" :class="{ 'recalled': isRecalledMessage(msg), 'sending': isSendingMessage(msg) }">
                  <template v-if="isRecalledMessage(msg)">
                    <span class="recalled-text">{{ getRecalledText(msg) }}</span>
                  </template>
                  <template v-else-if="isSendingMessage(msg)">
                    <span class="sending-text">{{ msg.content }}</span>
                    <span class="sending-indicator">发送中...</span>
                  </template>
                  <template v-else>
                    <template v-if="msg.msgType === 2">
                      <img
                        class="image-message"
                        :src="msg.url || getMediaUrlFromContent(msg)"
                        @click.stop="previewImage(msg)"
                      />
                    </template>
                    <template v-else-if="msg.msgType === 3">
                      <video
                        class="video-message"
                        :src="msg.url || getMediaUrlFromContent(msg)"
                        controls
                      />
                    </template>
                    <template v-else-if="msg.msgType === 5">
                      <div
                        class="voice-message"
                        :class="{ playing: playingMessageId === msg.id }"
                        @click="playVoice(msg)"
                      >
                        <span class="voice-label">语音</span>
                        <span class="voice-duration">
                          <template v-if="playingMessageId === msg.id && playingCurrentTime > 0">
                            {{ formatVoiceSecond(playingCurrentTime) }} / {{ getVoiceDurationText(msg) }}
                          </template>
                          <template v-else>
                            {{ getVoiceDurationText(msg) }}
                          </template>
                        </span>
                      </div>
                    </template>
                    <template v-else-if="msg.msgType === 4">
                    <a
                      class="file-message"
                      :href="msg.url || getMediaUrlFromContent(msg)"
                      target="_blank"
                      @click.stop
                    >
                      {{ getFileName(msg) }}
                    </a>
                  </template>
                  <template v-else>
                    {{ msg.content }}
                  </template>
                  <!-- 悬停操作菜单 -->
                  <div 
                    v-if="hoveredMessageId === msg.id && !isRecalledMessage(msg) && !isSendingMessage(msg)" 
                    class="message-actions"
                  >
                    <el-button 
                      v-if="msg.msgType === 2"
                      text 
                      size="small" 
                      @click="favoriteEmojiFromMessage(msg)"
                      class="action-btn"
                      title="收藏为表情"
                    >
                      收藏表情
                    </el-button>
                    <!-- 撤回按钮（仅自己的消息且5分钟内） -->
                    <el-button 
                      v-if="msg.fromUserId === currentUserId && canRecall(msg)"
                      text 
                      size="small" 
                      @click="recallMessage(msg)"
                      class="action-btn"
                      title="撤回消息"
                    >
                      撤回
                    </el-button>
                    <!-- 删除按钮（所有消息都可以删除） -->
                    <el-button 
                      text 
                      size="small" 
                      @click="deleteMessage(msg)"
                      class="action-btn delete-btn"
                      title="删除消息"
                    >
                      删除
                    </el-button>
                  </div>
                </template>
              </div>
            </div>
          </div>
          
          <!-- 空消息状态 -->
          <div v-if="messages.length === 0" class="empty-messages">
            <el-empty description="暂无消息，开始聊天吧" />
          </div>
        </div>
      </el-scrollbar>
      
      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-toolbar">
          <el-button text :icon="PictureFilled" title="发送图片或视频" @click="onSelectMedia" />
          <el-button text :icon="Paperclip" title="发送文件" @click="onSelectFile" />
          <el-popover
            placement="top-start"
            trigger="click"
            width="320"
            @show="onEmojiPanelShow"
          >
            <div class="emoji-panel">
              <div class="emoji-panel-header">
                <el-button type="primary" text size="small" @click="onSelectEmojiFile">
                  添加表情
                </el-button>
              </div>
              <div v-if="favoriteEmojis.length > 0" class="emoji-list">
                <div
                  v-for="emoji in favoriteEmojis"
                  :key="emoji.id"
                  class="emoji-item"
                >
                  <img
                    :src="emoji.url"
                    class="emoji-image"
                    @click="sendEmoji(emoji)"
                  />
                  <el-button
                    text
                    size="small"
                    class="emoji-delete-btn"
                    @click.stop="removeEmoji(emoji)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
              <el-empty v-else description="暂无收藏表情" />
              <input
                ref="emojiFileInputRef"
                type="file"
                accept="image/*"
                style="display: none;"
                @change="handleEmojiFileChange"
              />
            </div>
            <template #reference>
              <el-button text :icon="ChatLineRound" title="表情" />
            </template>
          </el-popover>
          <el-button
            text
            :type="isRecording ? 'danger' : 'default'"
            @click="toggleVoiceRecording"
            :title="isRecording ? '点击停止并发送语音' : '点击开始录音，再次点击停止并发送'"
          >
            {{ isRecording ? '停止语音' : '语音' }}
          </el-button>
        </div>
        <div class="input-box">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="按 Enter 发送，Shift + Enter 换行"
            @keydown.enter="handleKeyDown"
          />
        </div>
        <div class="input-actions">
          <span class="text-count">{{ inputMessage.length }}/2000</span>
          <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">
            发送
          </el-button>
        </div>
      </div>
      <input
        ref="mediaInputRef"
        type="file"
        accept="image/*,video/*"
        style="display: none;"
        @change="handleMediaChange"
      />
      <input
        ref="fileInputRef"
        type="file"
        style="display: none;"
        @change="handleFileChange"
      />
    </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMessage as deleteMessageApi } from '@/api/message'
import { 
  Search, 
  Plus, 
  MoreFilled, 
  PictureFilled, 
  Paperclip,
  ChatLineRound,
  Top,
  Bottom,
  Delete,
  Hide
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import wsClient from '@/utils/websocket'
import messageSyncManager from '@/utils/MessageSyncManager'
import { useRouter, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()

const selectedConv = ref(null)
const inputMessage = ref('')
const messages = ref([])
const messageScrollbar = ref(null)
const hoveredMessageId = ref(null) // 当前悬停的消息ID
const isRecording = ref(false)
const playingMessageId = ref(null)
const playingCurrentTime = ref(0)
const playingTotalDuration = ref(0)
const mediaInputRef = ref(null)
const fileInputRef = ref(null)
const emojiFileInputRef = ref(null)
const favoriteEmojis = ref([])

let mediaRecorder = null
let recordedChunks = []
let recordStartTime = 0
let currentAudio = null
let mediaRecorderMimeType = ''

// 会话列表宽度控制
const conversationListWidth = ref(280)
const isResizing = ref(false)
const minWidth = 200
const maxWidth = 400

const conversations = computed(() => {
  // 对会话进行排序：置顶的在前面，然后按最后消息时间排序
  return chatStore.conversations
    .filter(conv => !conv.isHidden) // 过滤掉隐藏的会话
    .sort((a, b) => {
      // 置顶的会话优先
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      
      // 都置顶或都不置顶，按最后消息时间排序
      const timeA = new Date(a.lastMsgTime || 0).getTime()
      const timeB = new Date(b.lastMsgTime || 0).getTime()
      return timeB - timeA
    })
})
const currentUserId = computed(() => userStore.userInfo?.userId || null)
const totalUnread = computed(() => {
  return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
})

// 组件挂载时检查URL参数
onMounted(async () => {
  try {
    console.log('Chat组件开始挂载')
    
    // 先加载会话列表
    try {
      await loadConversationList()
      console.log('会话列表加载完成')
    } catch (error) {
      console.error('加载会话列表失败:', error)
    }
    
    // 处理URL参数
    const { targetId, chatType } = route.query
    if (targetId && chatType) {
      console.log('处理URL参数:', { targetId, chatType })
      try {
        const conversationId = `${chatType}-${targetId}`
        const existingConv = conversations.value.find(conv => conv.id === conversationId)
        
        if (existingConv) {
          selectedConv.value = existingConv
          console.log('使用已存在的会话:', existingConv)
        } else {
          console.log('创建新会话')
          const userInfo = await getUserInfo(Number(targetId), Number(chatType))
          selectedConv.value = {
            id: conversationId,
            targetId: Number(targetId),
            chatType: Number(chatType),
            targetName: userInfo.name,
            targetAvatar: userInfo.avatar,
            unreadCount: 0,
            lastMessage: '',
            lastMsgTime: null
          }
        }
        
        // 加载历史消息
        await loadHistoryMessages()
        console.log('历史消息加载完成')
      } catch (error) {
        console.error('处理URL参数失败:', error)
      }
    }
    
    // 初始化WebSocket和消息同步
    try {
      if (!wsClient.isConnected()) {
        // 获取token
        const token = localStorage.getItem('token')
        if (token) {
          wsClient.connect(token, (data) => {
            console.log('收到WebSocket消息:', data)
            // 处理接收到的消息
          })
        } else {
          console.warn('未找到token，跳过WebSocket连接')
        }
      }
      setupMessageSyncManager()
      console.log('WebSocket和消息同步初始化完成')
    } catch (error) {
      console.error('WebSocket初始化失败:', error)
    }
    
    // 监听消息更新
    const cleanupMessageListener = watchMessages()
    
    // 清理函数
    onUnmounted(() => {
      try {
        cleanupMessageListener()
        messageSyncManager.destroy()
        // 断开WebSocket连接
        if (wsClient.isConnected()) {
          wsClient.disconnect()
        }
      } catch (error) {
        console.error('清理资源失败:', error)
      }
    })
    
    console.log('Chat组件挂载完成')
  } catch (error) {
    console.error('Chat组件挂载失败:', error)
  }
})

// 加载会话列表
const loadConversationList = async () => {
  try {
    const response = await request.get('/conversation/list')
    
    if (response.data) {
      // 处理会话数据，添加用户信息
      const conversationsWithUserInfo = await Promise.all(
        response.data.map(async (conv) => {
          // 获取对方用户信息
          const userInfo = await getUserInfo(conv.targetId, conv.chatType)
          
          return {
            id: `${conv.chatType}-${conv.targetId}`,
            targetId: conv.targetId,
            chatType: conv.chatType,
            targetName: userInfo.name,
            targetAvatar: userInfo.avatar,
            unreadCount: conv.unreadCount || 0,
            lastMessage: conv.lastMessage || '',
            lastMsgTime: conv.updateTime
          }
        })
      )
      
      // 更新 chatStore
      chatStore.setConversations(conversationsWithUserInfo)
      
      console.log('会话列表加载成功:', conversationsWithUserInfo)
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

// 获取用户信息
const getUserInfo = async (targetId, chatType) => {
  try {
    if (chatType === 1) {
      // 单聊 - 获取用户信息
      const response = await request.get(`/user/info/${targetId}`)
      return {
        name: response.data?.nickname || `用户${targetId}`,
        avatar: response.data?.avatar || ''
      }
    } else if (chatType === 2) {
      // 群聊 - 获取群组信息
      const response = await request.get(`/group/detail/${targetId}`)
      return {
        name: response.data?.groupName || `群组${targetId}`,
        avatar: response.data?.avatar || ''
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return {
      name: chatType === 1 ? `用户${targetId}` : `群组${targetId}`,
      avatar: ''
    }
  }
}

// 加载历史消息
const loadHistoryMessages = async () => {
  if (!selectedConv.value) return
  
  try {
    const response = await request.get('/message/history', {
      params: {
        targetId: selectedConv.value.targetId,
        chatType: selectedConv.value.chatType,
        page: 1,
        size: 50
      }
    })
    
    if (response.data && response.data.list) {
      // 处理消息列表，为群聊消息获取发送者昵称
      const messageList = await Promise.all(
        response.data.list.map(async (msg) => {
          let nickname = ''
          let avatar = ''
          
          if (msg.fromUserId === currentUserId.value) {
            // 当前用户发送的消息
            nickname = userStore.userInfo?.nickname || '我'
            avatar = userStore.userInfo?.avatar || ''
          } else if (selectedConv.value.chatType === 1) {
            // 单聊：使用对方的昵称和头像
            nickname = selectedConv.value.targetName
            avatar = selectedConv.value.targetAvatar
          } else if (selectedConv.value.chatType === 2) {
            // 群聊：获取发送者信息
            try {
              const userInfo = await getUserInfo(msg.fromUserId, 1)
              nickname = userInfo.name
              avatar = userInfo.avatar
            } catch (error) {
              console.error('获取发送者信息失败:', error)
              nickname = `用户${msg.fromUserId}`
              avatar = ''
            }
          }
          
          return {
            // 确保id为字符串，避免JS大整数精度问题
            id: String(msg.id),
            fromUserId: msg.fromUserId,
            content: msg.content,
            msgType: msg.msgType,
            url: msg.url,
            sendTime: new Date(msg.sendTime),
            status: msg.status,
            nickname: nickname,
            avatar: avatar
          }
        })
      )
      
      // 按时间升序排列
      messages.value = messageList.sort((a, b) => new Date(a.sendTime) - new Date(b.sendTime))
      
      // 标记消息已读
      await markMessagesAsRead()
      
      // 加载完成后滚动到底部
      nextTick(() => {
        scrollToBottom()
      })
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
  }
}

// 标记消息已读
const markMessagesAsRead = async () => {
  if (!selectedConv.value || messages.value.length === 0) return
  
  try {
    // 获取所有未读消息的ID（对方发送给我的消息）
    const unreadMessageIds = messages.value
      .filter(msg => msg.fromUserId !== currentUserId.value)
      .map(msg => msg.id)
    
    if (unreadMessageIds.length > 0) {
      // 调用标记已读接口
      await request.post('/message/read', {
        messageIds: unreadMessageIds
      })
      
      console.log('标记已读成功:', unreadMessageIds)
    }
    
    // 清空会话未读数
    await clearConversationUnread()
    
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 清空会话未读数
const clearConversationUnread = async () => {
  if (!selectedConv.value) return
  
  try {
    await request.post('/conversation/clear-unread', {
      targetId: selectedConv.value.targetId,
      chatType: selectedConv.value.chatType
    })
    
    // 更新本地会话的未读数
    if (selectedConv.value) {
      selectedConv.value.unreadCount = 0
    }
    
    // 触发会话列表更新事件
    window.dispatchEvent(new CustomEvent('updateConversation', {
      detail: { 
        conversationId: selectedConv.value.id,
        action: 'clear_unread'
      }
    }))
    
    console.log('清空未读数成功')
  } catch (error) {
    console.error('清空未读数失败:', error)
  }
}

// 标记单个消息已读
const markSingleMessageAsRead = async (messageId) => {
  try {
    await request.post('/message/read', {
      messageIds: [messageId]
    })
    console.log('标记单个消息已读成功:', messageId)
  } catch (error) {
    console.error('标记单个消息已读失败:', error)
  }
}

// 监听消息更新
const watchMessages = () => {
  // 监听新消息事件
  const handleNewMessage = async (event) => {
    const { conversationId, message } = event.detail
    
    // 如果是当前会话的消息，添加到消息列表
    if (selectedConv.value && conversationId === selectedConv.value.id) {
      const exists = messages.value.find(m => m.id === message.id)
      if (!exists) {
        // 获取发送者昵称和头像
        let senderNickname = '未知用户'
        let senderAvatar = ''
        
        if (message.fromUserId === currentUserId.value) {
          senderNickname = userStore.userInfo?.nickname || '我'
          senderAvatar = userStore.userInfo?.avatar || ''
        } else if (selectedConv.value.chatType === 1) {
          // 单聊：使用对方昵称和头像
          senderNickname = selectedConv.value.targetName
          senderAvatar = selectedConv.value.targetAvatar
        } else if (selectedConv.value.chatType === 2) {
          // 群聊：需要获取发送者信息
          try {
            const userInfo = await getUserInfo(message.fromUserId, 1)
            senderNickname = userInfo.name
            senderAvatar = userInfo.avatar
          } catch (error) {
            console.error('获取发送者信息失败:', error)
            senderNickname = `用户${message.fromUserId}`
            senderAvatar = ''
          }
        }
        
        messages.value.push({
          ...message,
          nickname: senderNickname,
          avatar: senderAvatar
        })
        
        // 滚动到底部
        setTimeout(() => {
          if (messageScrollbar.value) {
            messageScrollbar.value.setScrollTop(messageScrollbar.value.wrapRef.scrollHeight)
          }
        }, 100)
        
        // 如果是别人发给我的消息，自动标记已读
        if (message.fromUserId !== currentUserId.value) {
          setTimeout(() => {
            markSingleMessageAsRead(message.id)
          }, 500)
        }
      }
    }
  }
  
  // 添加事件监听
  window.addEventListener('newMessage', handleNewMessage)
  
  // 返回清理函数
  return () => {
    window.removeEventListener('newMessage', handleNewMessage)
  }
}

// 选择会话
const selectConversation = (conv) => {
  selectedConv.value = conv
  loadHistoryMessages()
  
  // 更新消息同步管理器的当前会话
  messageSyncManager.setCurrentConversation(conv)
}

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim() || !selectedConv.value) return
  
  const messageData = {
    chatType: selectedConv.value.chatType,
    msgType: 1, // 1-文本消息
    content: inputMessage.value.trim()
  }
  
  // 根据聊天类型设置接收方
  if (selectedConv.value.chatType === 1) {
    // 单聊
    messageData.toUserId = selectedConv.value.targetId
  } else if (selectedConv.value.chatType === 2) {
    // 群聊
    messageData.groupId = selectedConv.value.targetId
  }
  
  // 生成临时ID
  const tempId = 'temp-' + Date.now()
  
  // 先添加到本地消息列表（显示发送中状态）
  const tempMessage = {
    id: tempId,
    fromUserId: currentUserId.value,
    content: messageData.content,
    msgType: 1,
    sendTime: new Date(),
    status: 0, // 0-发送中
    nickname: userStore.userInfo?.nickname || '我'
  }
  
  messages.value.push(tempMessage)
  
  // 清空输入框
  inputMessage.value = ''
  
  // 检查WebSocket连接状态
  console.log('WebSocket连接状态:', wsClient.isConnected())
  console.log('准备发送消息:', messageData)
  
  // 发送WebSocket消息，并处理ACK
  console.log('🔵 开始发送消息，临时ID:', tempId)
  wsClient.sendMessage(messageData, (error, ackData) => {
    console.log('🔵 收到ACK回调', { error, ackData, tempId })
    
    if (error) {
      console.error('❌ 消息发送失败:', error)
      // 标记消息为发送失败
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].status = -1 // -1-发送失败
      }
      return
    }
    
    // 收到ACK，用真实ID替换临时ID
    console.log('🔍 查找临时消息:', tempId, '当前消息列表:', messages.value.map(m => ({ id: m.id, content: m.content })))
    const msgIndex = messages.value.findIndex(m => m.id === tempId)
    console.log('🔍 找到索引:', msgIndex)
    
    if (msgIndex !== -1) {
      const oldId = messages.value[msgIndex].id
      // ACK中的messageId也转为字符串
      messages.value[msgIndex].id = String(ackData.messageId)
      messages.value[msgIndex].status = 1 // 1-发送成功
      console.log(`✅ 消息ID已更新: ${oldId} -> ${ackData.messageId}`)
      console.log('✅ 更新后的消息:', messages.value[msgIndex])
    } else {
      console.error('❌ 未找到临时消息:', tempId)
    }
  })
  
  // 滚动到底部
  scrollToBottom()
  
  // WebSocket发送是异步的，消息状态会通过WebSocket回调更新
  // 如果发送失败，临时消息会保持"发送中"状态，用户可以重试
}

// 语音录制开关（目前只支持单聊）
const toggleVoiceRecording = async () => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (!isRecording.value) {
    await startRecording()
  } else {
    await stopRecording()
  }
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    recordedChunks = []
    const options = {}
    // 优先使用 Chrome 支持较好的 audio/webm;codecs=opus
    if (window.MediaRecorder && MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
      options.mimeType = 'audio/webm;codecs=opus'
      mediaRecorderMimeType = options.mimeType
    } else {
      mediaRecorderMimeType = ''
    }

    mediaRecorder = new MediaRecorder(stream, options)

    mediaRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        recordedChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = async () => {
      const blobType = mediaRecorderMimeType || 'audio/webm'
      const blob = new Blob(recordedChunks, { type: blobType })
      const duration = (Date.now() - recordStartTime) / 1000
      isRecording.value = false

      // 释放麦克风
      if (mediaRecorder && mediaRecorder.stream) {
        mediaRecorder.stream.getTracks().forEach(track => track.stop())
      }

      try {
        await uploadAndSendVoice(blob, duration)
      } catch (error) {
        console.error('发送语音失败:', error)
        ElMessage.error('发送语音失败')
      }
    }

    mediaRecorder.start()
    recordStartTime = Date.now()
    isRecording.value = true
  } catch (error) {
    console.error('无法开始录音:', error)
    ElMessage.error('无法访问麦克风，请检查浏览器权限')
  }
}

const stopRecording = async () => {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
  }
}

const uploadAndSendVoice = async (blob, duration) => {
  if (!blob || blob.size === 0) {
    ElMessage.warning('录音太短，未发送')
    return
  }

  const formData = new FormData()
  formData.append('file', blob, 'voice-message.webm')

  const res = await request.post('/files/upload/audio', formData)
  const { url, size } = res.data || {}

  if (!url) {
    throw new Error('上传语音失败：未返回URL')
  }

  await sendVoiceMessage(url, duration, size)
}

const onSelectMedia = () => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (mediaInputRef.value) {
    mediaInputRef.value.click()
  }
}

const handleMediaChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (file.type.startsWith('image/')) {
    try {
      await uploadAndSendImage(file)
    } catch (error) {
      console.error('发送图片失败:', error)
      ElMessage.error('发送图片失败')
    }
    return
  }
  if (file.type.startsWith('video/')) {
    try {
      await uploadAndSendVideo(file)
    } catch (error) {
      console.error('发送视频失败:', error)
      ElMessage.error('发送视频失败')
    }
    return
  }
  ElMessage.warning('只支持发送图片或视频文件')
}

const uploadAndSendImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/image', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传图片失败：未返回URL')
  }

  await sendMediaMessage('image', url, size, fileName || file.name)
}

const uploadAndSendVideo = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/video', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传视频失败：未返回URL')
  }

  await sendMediaMessage('video', url, size, fileName || file.name)
}

const onEmojiPanelShow = async () => {
  try {
    const res = await request.get('/emoji/list')
    let list = Array.isArray(res.data) ? res.data : []
    // 按添加时间倒序排列（id 越大越靠前）
    list = list.slice().sort((a, b) => {
      const idA = Number(a && a.id ? a.id : 0)
      const idB = Number(b && b.id ? b.id : 0)
      return idB - idA
    })
    favoriteEmojis.value = list
  } catch (error) {
    console.error('加载收藏表情失败:', error)
    ElMessage.error('加载收藏表情失败')
  }
}

const onSelectEmojiFile = () => {
  if (!emojiFileInputRef.value) return
  emojiFileInputRef.value.click()
}

const handleEmojiFileChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  try {
    await uploadAndAddEmoji(file)
  } catch (error) {
    console.error('添加表情失败:', error)
    ElMessage.error('添加表情失败')
  }
}

const uploadAndAddEmoji = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/image', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传表情失败：未返回URL')
  }

  const emojiRes = await request.post('/emoji', {
    url,
    fileName: fileName || file.name,
    size
  })

  const emoji = emojiRes.data
  if (emoji) {
    const exists = favoriteEmojis.value.find(item => item.id === emoji.id)
    if (!exists) {
      favoriteEmojis.value.unshift(emoji)
    }
  }
  ElMessage.success('已添加到收藏表情')
}

const sendEmoji = async (emoji) => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  await sendMediaMessage('image', emoji.url, emoji.size, emoji.fileName)
}

const removeEmoji = async (emoji) => {
  try {
    await request.delete(`/emoji/${emoji.id}`)
    const index = favoriteEmojis.value.findIndex(item => item.id === emoji.id)
    if (index !== -1) {
      favoriteEmojis.value.splice(index, 1)
    }
    ElMessage.success('已删除表情')
  } catch (error) {
    console.error('删除表情失败:', error)
    ElMessage.error('删除表情失败')
  }
}

const favoriteEmojiFromMessage = async (message) => {
  if (!message || message.msgType !== 2) return
  const url = message.url || getMediaUrlFromContent(message)
  if (!url) {
    ElMessage.error('找不到图片地址')
    return
  }

  let fileName = '表情'
  let size = undefined
  try {
    if (message.content) {
      const obj = typeof message.content === 'string' ? JSON.parse(message.content) : message.content
      if (obj) {
        if (obj.fileName) fileName = obj.fileName
        if (obj.size) size = obj.size
      }
    }
  } catch (error) {
    console.warn('解析图片消息内容失败:', error)
  }

  try {
    const res = await request.post('/emoji', {
      url,
      fileName,
      size
    })
    const emoji = res.data
    if (emoji) {
      const exists = favoriteEmojis.value.find(item => item.id === emoji.id)
      if (!exists) {
        favoriteEmojis.value.unshift(emoji)
      }
    }
    ElMessage.success('已收藏为表情')
  } catch (error) {
    console.error('收藏表情失败:', error)
    ElMessage.error('收藏表情失败')
  }
}

const onSelectFile = () => {
  if (!selectedConv.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

const handleFileChange = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  try {
    await uploadAndSendFile(file)
  } catch (error) {
    console.error('发送文件失败:', error)
    ElMessage.error('发送文件失败')
  }
}

const uploadAndSendFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  const res = await request.post('/files/upload/file', formData)
  const { url, size, fileName } = res.data || {}

  if (!url) {
    throw new Error('上传文件失败：未返回URL')
  }

  await sendMediaMessage('file', url, size, fileName || file.name)
}

const sendMediaMessage = async (type, url, size, fileName) => {
  if (!selectedConv.value) return

  let msgType
  if (type === 'image') {
    msgType = 2
  } else if (type === 'video') {
    msgType = 3
  } else if (type === 'file') {
    msgType = 4
  } else {
    msgType = 1
  }

  const meta = {
    url,
    size,
    fileName,
    type
  }
  const content = JSON.stringify(meta)

  const messageData = {
    chatType: selectedConv.value.chatType,
    msgType,
    content,
    url
  }

  if (selectedConv.value.chatType === 1) {
    messageData.toUserId = selectedConv.value.targetId
  } else if (selectedConv.value.chatType === 2) {
    messageData.groupId = selectedConv.value.targetId
  }

  const tempId = 'temp-' + Date.now()

  const tempMessage = {
    id: tempId,
    fromUserId: currentUserId.value,
    content,
    url,
    msgType,
    sendTime: new Date(),
    status: 0,
    nickname: userStore.userInfo?.nickname || '我'
  }

  messages.value.push(tempMessage)
  scrollToBottom()

  console.log('🔵 开始发送媒体消息，临时ID:', tempId, '类型:', type)
  wsClient.sendMessage(messageData, (error, ackData) => {
    console.log('🔵 收到媒体ACK回调', { error, ackData, tempId })

    if (error) {
      console.error('❌ 媒体消息发送失败:', error)
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].status = -1
      }
      return
    }

    const msgIndex = messages.value.findIndex(m => m.id === tempId)
    if (msgIndex !== -1) {
      const oldId = messages.value[msgIndex].id
      messages.value[msgIndex].id = String(ackData.messageId)
      messages.value[msgIndex].status = 1
      console.log(`✅ 媒体消息ID已更新: ${oldId} -> ${ackData.messageId}`)
    } else {
      console.error('❌ 未找到临时媒体消息:', tempId)
    }
  })
}

const sendVoiceMessage = async (url, duration, size) => {
  if (!selectedConv.value) return

  const meta = {
    url,
    duration,
    size
  }
  const content = JSON.stringify(meta)

  const messageData = {
    chatType: selectedConv.value.chatType,
    msgType: 5, // 5-语音消息
    content,
    url
  }

  if (selectedConv.value.chatType === 1) {
    messageData.toUserId = selectedConv.value.targetId
  } else if (selectedConv.value.chatType === 2) {
    messageData.groupId = selectedConv.value.targetId
  }

  const tempId = 'temp-' + Date.now()

  const tempMessage = {
    id: tempId,
    fromUserId: currentUserId.value,
    content,
    url,
    msgType: 5,
    sendTime: new Date(),
    status: 0,
    nickname: userStore.userInfo?.nickname || '我'
  }

  messages.value.push(tempMessage)
  scrollToBottom()

  console.log('🔵 开始发送语音消息，临时ID:', tempId)
  wsClient.sendMessage(messageData, (error, ackData) => {
    console.log('🔵 收到语音ACK回调', { error, ackData, tempId })

    if (error) {
      console.error('❌ 语音消息发送失败:', error)
      const msgIndex = messages.value.findIndex(m => m.id === tempId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].status = -1
      }
      return
    }

    const msgIndex = messages.value.findIndex(m => m.id === tempId)
    if (msgIndex !== -1) {
      const oldId = messages.value[msgIndex].id
      messages.value[msgIndex].id = String(ackData.messageId)
      messages.value[msgIndex].status = 1
      console.log(`✅ 语音消息ID已更新: ${oldId} -> ${ackData.messageId}`)
    } else {
      console.error('❌ 未找到临时语音消息:', tempId)
    }
  })
}

const getVoiceMeta = (message) => {
  if (!message || message.msgType !== 5) return {}
  try {
    if (typeof message.content === 'string') {
      return JSON.parse(message.content) || {}
    }
    return message.content || {}
  } catch (error) {
    console.warn('解析语音消息内容失败:', error)
    return {}
  }
}

const getMediaUrlFromContent = (message) => {
  if (!message || !message.content) return ''
  try {
    if (typeof message.content === 'string') {
      const obj = JSON.parse(message.content)
      return obj && obj.url ? obj.url : ''
    }
    if (message.content && message.content.url) {
      return message.content.url
    }
  } catch (error) {
    console.warn('解析媒体消息内容失败:', error)
  }
  return ''
}

const getFileName = (message) => {
  if (!message) return '文件'
  try {
    if (typeof message.content === 'string' && message.content) {
      const obj = JSON.parse(message.content)
      if (obj && obj.fileName) {
        return obj.fileName
      }
    } else if (message.content && message.content.fileName) {
      return message.content.fileName
    }
  } catch (error) {
    console.warn('解析文件消息内容失败:', error)
  }
  return '文件'
}

const getVoiceDurationText = (message) => {
  const meta = getVoiceMeta(message)
  if (meta && meta.duration) {
    const sec = Math.max(1, Math.round(meta.duration))
    return `${sec}″`
  }
  return ''
}

const formatVoiceSecond = (sec) => {
  if (!sec || sec <= 0) return '0″'
  const s = Math.floor(sec)
  return `${s}″`
}

const previewImage = (message) => {
  const url = message.url || getMediaUrlFromContent(message)
  if (!url) {
    ElMessage.error('找不到图片地址')
    return
  }
  window.open(url, '_blank')
}

const playVoice = (message) => {
  if (!message || message.msgType !== 5) return

  let url = message.url
  if (!url) {
    const meta = getVoiceMeta(message)
    url = meta.url
  }

  if (!url) {
    ElMessage.error('找不到语音地址')
    return
  }

  // 再次点击当前播放的语音，认为是暂停
  if (playingMessageId.value === message.id && currentAudio) {
    currentAudio.pause()
    currentAudio = null
    playingMessageId.value = null
    playingCurrentTime.value = 0
    return
  }

  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }

  const audio = new Audio()

  // 检测浏览器是否支持当前语音格式（主要是 audio/webm / opus）
  const canPlayWebm = audio.canPlayType('audio/webm') || audio.canPlayType('audio/webm; codecs=opus')
  if (!canPlayWebm) {
    ElMessage.error('当前浏览器不支持语音格式，请使用最新版 Chrome 再试')
    return
  }

  const meta = getVoiceMeta(message)
  if (meta && meta.duration) {
    playingTotalDuration.value = meta.duration
  } else {
    playingTotalDuration.value = 0
  }
  playingCurrentTime.value = 0

  audio.src = url
  currentAudio = audio
  playingMessageId.value = message.id

  audio.play().catch(error => {
    console.error('播放语音失败:', error)
    ElMessage.error('播放语音失败')
    playingMessageId.value = null
    currentAudio = null
    playingCurrentTime.value = 0
  })

  audio.ontimeupdate = () => {
    playingCurrentTime.value = audio.currentTime
  }

  audio.onended = () => {
    playingMessageId.value = null
    currentAudio = null
    playingCurrentTime.value = 0
  }
}

// 处理键盘事件
const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 跳转到通讯录
const goToContacts = () => {
  router.push('/contacts')
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 开始拖拽调整宽度
const startResize = (e) => {
  isResizing.value = true
  const startX = e.clientX
  const startWidth = conversationListWidth.value
  
  const handleMouseMove = (e) => {
    if (!isResizing.value) return
    
    const deltaX = e.clientX - startX
    const newWidth = Math.max(minWidth, Math.min(maxWidth, startWidth + deltaX))
    conversationListWidth.value = newWidth
  }
  
  const handleMouseUp = () => {
    isResizing.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

// 重置宽度
const resetWidth = () => {
  conversationListWidth.value = 280
}

// 显示右键菜单
const showContextMenu = (event, conv) => {
  // 阻止默认右键菜单
  event.preventDefault()
  // 这里可以添加自定义右键菜单逻辑
}

// 处理会话操作
const handleConvAction = async ({ action, conv }) => {
  try {
    switch (action) {
      case 'pin':
        await pinConversation(conv.id)
        conv.isPinned = true
        ElMessage.success('会话已置顶')
        break
      case 'unpin':
        await unpinConversation(conv.id)
        conv.isPinned = false
        ElMessage.success('已取消置顶')
        break
      case 'hide':
        await hideConversation(conv.id)
        conv.isHidden = true
        ElMessage.success('会话已隐藏')
        // 从列表中移除隐藏的会话
        const index = conversations.value.findIndex(c => c.id === conv.id)
        if (index > -1) {
          conversations.value.splice(index, 1)
        }
        break
      case 'delete':
        // 根据聊天类型显示不同的提示
        const deleteMessage = conv.chatType === 1 
          ? '确定要删除这个会话吗？\n⚠️ 删除后你将看不到所有聊天记录，但对方不受影响。'
          : '确定要删除这个会话吗？\n删除后会话和聊天记录将从你的列表中消失。'
        
        await ElMessageBox.confirm(deleteMessage, '确认删除', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: false
        })
        await deleteConversation(conv.id)
        // 从列表中移除删除的会话
        const delIndex = conversations.value.findIndex(c => c.id === conv.id)
        if (delIndex > -1) {
          conversations.value.splice(delIndex, 1)
        }
        ElMessage.success('会话已删除')
        break
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// API 调用方法
const pinConversation = async (conversationId) => {
  await request.post('/conversation/pin', { conversationId })
}

const unpinConversation = async (conversationId) => {
  await request.post('/conversation/unpin', { conversationId })
}

const hideConversation = async (conversationId) => {
  await request.post('/conversation/hide', { conversationId })
}

const deleteConversation = async (conversationId) => {
  await request.delete(`/conversation/${conversationId}`)
}

// 检查是否为发送中的消息
const isSendingMessage = (message) => {
  return message.status === 0 && String(message.id).startsWith('temp-')
}

// 检查是否为已撤回的消息
const isRecalledMessage = (message) => {
  return message.status === 0 && !String(message.id).startsWith('temp-')
}

// 撤回消息气泡文案
// 单聊：自己 -> “你 撤回了一条消息”，对方 -> “对方 撤回了一条消息”
// 群聊：自己 -> “你 撤回了一条消息”，他人 -> “{昵称} 撤回了一条消息”
const getRecalledText = (message) => {
  // 自己撤回
  if (message.fromUserId === currentUserId.value) {
    return '你 撤回了一条消息'
  }

  // 对方撤回 - 单聊：固定显示“对方”
  if (selectedConv.value && selectedConv.value.chatType === 1) {
    return '对方 撤回了一条消息'
  }

  // 群聊：显示昵称
  const name = message.nickname || '对方'
  return `${name} 撤回了一条消息`
}

// 检查消息是否可以撤回（5分钟内）
const canRecall = (message) => {
  // 检查消息状态：必须是正常状态(1)，且不是临时消息
  if (message.status !== 1) return false
  
  // 检查是否有真实的消息ID（排除临时消息）
  if (!message.id || String(message.id).startsWith('temp-')) return false
  
  // 检查发送时间是否有效
  if (!message.sendTime) return false
  
  const now = new Date()
  const sendTime = new Date(message.sendTime)
  
  // 检查时间是否有效
  if (isNaN(sendTime.getTime())) return false
  
  const diffMinutes = (now - sendTime) / (1000 * 60)
  
  return diffMinutes <= 5 // 5分钟内可以撤回
}

// 撤回消息
const recallMessage = async (message) => {
  try {
    await ElMessageBox.confirm('确定要撤回这条消息吗？', '撤回消息', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.post('/message/recall', { messageId: message.id })
    
    // 更新本地消息状态
    message.status = 0
    message.recallTime = new Date()
    
    ElMessage.success('消息已撤回')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回消息失败:', error)
      ElMessage.error('撤回消息失败')
    }
  }
}

// 删除消息
const deleteMessage = async (message) => {
  try {
    await ElMessageBox.confirm('删除后仅自己不可见，对方仍然可以看到。确定要删除这条消息吗？', '删除消息', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteMessageApi(message.id)
    
    // 从本地消息列表中移除
    const index = messages.value.findIndex(m => m.id === message.id)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
    
    ElMessage.success('消息已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
      ElMessage.error('删除消息失败')
    }
  }
}

// 设置消息同步管理器
const setupMessageSyncManager = () => {
  // 设置当前会话
  if (selectedConv.value) {
    messageSyncManager.setCurrentConversation(selectedConv.value)
  }
  
  // 设置消息更新回调
  messageSyncManager.setMessageUpdateCallback(handleMessageUpdate)
  
  // 设置会话更新回调
  messageSyncManager.setConversationUpdateCallback(loadConversationList)
  
  // 监听WebSocket连接状态
  wsClient.onConnect(() => {
    messageSyncManager.onWebSocketConnected()
  })
  
  wsClient.onDisconnect(() => {
    messageSyncManager.onWebSocketDisconnected()
  })
  
  // 监听WebSocket消息
  wsClient.onMessage((data) => {
    if (data.type && ['NEW_MESSAGE', 'MESSAGE_RECALLED', 'MESSAGES_READ', 'CONVERSATION_UPDATED'].includes(data.type)) {
      messageSyncManager.handleWebSocketMessage(data)
    }
  })
}

// 处理消息更新
const handleMessageUpdate = (type, data) => {
  switch (type) {
    case 'NEW_MESSAGE':
      handleNewMessageUpdate(data)
      break
    case 'MESSAGE_RECALLED':
      handleMessageRecalledUpdate(data)
      break
    case 'MESSAGES_READ':
      handleMessagesReadUpdate(data)
      break
    case 'SYNC_MESSAGES':
      handleSyncMessages(data)
      break
  }
}

// 处理新消息更新
const handleNewMessageUpdate = (data) => {
  // 如果是当前会话的消息，添加到消息列表
  if (selectedConv.value && data.conversationId === selectedConv.value.id) {
    const newMessage = {
      // 通知中的messageId也转为字符串
      id: String(data.messageId),
      fromUserId: data.fromUserId,
      content: data.content,
      msgType: data.msgType,
      url: data.url,
      sendTime: new Date(),
      status: 1,
      nickname: data.fromUserId === currentUserId.value ? 
        (userStore.userInfo?.nickname || '我') : 
        selectedConv.value.targetName
    }
    
    messages.value.push(newMessage)
    scrollToBottom()
  }
}

// 处理消息撤回更新
const handleMessageRecalledUpdate = (data) => {
  const targetId = String(data.messageId)
  const message = messages.value.find(m => String(m.id) === targetId)
  if (message) {
    message.status = 0
    message.recallTime = data.recallTime || new Date()
  }
}

// 处理消息已读更新
const handleMessagesReadUpdate = (data) => {
  // 可以在这里更新消息的已读状态显示
  console.log('消息已读更新:', data)
}

// 处理同步消息
const handleSyncMessages = (data) => {
  if (selectedConv.value && data.conversationId === selectedConv.value.id) {
    loadHistoryMessages()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageScrollbar.value) {
      const scrollContainer = messageScrollbar.value.wrapRef
      if (scrollContainer) {
        scrollContainer.scrollTop = scrollContainer.scrollHeight
      }
    }
  })
}

</script>

<style scoped>
.chat-container {
  display: flex;
  width: 100%;
  height: 100%;
  background: #f0f0f0;
  overflow: hidden;
}

/* 会话列表 */
.conversation-list {
  background: white;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 可拖拽分割线 */
.resizer {
  width: 4px;
  background: #e0e0e0;
  cursor: col-resize;
  position: relative;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.resizer:hover {
  background: #409eff;
}

.resizer::after {
  content: '';
  position: absolute;
  left: -2px;
  right: -2px;
  top: 0;
  bottom: 0;
}

.list-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.conv-scrollbar {
  flex: 1;
}

.empty-state {
  padding: 60px 20px;
}

.conv-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f5f5f5;
  position: relative;
}

.conv-item:hover {
  background: #f5f7fa;
}

.conv-item:hover .conv-actions {
  opacity: 1;
}

.conv-item.active {
  background: #ecf5ff;
}

.conv-item.pinned {
  background: #f0f9ff;
  border-left: 3px solid #409eff;
}

.conv-item.hidden {
  opacity: 0.5;
}

.conv-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.conv-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.conv-time {
  font-size: 12px;
  color: #909399;
}

.conv-bottom {
  font-size: 13px;
  color: #909399;
}

.last-message {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 会话操作按钮 */
.conv-actions {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.action-btn {
  padding: 4px !important;
  min-height: auto !important;
  color: #909399 !important;
}

.action-btn:hover {
  color: #409eff !important;
  background: rgba(64, 158, 255, 0.1) !important;
}

/* 置顶图标 */
.pin-icon {
  color: #409eff;
  font-size: 12px;
  margin-right: 4px;
}

/* 聊天窗口 */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.no-chat-selected {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-header {
  height: 60px;
  padding: 0 20px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.target-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

/* 消息列表 */
.message-list {
  flex: 1;
  padding: 16px 24px;
  min-height: 0; /* 确保滚动正常工作 */
}

.message-content {
  min-height: 100%;
}

.date-divider {
  text-align: center;
  margin: 20px 0;
}

.date-divider span {
  padding: 4px 12px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  font-size: 12px;
  color: #909399;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;
}

.message-item.is-mine {
  flex-direction: row-reverse;
}

.message-wrapper {
  max-width: 70%;
}

.message-item.is-mine .message-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.message-item.is-mine .message-info {
  flex-direction: row-reverse;
}

.message-bubble {
  padding: 16px 20px !important;
  background: white;
  border-radius: 16px !important;
  font-size: 16px !important;
  line-height: 1.5;
  color: #303133;
  word-wrap: break-word;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12) !important;
  min-width: 80px !important;
}

.message-item.is-mine .message-bubble {
  background: #95ec69;
  color: #303133;
}

/* 撤回消息样式 */
.message-bubble.recalled {
  background: #f5f5f5 !important;
  color: #909399 !important;
  font-style: italic;
  text-align: center;
}

.recalled-text {
  font-size: 14px;
}

/* 发送中消息样式 */
.message-bubble.sending {
  opacity: 0.7;
  position: relative;
}

.sending-indicator {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

/* 消息操作按钮 */
.message-actions {
  position: absolute;
  top: -12px;
  right: -8px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-bubble:hover .message-actions {
  opacity: 1;
}

.message-actions .action-btn {
  font-size: 12px !important;
  padding: 4px 10px !important;
  height: auto !important;
  background: rgba(0, 0, 0, 0.7) !important;
  color: white !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
}

.message-actions .action-btn:hover {
  background: rgba(0, 0, 0, 0.85) !important;
}

.message-actions .delete-btn {
  background: rgba(245, 108, 108, 0.9) !important;
}

.message-actions .delete-btn:hover {
  background: rgba(245, 108, 108, 1) !important;
}

.message-bubble {
  position: relative;
}

.voice-message {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.voice-message.playing .voice-label {
  font-weight: 600;
}

.voice-label {
  font-size: 14px;
}

.voice-duration {
  font-size: 14px;
  color: #606266;
}

.image-message {
  max-width: 200px;
  border-radius: 6px;
  cursor: pointer;
}

.video-message {
  max-width: 260px;
  border-radius: 6px;
}

.empty-messages {
  padding: 60px 20px;
}

/* 输入区域 */
.input-area {
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 16px 24px;
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.input-box {
  margin-bottom: 10px;
}

.input-box :deep(.el-textarea__inner) {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.input-box :deep(.el-textarea__inner):focus {
  border-color: #409eff;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.emoji-panel {
  padding: 8px;
  max-height: 260px;
  overflow-y: auto;
}

.emoji-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emoji-item {
  width: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.emoji-image {
  width: 40px;
  height: 40px;
  object-fit: contain;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  background: #fff;
}

.emoji-delete-btn {
  padding: 0 4px !important;
  min-height: auto !important;
  font-size: 12px !important;
}

.text-count {
  font-size: 12px;
  color: #909399;
}
</style>
