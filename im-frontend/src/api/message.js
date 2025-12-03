import request from '@/utils/request'

/**
 * 获取会话列表
 */
export function getConversationList() {
  return request({
    url: '/conversation/list',
    method: 'get'
  })
}

/**
 * 获取历史消息
 */
export function getHistoryMessages(targetId, chatType, page = 1, size = 20) {
  return request({
    url: '/message/history',
    method: 'get',
    params: { targetId, chatType, page, size }
  })
}

/**
 * 撤回消息
 */
export function recallMessage(messageId) {
  return request({
    url: '/message/recall',
    method: 'post',
    data: { messageId }
  })
}

/**
 * 标记消息已读
 */
export function markMessageRead(messageIds) {
  return request({
    url: '/message/read',
    method: 'post',
    data: { messageIds }
  })
}

/**
 * 清空未读数
 */
export function clearUnreadCount(targetId, chatType) {
  return request({
    url: '/conversation/clear-unread',
    method: 'post',
    data: { targetId, chatType }
  })
}

/**
 * 删除单条消息
 */
export function deleteMessage(messageId) {
  return request({
    url: `/message/${messageId}`,
    method: 'delete'
  })
}

/**
 * 搜索消息
 * @param {string} keyword 搜索关键词
 * @param {number} chatType 聊天类型（可选，1-单聊 2-群聊）
 * @param {number} targetId 对方ID（可选）
 * @param {number} page 页码
 * @param {number} size 每页大小
 */
export function searchMessages(keyword, chatType = null, targetId = null, page = 1, size = 20) {
  const params = { keyword, page, size }
  if (chatType) params.chatType = chatType
  if (targetId) params.targetId = targetId
  return request({
    url: '/message/search',
    method: 'get',
    params
  })
}

/**
 * 获取消息上下文（用于搜索跳转定位）
 * @param {number} messageId 消息ID
 * @param {number} contextSize 上下文大小
 */
export function getMessageContext(messageId, contextSize = 50) {
  return request({
    url: '/message/context',
    method: 'get',
    params: { messageId, contextSize }
  })
}
