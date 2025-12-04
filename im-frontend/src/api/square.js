import request from '@/utils/request'

// 发布广场帖子
export function publishSquarePost(data) {
  return request({
    url: '/square/posts',
    method: 'post',
    data
  })
}

// 获取广场帖子列表
export function getSquarePosts(page = 1, size = 20) {
  return request({
    url: '/square/posts',
    method: 'get',
    params: { page, size }
  })
}

// 获取帖子详情
export function getSquarePostDetail(postId) {
  return request({
    url: `/square/posts/${postId}`,
    method: 'get'
  })
}

// 删除帖子
export function deleteSquarePost(postId) {
  return request({
    url: `/square/posts/${postId}`,
    method: 'delete'
  })
}

// 点赞帖子
export function likeSquarePost(postId) {
  return request({
    url: `/square/posts/${postId}/like`,
    method: 'post'
  })
}

// 取消点赞
export function unlikeSquarePost(postId) {
  return request({
    url: `/square/posts/${postId}/like`,
    method: 'delete'
  })
}

// 获取评论列表
export function getSquareComments(postId, page = 1, size = 20) {
  return request({
    url: `/square/posts/${postId}/comments`,
    method: 'get',
    params: { page, size }
  })
}

// 发表评论/回复评论
export function addSquareComment(postId, data) {
  return request({
    url: `/square/posts/${postId}/comments`,
    method: 'post',
    data
  })
}

// 删除评论
export function deleteSquareComment(commentId) {
  return request({
    url: `/square/comments/${commentId}`,
    method: 'delete'
  })
}

// 我的帖子列表
export function getMySquarePosts(page = 1, size = 20) {
  return request({
    url: '/square/my/posts',
    method: 'get',
    params: { page, size }
  })
}

// 我的关注动态（关注的用户的广场帖子）
export function getFollowSquarePosts(page = 1, size = 20) {
  return request({
    url: '/square/feed',
    method: 'get',
    params: { page, size }
  })
}

// 关注广场用户
export function followSquareUser(userId) {
  return request({
    url: `/square/follow/${userId}`,
    method: 'post'
  })
}

// 取消关注广场用户
export function unfollowSquareUser(userId) {
  return request({
    url: `/square/follow/${userId}`,
    method: 'delete'
  })
}

// 我的广场消息列表
export function getSquareNotifications(page = 1, size = 20) {
  return request({
    url: '/notification/square',
    method: 'get',
    params: { page, size }
  })
}

// 将广场消息标记为已读
export function markSquareNotificationsRead() {
  return request({
    url: '/notification/square/read',
    method: 'post'
  })
}

// 按ID批量将广场消息标记为已读
export function markSquareNotificationsReadByIds(ids) {
  return request({
    url: '/notification/square/read/ids',
    method: 'post',
    data: { ids }
  })
}
