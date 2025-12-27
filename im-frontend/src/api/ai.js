import request from '@/utils/request'

/**
 * 创建新会话
 */
export function createConversation(userId, firstMessage) {
  return request({
    url: '/ai/conversation/create',
    method: 'post',
    data: { userId, message: firstMessage }
  })
}

/**
 * 发送消息（带会话）
 */
export function sendChatMessage(conversationId, message) {
  return request({
    url: '/ai/conversation/chat',
    method: 'post',
    data: { conversationId, message }
  })
}

/**
 * 获取会话列表
 */
export function getConversationList(userId) {
  return request({
    url: '/ai/conversation/list',
    method: 'get',
    params: { userId }
  })
}

/**
 * 获取会话消息
 */
export function getConversationMessages(conversationId) {
  return request({
    url: '/ai/conversation/messages',
    method: 'get',
    params: { conversationId }
  })
}

/**
 * 删除会话
 */
export function deleteConversation(conversationId) {
  return request({
    url: '/ai/conversation',
    method: 'delete',
    params: { conversationId }
  })
}

/**
 * 简单对话（不保存会话）
 */
export function aiSimpleChat(message) {
  return request({
    url: '/ai/simpleChat',
    method: 'post',
    data: { message }
  })
}

/**
 * AI服务健康检查
 */
export function aiHealthCheck() {
  return request({
    url: '/ai/health',
    method: 'get'
  })
}
