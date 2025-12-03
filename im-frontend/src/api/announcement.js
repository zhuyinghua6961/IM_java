import request from '@/utils/request'

/**
 * 发布群公告
 */
export function publishAnnouncement(data) {
  return request({
    url: '/message/announcement',
    method: 'post',
    data
  })
}

/**
 * 更新群公告
 */
export function updateAnnouncement(id, data) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除群公告
 */
export function deleteAnnouncement(id) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'delete'
  })
}

/**
 * 获取群公告列表
 */
export function getAnnouncementList(groupId) {
  return request({
    url: `/message/announcement/list/${groupId}`,
    method: 'get'
  })
}

/**
 * 获取群最新公告
 */
export function getLatestAnnouncement(groupId) {
  return request({
    url: `/message/announcement/latest/${groupId}`,
    method: 'get'
  })
}

/**
 * 获取公告详情
 */
export function getAnnouncementDetail(id) {
  return request({
    url: `/message/announcement/${id}`,
    method: 'get'
  })
}
