import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { ElMessage, ElNotification } from 'element-plus'
import { useChatStore } from '@/stores/chat'

class WebSocketClient {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.messageHandlers = []
    this.connectHandlers = []
    this.disconnectHandlers = []
    this.sessionId = null
    this.pendingCallbacks = []
  }

  // 连接 WebSocket
  connect(token, onMessageReceived) {
    // 保存参数供重连使用
    this.currentToken = token
    this.currentMessageHandler = onMessageReceived
    
    // 使用相对路径，让Vite代理转发到消息服务
    const socket = new SockJS(`/ws?token=${token}`)
    this.stompClient = Stomp.over(socket)
    
    // 禁用调试日志
    this.stompClient.debug = null

    this.stompClient.connect(
      {},
      () => {
        console.log('WebSocket 连接成功')
        this.connected = true
        this.reconnectAttempts = 0
        
        const chatStore = useChatStore()
        chatStore.connected = true
        
        // 触发连接处理器
        this.connectHandlers.forEach(handler => handler())
        
        // 获取 sessionId（从 socket 连接中提取）
        try {
          const url = socket._transport.url
          const match = url.match(/\/ws\/\d+\/([^/]+)\/websocket/)
          if (match) {
            this.sessionId = match[1]
            console.log('🔵 获取到 sessionId:', this.sessionId)
          }
        } catch (e) {
          // 备用方案：生成随机 sessionId
          this.sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9)
          console.log('🔵 生成备用 sessionId:', this.sessionId)
        }
        
        // 订阅 ACK 确认队列
        const ackQueue = `/queue/ack-${this.sessionId}`
        console.log('🔵 订阅ACK队列:', ackQueue)
        this.stompClient.subscribe(ackQueue, (ack) => {
          const data = JSON.parse(ack.body)
          console.log('🟢 收到ACK确认:', data)
          
          if (this.pendingCallbacks && this.pendingCallbacks.length > 0) {
            const pending = this.pendingCallbacks.shift()
            if (pending && pending.callback) {
              pending.callback(null, data)
              console.log('✅ ACK回调已执行, messageId:', data.messageId)
            }
          }
        })
        
        // 订阅错误消息队列
        const errorQueue = `/queue/error-${this.sessionId}`
        console.log('🔵 订阅错误队列:', errorQueue)
        this.stompClient.subscribe(errorQueue, (error) => {
          const data = JSON.parse(error.body)
          console.error('❌ WebSocket错误:', data)
          
          if (data.type === 'BLOCKED') {
            // 被拉黑的情况
            ElMessage.error({
              message: data.message || '对方已将你拉黑，无法发送消息',
              duration: 5000
            })
            // 触发回调，传递真实的消息ID（后端已保存）
            if (this.pendingCallbacks && this.pendingCallbacks.length > 0) {
              const pending = this.pendingCallbacks.shift()
              if (pending && pending.callback) {
                // 传递错误和带有messageId的数据
                const blockedError = new Error('BLOCKED')
                blockedError.messageId = data.messageId
                pending.callback(blockedError, { messageId: data.messageId, status: -1 })
              }
            }
          } else {
            ElMessage.error(data.message || '消息发送失败')
            // 普通错误也触发回调
            if (this.pendingCallbacks && this.pendingCallbacks.length > 0) {
              const pending = this.pendingCallbacks.shift()
              if (pending && pending.callback) {
                pending.callback(new Error(data.message || 'ERROR'), null)
              }
            }
          }
        })

        // 订阅消息
        this.stompClient.subscribe('/user/queue/messages', (message) => {
          const data = JSON.parse(message.body)
          onMessageReceived(data)
        })

        // 订阅通知
        this.stompClient.subscribe('/user/queue/notifications', (message) => {
          const data = JSON.parse(message.body)
          console.log('收到通知:', data)
          
          // 处理消息同步通知
          if (['NEW_MESSAGE', 'MESSAGE_RECALLED', 'MESSAGES_READ', 'CONVERSATION_UPDATED'].includes(data.type)) {
            // 触发消息同步事件
            if (this.messageHandlers) {
              this.messageHandlers.forEach(handler => handler(data))
            }
            return
          }
          
          // 触发群组通知事件（供Contacts.vue监听）
          const groupNotificationTypes = [
            'GROUP_ADMIN_CHANGE',
            'GROUP_MEMBER_REMOVED',
            'GROUP_MEMBER_QUIT', 
            'GROUP_DIRECT_JOIN',
            'GROUP_DISSOLVED',
            'GROUP_OWNER_TRANSFER',
            'GROUP_INFO_UPDATE',
            'GROUP_NEW_MEMBER'
          ]
          if (groupNotificationTypes.includes(data.type)) {
            window.dispatchEvent(new CustomEvent('groupNotification', { detail: data }))
          }
          
          // 根据通知类型显示不同的通知
          if (data.type === 'FRIEND_REQUEST') {
            // 好友申请通知
            ElNotification({
              title: '好友申请',
              message: `${data.fromUserName} 想加你为好友：${data.message || ''}`,
              type: 'info',
              duration: 6000,
              position: 'top-right'
            })
          } else if (data.type === 'FRIEND_REQUEST_RESULT') {
            // 好友申请处理结果通知
            ElNotification({
              title: data.accepted ? '好友申请已通过' : '好友申请被拒绝',
              message: data.message,
              type: data.accepted ? 'success' : 'info',
              duration: 5000,
              position: 'top-right'
            })
          } else if (data.type === 'GROUP_INVITATION') {
            // 群组邀请通知
            ElNotification({
              title: '群组邀请',
              message: `${data.inviterName} 邀请你加入群组 ${data.groupName}`,
              type: 'info',
              duration: 8000,
              position: 'top-right'
            })
          } else if (data.type === 'GROUP_INVITATION_RESULT') {
            // 群组邀请处理结果通知
            ElNotification({
              title: data.accepted ? '群组邀请已接受' : '群组邀请被拒绝',
              message: data.message,
              type: data.accepted ? 'success' : 'info',
              duration: 5000,
              position: 'top-right'
            })
          } else if (data.type === 'GROUP_INVITE_APPROVAL') {
            // 群组邀请审批通知（管理员收到）
            ElNotification({
              title: '邀请审批请求',
              message: data.message,
              type: 'warning',
              duration: 10000,
              position: 'top-right'
            })
          } else if (data.type === 'GROUP_INVITE_APPROVAL_RESULT') {
            // 群组邀请审批结果通知（邀请人收到）
            ElNotification({
              title: data.approved ? '邀请已批准' : '邀请被拒绝',
              message: data.message,
              type: data.approved ? 'success' : 'warning',
              duration: 8000,
              position: 'top-right'
            })
          } else if ([
            'GROUP_DISSOLVED',
            'GROUP_MEMBER_REMOVED',
            'GROUP_OWNER_TRANSFER',
            'GROUP_INFO_UPDATE',
            'GROUP_NEW_MEMBER',
            'GROUP_ADMIN_CHANGE',
            'GROUP_MEMBER_QUIT',
            'GROUP_DIRECT_JOIN'
          ].includes(data.type)) {
            // 群组相关通知：不弹窗，只推送到群消息通知列表（通过 groupNotification 事件）
            // 事件已在上面触发，这里不需要额外处理
          } else {
            // 其他类型通知
            ElNotification({
              title: '系统通知',
              message: data.message || JSON.stringify(data),
              type: 'info',
              duration: 4000,
              position: 'top-right'
            })
          }
        })
      },
      (error) => {
        console.error('WebSocket 连接失败:', error)
        this.connected = false
        
        const chatStore = useChatStore()
        chatStore.connected = false

        // 自动重连
        if (this.reconnectAttempts < this.maxReconnectAttempts && this.currentToken) {
          this.reconnectAttempts++
          console.log(`尝试重连... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
          setTimeout(() => {
            this.connect(this.currentToken, this.currentMessageHandler)
          }, this.reconnectDelay * this.reconnectAttempts) // 递增延迟
        } else {
          console.error('WebSocket 连接失败，已达到最大重连次数')
          ElMessage.error('网络连接失败，请检查网络后刷新页面')
        }
      }
    )
  }

  // 发送消息（支持回调）
  sendMessage(message, callback) {
    if (this.connected && this.stompClient) {
      console.log('发送WebSocket消息:', message)
      
      // 如果有回调，添加到待处理队列
      if (callback) {
        this.pendingCallbacks.push({
          callback: callback,
          timestamp: Date.now()
        })
        console.log('🔵 回调已加入队列, 当前队列长度:', this.pendingCallbacks.length)
      }
      
      this.stompClient.send('/app/message', {}, JSON.stringify(message))
      return true
    } else {
      console.error('WebSocket未连接，无法发送消息')
      ElMessage.error('连接已断开，请稍后重试')
      if (callback) {
        callback(new Error('WebSocket未连接'), null)
      }
      return false
    }
  }

  // 发送心跳
  sendHeartbeat() {
    if (this.connected && this.stompClient) {
      this.stompClient.send('/app/heartbeat', {}, JSON.stringify({ type: 'HEARTBEAT' }))
    }
  }

  // 断开连接
  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect()
      this.connected = false
      
      // 停止重连
      this.reconnectAttempts = this.maxReconnectAttempts
      this.currentToken = null
      this.currentMessageHandler = null
      
      const chatStore = useChatStore()
      chatStore.connected = false
      
      // 触发断开连接处理器
      this.disconnectHandlers.forEach(handler => handler())
    }
  }
  
  // 添加消息处理器
  onMessage(handler) {
    this.messageHandlers.push(handler)
  }
  
  // 移除消息处理器
  offMessage(handler) {
    const index = this.messageHandlers.indexOf(handler)
    if (index > -1) {
      this.messageHandlers.splice(index, 1)
    }
  }
  
  // 添加连接处理器
  onConnect(handler) {
    this.connectHandlers.push(handler)
  }
  
  // 添加断开连接处理器
  onDisconnect(handler) {
    this.disconnectHandlers.push(handler)
  }
  
  // 检查连接状态
  isConnected() {
    return this.connected
  }
}

export default new WebSocketClient()
