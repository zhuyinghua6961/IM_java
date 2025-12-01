import { ElMessage } from 'element-plus'

/**
 * 消息同步管理器
 * 负责WebSocket消息同步和智能轮询备用机制
 */
class MessageSyncManager {
  constructor() {
    this.wsConnected = false
    this.pollingTimer = null
    this.lastSyncTime = null
    this.currentConversation = null
    this.messageUpdateCallback = null
    this.conversationUpdateCallback = null
    
    // 轮询间隔配置
    this.pollingIntervals = {
      active: 3000,      // 用户活跃时3秒
      inactive: 10000,   // 用户不活跃时10秒
      background: 30000  // 页面在后台时30秒
    }
    
    this.isUserActive = true
    this.isPageVisible = true
    
    this.init()
  }
  
  /**
   * 初始化
   */
  init() {
    this.setupVisibilityListeners()
    this.setupActivityListeners()
  }
  
  /**
   * 设置页面可见性监听
   */
  setupVisibilityListeners() {
    document.addEventListener('visibilitychange', () => {
      this.isPageVisible = !document.hidden
      this.updatePollingStrategy()
      
      if (this.isPageVisible) {
        // 页面重新可见时，立即同步一次
        this.syncMessages()
      }
    })
  }
  
  /**
   * 设置用户活跃度监听
   */
  setupActivityListeners() {
    let activityTimer = null
    
    const resetActivityTimer = () => {
      this.isUserActive = true
      clearTimeout(activityTimer)
      
      activityTimer = setTimeout(() => {
        this.isUserActive = false
        this.updatePollingStrategy()
      }, 60000) // 1分钟无操作视为不活跃
      
      this.updatePollingStrategy()
    }
    
    // 监听用户活动
    ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'].forEach(event => {
      document.addEventListener(event, resetActivityTimer, { passive: true })
    })
    
    resetActivityTimer()
  }
  
  /**
   * WebSocket连接成功
   */
  onWebSocketConnected() {
    // console.log('MessageSync: WebSocket连接成功')
    this.wsConnected = true
    this.stopPolling()
  }
  
  /**
   * WebSocket断开连接
   */
  onWebSocketDisconnected() {
    // console.log('MessageSync: WebSocket断开，启用轮询备用')
    this.wsConnected = false
    this.startPolling()
  }
  
  /**
   * 设置当前会话
   */
  setCurrentConversation(conversation) {
    this.currentConversation = conversation
  }
  
  /**
   * 设置消息更新回调
   */
  setMessageUpdateCallback(callback) {
    this.messageUpdateCallback = callback
  }
  
  /**
   * 设置会话更新回调
   */
  setConversationUpdateCallback(callback) {
    this.conversationUpdateCallback = callback
  }
  
  /**
   * 处理WebSocket消息
   */
  handleWebSocketMessage(data) {
    // console.log('MessageSync: 收到WebSocket消息', data)
    
    switch (data.type) {
      case 'NEW_MESSAGE':
        this.handleNewMessage(data)
        break
      case 'MESSAGE_RECALLED':
        this.handleMessageRecalled(data)
        break
      case 'MESSAGES_READ':
        this.handleMessagesRead(data)
        break
      case 'CONVERSATION_UPDATED':
        this.handleConversationUpdated(data)
        break
      default:
        console.warn('MessageSync: 未知消息类型', data.type)
    }
  }
  
  /**
   * 处理新消息
   */
  handleNewMessage(data) {
    if (this.messageUpdateCallback) {
      this.messageUpdateCallback('NEW_MESSAGE', data)
    }
    
    // 更新会话列表
    if (this.conversationUpdateCallback) {
      this.conversationUpdateCallback()
    }
  }
  
  /**
   * 处理消息撤回
   */
  handleMessageRecalled(data) {
    // console.log('MessageSync: 处理消息撤回', data)
    
    if (this.messageUpdateCallback) {
      this.messageUpdateCallback('MESSAGE_RECALLED', {
        messageId: data.messageId,
        recallTime: data.recallTime
      })
    }
    
    // 显示撤回提示
    if (data.fromUserId !== this.getCurrentUserId()) {
      ElMessage.info('对方撤回了一条消息')
    }
  }
  
  /**
   * 处理消息已读
   */
  handleMessagesRead(data) {
    if (this.messageUpdateCallback) {
      this.messageUpdateCallback('MESSAGES_READ', data)
    }
  }
  
  /**
   * 处理会话更新
   */
  handleConversationUpdated(data) {
    if (this.conversationUpdateCallback) {
      this.conversationUpdateCallback()
    }
  }
  
  /**
   * 更新轮询策略
   */
  updatePollingStrategy() {
    if (this.wsConnected) {
      this.stopPolling()
      return
    }
    
    let interval
    if (!this.isPageVisible) {
      interval = this.pollingIntervals.background
    } else if (this.isUserActive) {
      interval = this.pollingIntervals.active
    } else {
      interval = this.pollingIntervals.inactive
    }
    
    this.restartPolling(interval)
  }
  
  /**
   * 启动轮询
   */
  startPolling() {
    if (!this.pollingTimer) {
      const interval = this.getPollingInterval()
      this.pollingTimer = setInterval(() => {
        this.syncMessages()
      }, interval)
      
      // console.log(`MessageSync: 启动轮询，间隔${interval}ms`)
    }
  }
  
  /**
   * 停止轮询
   */
  stopPolling() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer)
      this.pollingTimer = null
      // console.log('MessageSync: 停止轮询')
    }
  }
  
  /**
   * 重启轮询
   */
  restartPolling(interval) {
    this.stopPolling()
    
    if (interval) {
      this.pollingTimer = setInterval(() => {
        this.syncMessages()
      }, interval)
      
      // console.log(`MessageSync: 重启轮询，间隔${interval}ms`)
    }
  }
  
  /**
   * 获取轮询间隔
   */
  getPollingInterval() {
    if (!this.isPageVisible) {
      return this.pollingIntervals.background
    } else if (this.isUserActive) {
      return this.pollingIntervals.active
    } else {
      return this.pollingIntervals.inactive
    }
  }
  
  /**
   * 同步消息
   */
  async syncMessages() {
    if (!this.currentConversation) return
    
    try {
      // console.log('MessageSync: 轮询同步消息')
      
      if (this.messageUpdateCallback) {
        this.messageUpdateCallback('SYNC_MESSAGES', {
          conversationId: this.currentConversation.id,
          targetId: this.currentConversation.targetId,
          chatType: this.currentConversation.chatType
        })
      }
      
      this.lastSyncTime = Date.now()
    } catch (error) {
      console.error('MessageSync: 同步消息失败', error)
    }
  }
  
  /**
   * 获取当前用户ID
   */
  getCurrentUserId() {
    // 从localStorage获取当前用户ID
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    // 优先使用 userId 字段，兼容老数据的 id 字段
    return userInfo.userId || userInfo.id
  }
  
  /**
   * 销毁管理器
   */
  destroy() {
    this.stopPolling()
    this.currentConversation = null
    this.messageUpdateCallback = null
    this.conversationUpdateCallback = null
  }
}

// 创建单例实例
const messageSyncManager = new MessageSyncManager()

export default messageSyncManager
