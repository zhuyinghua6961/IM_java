import request from '@/utils/request'

/**
 * 获取群组列表
 */
export const getGroupList = () => {
  return request.get('/group/list')
}

/**
 * 获取群组详情
 */
export const getGroupDetail = (groupId) => {
  return request.get(`/group/detail/${groupId}`)
}

/**
 * 创建群组
 */
export const createGroup = (data) => {
  return request.post('/group/create', data)
}

/**
 * 获取群组成员列表
 */
export const getGroupMembers = (groupId) => {
  return request.get(`/group/${groupId}/members`)
}

/**
 * 邀请用户加入群组
 */
export const inviteToGroup = (groupId, userIds) => {
  return request.post('/group/invite', {
    groupId,
    userIds
  })
}

/**
 * 处理群组邀请
 */
export const handleGroupInvitation = (invitationId, accept) => {
  return request.post('/group/invitation/handle', {
    invitationId,
    accept
  })
}

/**
 * 获取我的群组邀请列表
 */
export const getMyGroupInvitations = () => {
  return request.get('/group/invitations')
}

/**
 * 退出群组
 */
export const quitGroup = (groupId) => {
  return request.post(`/group/${groupId}/quit`)
}

/**
 * 解散群组（群主专用）
 */
export const dissolveGroup = (groupId) => {
  return request.post(`/group/${groupId}/dissolve`)
}

/**
 * 设置/取消管理员
 */
export const setGroupAdmin = (groupId, userId, isAdmin) => {
  return request.post(`/group/${groupId}/admin`, {
    userId,
    isAdmin
  })
}
