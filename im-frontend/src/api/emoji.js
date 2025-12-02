import request from '@/utils/request'

export function listEmojis() {
  return request({
    url: '/emoji/list',
    method: 'get'
  })
}

export function addEmoji(data) {
  return request({
    url: '/emoji',
    method: 'post',
    data
  })
}

export function deleteEmoji(id) {
  return request({
    url: `/emoji/${id}`,
    method: 'delete'
  })
}
