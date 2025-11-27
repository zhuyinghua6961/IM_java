import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'

class WebSocketClient {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000
    this.messageCallbacks = new Map()
  }

  // 连接WebSocket
  connect() {
    const userStore = useUserStore()
    const token = userStore.token
    
    if (!token) {
      console.error('No token found, cannot connect to WebSocket')
      return
    }

    const socket = new SockJS(`http://localhost:8082/ws?token=${token}`)
    this.stompClient = Stomp.over(socket)
    
    // 禁用调试日志
    this.stompClient.debug = () => {}

    this.stompClient.connect(
      {},
      (frame) => {
        console.log('WebSocket connected:', frame)
        this.connected = true
        this.reconnectAttempts = 0
        
        // 订阅消息
        this.subscribeToMessages()
        
        ElMessage.success('连接成功')
      },
      (error) => {
        console.error('WebSocket connection error:', error)
        this.connected = false
        this.handleReconnect()
      }
    )
  }

  // 订阅消息
  subscribeToMessages() {
    const userStore = useUserStore()
    const chatStore = useChatStore()
    const userId = userStore.userInfo?.userId

    if (!userId) {
      console.error('No user ID found')
      return
    }

    // 订阅私人消息
    this.stompClient.subscribe(`/user/queue/messages`, (message) => {
      const data = JSON.parse(message.body)
      console.log('收到消息:', data)
      
      // 添加到消息列表
      const conversationId = `${data.chatType}-${data.fromUserId}`
      const messageObj = {
        id: data.messageId,
        fromUserId: data.fromUserId,
        content: data.content,
        msgType: data.msgType,
        sendTime: new Date(data.timestamp),
        status: 1
      }
      
      chatStore.addMessage(conversationId, messageObj)
      
      // 触发自定义事件，通知聊天界面更新
      window.dispatchEvent(new CustomEvent('newMessage', {
        detail: { conversationId, message: messageObj }
      }))
      
      // 更新会话列表的未读数
      this.updateConversationUnread(conversationId, data.fromUserId)
    })

    // 订阅ACK确认
    this.stompClient.subscribe(`/queue/ack-${this.getSessionId()}`, (ack) => {
      const data = JSON.parse(ack.body)
      console.log('消息发送成功:', data)
      
      // 执行回调
      const callback = this.messageCallbacks.get(data.messageId)
      if (callback) {
        callback(null, data)
        this.messageCallbacks.delete(data.messageId)
      }
    })

    // 订阅错误消息
    this.stompClient.subscribe(`/queue/error-${this.getSessionId()}`, (error) => {
      const data = JSON.parse(error.body)
      console.error('WebSocket错误:', data)
      ElMessage.error(data.message)
    })
  }

  // 发送消息
  sendMessage(messageData, callback) {
    if (!this.connected || !this.stompClient) {
      ElMessage.error('WebSocket未连接')
      if (callback) callback(new Error('WebSocket未连接'))
      return
    }

    try {
      // 生成临时消息ID用于回调
      const tempId = Date.now()
      if (callback) {
        this.messageCallbacks.set(tempId, callback)
      }

      this.stompClient.send('/app/message', {}, JSON.stringify(messageData))
      console.log('发送消息:', messageData)
    } catch (error) {
      console.error('发送消息失败:', error)
      if (callback) callback(error)
    }
  }

  // 断开连接
  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect()
      this.connected = false
      console.log('WebSocket disconnected')
    }
  }

  // 重连处理
  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      
      setTimeout(() => {
        this.connect()
      }, this.reconnectInterval)
    } else {
      ElMessage.error('WebSocket连接失败，请刷新页面重试')
    }
  }

  // 获取会话ID（简化版）
  getSessionId() {
    return 'session-' + Date.now()
  }

  // 更新会话未读数
  updateConversationUnread(conversationId, fromUserId) {
    // 触发会话列表更新事件
    window.dispatchEvent(new CustomEvent('updateConversation', {
      detail: { 
        conversationId, 
        fromUserId,
        action: 'increment_unread'
      }
    }))
  }

  // 检查连接状态
  isConnected() {
    return this.connected
  }
}

// 创建单例
const wsClient = new WebSocketClient()

export default wsClient
