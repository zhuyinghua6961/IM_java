import request from '@/utils/request'

/**
 * 获取好友列表
 */
export function getFriendList() {
  return request({
    url: '/friend/list',
    method: 'get'
  })
}

/**
 * 添加好友
 */
export function addFriend(data) {
  return request({
    url: '/friend/request',
    method: 'post',
    data
  })
}

/**
 * 获取好友申请列表
 */
export function getFriendRequestList() {
  return request({
    url: '/friend/request/list',
    method: 'get'
  })
}

/**
 * 处理好友申请
 */
export function handleFriendRequest(data) {
  return request({
    url: '/friend/request/handle',
    method: 'post',
    data
  })
}

/**
 * 删除好友
 */
export function deleteFriend(friendId) {
  return request({
    url: `/friend/${friendId}`,
    method: 'delete'
  })
}

/**
 * 更新好友备注
 */
export function updateFriendRemark(data) {
  return request({
    url: '/friend/remark',
    method: 'post',
    data
  })
}
