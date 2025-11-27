import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // 会话列表
  const conversations = ref([])
  
  // 当前选中的会话
  const currentConversation = ref(null)
  
  // 消息列表
  const messages = ref({})
  
  // WebSocket 连接状态
  const connected = ref(false)

  // 设置会话列表
  const setConversations = (list) => {
    conversations.value = list
  }

  // 设置当前会话
  const setCurrentConversation = (conversation) => {
    currentConversation.value = conversation
  }

  // 添加消息
  const addMessage = (conversationId, message) => {
    if (!messages.value[conversationId]) {
      messages.value[conversationId] = []
    }
    messages.value[conversationId].push(message)
  }

  // 设置消息列表
  const setMessages = (conversationId, messageList) => {
    messages.value[conversationId] = messageList
  }

  // 更新未读数
  const updateUnreadCount = (conversationId, count) => {
    const conversation = conversations.value.find(c => c.conversationId === conversationId)
    if (conversation) {
      conversation.unreadCount = count
    }
  }

  // 清空未读数
  const clearUnreadCount = (conversationId) => {
    updateUnreadCount(conversationId, 0)
  }

  return {
    conversations,
    currentConversation,
    messages,
    connected,
    setConversations,
    setCurrentConversation,
    addMessage,
    setMessages,
    updateUnreadCount,
    clearUnreadCount
  }
})
