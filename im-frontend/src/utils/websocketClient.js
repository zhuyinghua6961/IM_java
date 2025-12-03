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
    this.pendingCallbacks = [] // 待处理的消息回调队列
    this.sessionId = null // 固定的sessionId，连接时生成一次
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
        console.log('✅ WebSocket connected successfully')
        this.connected = true
        this.reconnectAttempts = 0
        
        // 从STOMP响应头获取真实的session ID
        // 这个session ID就是Spring WebSocket内部使用的ID
        const headers = frame.headers || {}
        this.sessionId = headers.session || headers['session-id']
        console.log('🔵 WebSocket sessionId:', this.sessionId)
        console.log('🔵 All headers:', JSON.stringify(headers))
        
        if (!this.sessionId) {
          console.error('❌ 无法获取sessionId，ACK可能无法接收')
        }
        
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
        // 确保id为字符串，避免JS大整数精度问题
        id: String(data.messageId),
        fromUserId: data.fromUserId,
        content: data.content,
        url: data.url,
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

    // 订阅ACK确认 - 使用session-specific的queue
    const ackQueue = `/queue/ack-${this.sessionId}`
    console.log('🔵 订阅ACK队列:', ackQueue)
    this.stompClient.subscribe(ackQueue, (ack) => {
      const data = JSON.parse(ack.body)
      console.log('🟢 收到ACK确认:', data)
      console.log('🟢 当前待处理回调数量:', this.pendingCallbacks ? this.pendingCallbacks.length : 0)
      
      // 从待处理队列中取出第一个回调并执行
      // 因为消息是按顺序发送和确认的，所以使用FIFO队列
      if (this.pendingCallbacks && this.pendingCallbacks.length > 0) {
        const pending = this.pendingCallbacks.shift()
        console.log('🟢 执行回调，剩余回调数:', this.pendingCallbacks.length)
        if (pending.callback) {
          pending.callback(null, data)
          console.log('✅ ACK回调已执行, messageId:', data.messageId)
        }
      } else {
        console.warn('⚠️ 收到ACK但没有待处理的回调')
      }
    })

    // 订阅错误消息 - 使用session-specific的queue
    const errorQueue = `/queue/error-${this.sessionId}`
    console.log('🔵 订阅错误队列:', errorQueue)
    this.stompClient.subscribe(errorQueue, (error) => {
      const data = JSON.parse(error.body)
      console.error('WebSocket错误:', data)
      
      if (data.type === 'BLOCKED') {
        // 被拉黑的情况
        ElMessage.error({
          message: data.message || '对方已将你拉黑，无法发送消息',
          duration: 5000
        })
        // 触发消息发送失败事件，让聊天页面处理
        window.dispatchEvent(new CustomEvent('messageSendBlocked', {
          detail: {
            blockedByUserId: data.blockedByUserId,
            message: data.message
          }
        }))
        // 清空待处理的回调（标记为失败）
        if (this.pendingCallbacks && this.pendingCallbacks.length > 0) {
          const pending = this.pendingCallbacks.shift()
          if (pending && pending.callback) {
            pending.callback(new Error('BLOCKED'), null)
          }
        }
      } else {
        ElMessage.error(data.message)
      }
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
      // 生成临时消息ID，并添加到回调映射
      // 注意：由于我们无法预知后端生成的messageId，
      // 我们需要在ACK时通过其他方式匹配回调
      // 这里先存储一个待处理的回调队列
      const tempId = 'temp-' + Date.now()
      if (callback) {
        // 使用一个特殊的key来存储等待ACK的回调
        if (!this.pendingCallbacks) {
          this.pendingCallbacks = []
        }
        this.pendingCallbacks.push({
          tempId: tempId,
          callback: callback,
          timestamp: Date.now()
        })
        console.log('🔵 回调已加入队列, 当前队列长度:', this.pendingCallbacks.length)
      }

      this.stompClient.send('/app/message', {}, JSON.stringify(messageData))
      console.log('发送消息:', messageData)
      
      // 返回tempId供调用者使用
      return tempId
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

  // 获取会话ID（返回固定的sessionId）
  getSessionId() {
    if (!this.sessionId) {
      // 如果还没有生成，先生成一个（理论上不应该走到这里）
      this.sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9)
      console.warn('⚠️ sessionId未初始化，临时生成:', this.sessionId)
    }
    return this.sessionId
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
