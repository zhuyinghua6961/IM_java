import request from '@/utils/request'

// 发布广场帖子
export function publishSquarePost(data) {
  return request({
    url: '/square/posts',
    method: 'post',
    data
  })
}

// 获取热门广场帖子列表（按热度排序）
export function getHotSquarePosts(page = 1, size = 20) {
  return request({
    url: '/square/posts/hot',
    method: 'get',
    params: { page, size }
  })
}

// 获取广场帖子列表（支持关键字和标签筛选）
export function getSquarePosts(page = 1, size = 20, keyword = '', tags = []) {
  const params = { page, size }
  if (keyword && String(keyword).trim()) {
    params.keyword = String(keyword).trim()
  }
  if (Array.isArray(tags) && tags.length > 0) {
    params.tags = tags.join(',')
  }
  return request({
    url: '/square/posts',
    method: 'get',
    params
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

// 收藏帖子
export function favoriteSquarePost(postId) {
  return request({
    url: `/square/posts/${postId}/favorite`,
    method: 'post'
  })
}

// 取消收藏
export function unfavoriteSquarePost(postId) {
  return request({
    url: `/square/posts/${postId}/favorite`,
    method: 'delete'
  })
}

// 个人广场主页信息
export function getSquareProfile(userId) {
  return request({
    url: `/square/profile/${userId}`,
    method: 'get'
  })
}

// 某个用户的广场帖子
export function getUserSquarePosts(userId, page = 1, size = 20) {
  return request({
    url: `/square/user/${userId}/posts`,
    method: 'get',
    params: { page, size }
  })
}

// 某个用户收藏的广场帖子
export function getUserFavoriteSquarePosts(userId, page = 1, size = 20) {
  return request({
    url: `/square/user/${userId}/favorites`,
    method: 'get',
    params: { page, size }
  })
}

// 某个用户点赞过的广场帖子
export function getUserLikedSquarePosts(userId, page = 1, size = 20) {
  return request({
    url: `/square/user/${userId}/likes`,
    method: 'get',
    params: { page, size }
  })
}

// 编辑帖子
export function updateSquarePost(postId, data) {
  return request({
    url: `/square/posts/${postId}`,
    method: 'put',
    data
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
