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
