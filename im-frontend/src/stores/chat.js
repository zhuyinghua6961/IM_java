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
  
  // 设置会话的@状态（被@时设为true）
  const setHasAtMe = (conversationId, hasAtMe) => {
    const conversation = conversations.value.find(c => c.id === conversationId)
    if (conversation) {
      conversation.hasAtMe = hasAtMe
    }
    // 持久化到 localStorage
    const atMeMap = JSON.parse(localStorage.getItem('hasAtMeMap') || '{}')
    if (hasAtMe) {
      atMeMap[conversationId] = true
    } else {
      delete atMeMap[conversationId]
    }
    localStorage.setItem('hasAtMeMap', JSON.stringify(atMeMap))
  }
  
  // 清除会话的@状态
  const clearHasAtMe = (conversationId) => {
    setHasAtMe(conversationId, false)
  }
  
  // 从 localStorage 恢复@状态
  const restoreHasAtMe = () => {
    const atMeMap = JSON.parse(localStorage.getItem('hasAtMeMap') || '{}')
    for (const conv of conversations.value) {
      if (atMeMap[conv.id]) {
        conv.hasAtMe = true
      }
    }
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
    clearUnreadCount,
    setHasAtMe,
    clearHasAtMe,
    restoreHasAtMe
  }
})
