import request from '@/utils/request'

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

/**
 * 获取用户信息
 */
export function getUserInfo(userId) {
  return request({
    url: `/user/info/${userId}`,
    method: 'get'
  })
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data) {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}

/**
 * 搜索用户
 */
export function searchUser(keyword) {
  return request({
    url: '/user/search',
    method: 'get',
    params: { keyword }
  })
}

/**
 * 发送短信验证码
 */
export function sendSmsCode(phone) {
  return request({
    url: '/user/sms/send',
    method: 'post',
    data: { phone }
  })
}
