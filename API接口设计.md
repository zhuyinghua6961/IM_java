# IM Chat System - API 接口设计文档

**版本**: v2.1  
**更新时间**: 2025-12-02  

**📌 接口状态说明**:  
- ✅ **已实现** - 功能完整，已上线可用
- 🚧 **规划中** - 设计完成，待开发实现  
- ⚠️ **待优化的接口（0个）**

暂无需要改进

---

## 目录
- [1. 接口规范](#1-接口规范)
- [2. 用户服务 API](#2-用户服务-api-im-user-service)
- [3. 好友管理 API](#3-好友管理-api)
- [4. 群组管理 API](#4-群组管理-api)
- [5. 消息服务 API](#5-消息服务-api-im-message-service)
- [6. 会话管理 API](#6-会话管理-api)
- [7. 通知服务 API](#7-通知服务-api)
- [8. 错误码说明](#8-错误码说明)

---

## 1. 接口规范

### 1.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1704067200000
}
```

### 1.2 状态码定义

| 状态码 | 说明 | 状态 |
|--------|------|------|
| 200 | 成功 | ✅ |
| 400 | 请求参数错误 | ✅ |
| 401 | 未认证 | ✅ |
| 403 | 无权限 | ✅ |
| 404 | 资源不存在 | ✅ |
| 500 | 服务器内部错误 | ✅ |
| 1001 | 用户名已存在 | ✅ |
| 1002 | 用户名或密码错误 | ✅ |
| 1003 | Token 已过期 | ✅ |
| 2001 | 好友已存在 | ✅ |
| 2002 | 好友申请已发送 | ✅ |
| 2003 | 好友申请不存在 | ✅ |
| 2004 | 不能添加自己为好友 | ✅ |
| 2005 | 对方已拉黑你 | 🚧 |
| 3001 | 消息发送失败 | ✅ |
| 3002 | 消息不存在 | ✅ |
| 3003 | 消息已撤回 | ✅ |
| 3004 | 撤回时间已过 | ✅ |
| 3005 | 不是消息发送者 | ✅ |
| 4001 | 群组不存在 | ✅ |
| 4002 | 不是群成员 | ✅ |
| 4003 | 无操作权限 | ✅ |
| 4004 | 群组已满 | ✅ |
| 4005 | 已是群成员 | ✅ |
| 7001 | 广场帖子不存在 | ✅ |
| 7002 | 广场评论不存在 | ✅ |
| 7003 | 已经点赞过该帖子 | ✅ |
| 7004 | 内容审核未通过 | ✅ |

### 1.3 请求头

```
Authorization: Bearer {token}
Content-Type: application/json
```

---

## 2. 用户服务 API (im-user-service)

### 2.1 用户注册 ✅

**接口地址**: `POST /api/user/register`

**请求参数**:
```json
{
  "username": "user001",
  "password": "123456",
  "nickname": "张三",
  "phone": "13800138000",
  "email": "user001@im.com"
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "user001",
    "nickname": "张三",
    "avatar": "https://xxx.com/default.jpg"
  }
}
```

---

### 2.2 用户登录 ✅

**接口地址**: `POST /api/user/login`

**请求参数**:
```json
{
  "username": "user001",
  "password": "123456"
}
```

**响应数据**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "userId": 1,
      "username": "user001",
      "nickname": "张三",
      "avatar": "https://xxx.com/avatar.jpg",
      "signature": "这是我的个性签名"
    }
  }
}
```

---

### 2.4 用户登出 ✅

**接口地址**: `POST /api/user/logout`

**响应数据**:
```json
{
  "code": 200,
  "message": "登出成功"
}
```

---

### 2.5 获取用户信息 ✅

**接口地址**: `GET /api/user/info/{userId}`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "user001",
    "nickname": "张三",
    "avatar": "https://xxx.com/avatar.jpg",
    "gender": 1,
    "phone": "13800138000",
    "email": "user001@im.com",
    "signature": "这是我的个性签名",
    "createTime": "2024-01-01 12:00:00"
  }
}
```

---

### 2.5 修改用户信息 ✅

**接口地址**: `PUT /api/user/info`

**请求参数**:
```json
{
  "nickname": "张三三",
  "avatar": "https://xxx.com/new-avatar.jpg",
  "gender": 1,
  "phone": "13800138000",
  "email": "user001@im.com",
  "signature": "新的个性签名"
}
```

---

### 2.6 搜索用户 ✅

**接口地址**: `GET /api/user/search?keyword={keyword}`

**说明**: 根据用户名或手机号精确搜索用户

**请求参数**:
- `keyword`: 用户名或手机号（必填）

**响应数据（找到用户）**:
```json
{
  "code": 200,
  "data": [
    {
      "userId": 1,
      "username": "user001",
      "nickname": "张三",
      "avatar": "https://xxx.com/avatar.jpg",
      "gender": 1,
      "signature": "这是个性签名"
    }
  ]
}
```

---

### 2.7 发送短信验证码 ✅

**接口地址**: `POST /api/user/sms/send`

**请求参数**:

```json
{
  "phone": "13800138000"
}
```

**说明**:
- 向指定手机号发送 6 位数字验证码
- 同一手机号发送频率限制（如 60s 内只能发送一次）

---

### 2.8 修改密码 ✅

**接口地址**: `POST /api/user/password/change`

**说明**: 需同时校验原密码 + 短信验证码

**请求参数**:

```json
{
  "oldPassword": "123456",        // 原密码
  "newPassword": "654321",        // 新密码（至少6位）
  "code": "123456"                // 短信验证码
}
```

**业务规则**:
- 当前登录用户才能修改自己的密码
- 账号必须已绑定手机号
- 原密码错误时返回业务错误码 `1002`/`PASSWORD_ERROR`
- 验证码错误或过期返回 `400 BAD_REQUEST`

---

## 3. 好友管理 API

### 3.1 获取好友列表 ✅

**接口地址**: `GET /api/friend/list`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "userId": 2,
      "username": "user002",
      "nickname": "李四",
      "remark": "我的好友",
      "avatar": "https://xxx.com/avatar.jpg",
      "gender": 1,
      "signature": "个性签名",
      "online": false
    }
  ]
}
```

---

### 3.2 发送好友申请 ✅

**接口地址**: `POST /api/friend/request`

**请求参数**:
```json
{
  "friendId": 2,
  "message": "你好，我想加你为好友"
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "fromUserId": 1,
    "toUserId": 2,
    "message": "你好，我想加你为好友",
    "status": 0,
    "createTime": "2024-01-01 12:00:00"
  }
}
```

---

### 3.3 获取好友申请列表 ✅

**接口地址**: `GET /api/friend/request/list`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "fromUserId": 2,
      "toUserId": 1,
      "message": "你好，我想加你为好友",
      "status": 0,
      "createTime": "2024-01-01 12:00:00"
    }
  ]
}
```

---

### 3.4 处理好友申请 ✅

**接口地址**: `POST /api/friend/request/handle`

**请求参数**:
```json
{
  "requestId": 1,
  "status": 1  // 1-同意 2-拒绝
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "status": 1
  }
}
```

---

### 3.5 删除好友 ✅

**接口地址**: `DELETE /api/friend/{friendId}`

**说明**: 双向删除好友关系

**响应数据**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 3.6 修改好友备注 ✅

**接口地址**: `POST /api/friend/remark`

**说明**: 为当前登录用户修改某个好友的备注名

**请求参数**:

```json
{
  "friendId": 2,
  "remark": "老同学张三"   // 为空或 null 表示清除备注
}
```

**响应数据**:

```json
{
  "code": 200,
  "message": "success"
}
```

**业务规则**:
- `friendId` 必须是当前用户的有效好友
- 备注只对当前用户可见，不会影响对方看到的昵称

---

### 3.7 拉黑/取消拉黑好友 🚧

**接口地址**: `POST /api/friend/{friendId}/block`

**请求参数**:
```json
{
  "block": true  // true-拉黑 false-取消拉黑
}
```

**说明**: 待实现功能，需要创建 `blacklist` 表

---

## 4. 群组管理 API

### 4.1 创建群组 ✅

**接口地址**: `POST /api/group/create`

**请求参数**:
```json
{
  "groupName": "技术交流群",
  "avatar": "https://xxx.com/group-avatar.jpg",
  "notice": "欢迎大家加入",
  "maxMembers": 500,
  "memberIds": [2, 3, 4]
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "groupId": 1,
    "groupName": "技术交流群",
    "avatar": "https://xxx.com/group-avatar.jpg",
    "ownerId": 1,
    "ownerName": "张三",
    "memberCount": 4,
    "maxMembers": 500,
    "notice": "欢迎大家加入",
    "createTime": "2024-01-01 12:00:00"
  }
}
```

---

### 4.2 获取群组列表 ✅

**接口地址**: `GET /api/group/list`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "groupId": 1,
      "groupName": "技术交流群",
      "avatar": "https://xxx.com/group-avatar.jpg",
      "ownerId": 1,
      "ownerName": "张三",
      "memberCount": 100,
      "maxMembers": 500,
      "notice": "欢迎大家"
    }
  ]
}
```

---

### 4.3 获取群组详情 ✅

**接口地址**: `GET /api/group/detail/{groupId}`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "groupId": 1,
    "groupName": "技术交流群",
    "avatar": "https://xxx.com/group-avatar.jpg",
    "ownerId": 1,
    "ownerName": "张三",
    "notice": "欢迎大家",
    "memberCount": 100,
    "maxMembers": 500,
    "createTime": "2024-01-01 12:00:00"
  }
}
```

---

### 4.4 修改群组信息 ✅

**接口地址**: `POST /api/group/{groupId}/update`

**权限**: 仅群主可操作

**请求参数**:
```json
{
  "groupName": "新群名称",
  "avatar": "https://xxx.com/new-avatar.jpg",
  "notice": "新公告",
  "maxMembers": 1000
}
```

**响应数据**: 返回更新后的群组详情

---

### 4.5 邀请用户加入群组 ✅

**接口地址**: `POST /api/group/{groupId}/invite`

**权限**: 群主/管理员/普通成员（根据配置）

**请求参数**:
```json
[5, 6, 7]  // 用户ID数组
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "groupId": 1,
    "groupName": "技术交流群",
    "memberCount": 103
  }
}
```

**说明**: 
- 白名单用户直接加入
- 非白名单用户需要管理员审批

---

### 4.6 获取我的群组邀请列表 ✅

**接口地址**: `GET /api/group/invitations`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "invitationId": 1,
      "groupId": 1,
      "groupName": "技术交流群",
      "inviterId": 2,
      "inviterName": "李四",
      "status": 1,
      "createTime": "2024-01-01 12:00:00",
      "expireTime": "2024-01-08 12:00:00"
    }
  ]
}
```

---

### 4.7 处理群组邀请 ✅

**接口地址**: `POST /api/group/invitation/handle`

**请求参数**:
```json
{
  "invitationId": 1,
  "accept": true  // true-同意 false-拒绝
}
```

---

### 4.8 管理员审批邀请 ✅

**接口地址**: `POST /api/group/invitation/{invitationId}/approve`

**权限**: 仅群主/管理员

**请求参数**:
```json
{
  "approve": true  // true-通过 false-拒绝
}
```

---

### 4.9 获取群组成员列表 ✅

**接口地址**: `GET /api/group/{groupId}/members`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "userId": 1,
      "nickname": "张三",
      "avatar": "https://xxx.com/avatar.jpg",
      "role": 2,  // 0-普通成员 1-管理员 2-群主
      "joinTime": "2024-01-01 12:00:00"
    }
  ]
}
```

---

### 4.10 设置/取消管理员 ✅

**接口地址**: `POST /api/group/{groupId}/admin`

**权限**: 仅群主

**请求参数**:
```json
{
  "userId": 5,
  "isAdmin": true  // true-设置为管理员 false-取消管理员
}
```

---

### 4.11 移除群成员 ✅

**接口地址**: `POST /api/group/{groupId}/remove`

**权限**: 群主/管理员

**请求参数**:
```json
{
  "userId": 5
}
```

**说明**:
- 群主可移除任何成员
- 管理员只能移除普通成员
- 不能移除群主和自己

---

### 4.12 退出群组 ✅

**接口地址**: `POST /api/group/{groupId}/quit`

**说明**: 群主不能退出，只能解散群组

---

### 4.13 解散群组 ✅

**接口地址**: `POST /api/group/{groupId}/dissolve`

**权限**: 仅群主

**说明**: 解散后所有成员被移除，群组状态变为已解散

---

### 4.14 转让群主 🚧

**接口地址**: `POST /api/group/{groupId}/transfer`

**权限**: 仅群主

**请求参数**:
```json
{
  "newOwnerId": 5
}
```

**说明**: 待实现功能

---

### 4.15 修改群昵称 🚧

**接口地址**: `PUT /api/group/{groupId}/member/nickname`

**请求参数**:
```json
{
  "nickname": "我的群昵称"
}
```

**说明**: 待实现功能

---

### 4.16 禁言群成员 ✅

**接口地址**: `POST /api/group/{groupId}/member/mute`

**权限**: 群主、管理员

**请求参数**:
```json
{
  "userId": 5,
  "muteUntil": "2024-01-08 12:00:00"  // 禁言截止时间，字符串格式：yyyy-MM-dd HH:mm:ss，null 或空字符串表示解除禁言
}
```

**说明**:
- 仅群主和管理员可以对成员进行禁言/解禁
- 禁言信息存储在 group_member.mute_until 字段
- 被禁言成员在禁言截止时间之前无法在该群发送消息（服务端会在发送接口进行校验）
- 与“群免打扰”不同：
  - 群免打扰：当前用户自己设置是否接收该群消息提醒，接口为 `POST /api/group/{groupId}/mute`，不影响其他成员
  - 群成员禁言：管理员对某个成员进行限制，禁止其在群内发言

---

### 4.17 群公告管理 ✅

**接口前缀**: `im-message-service`

群公告相关接口由消息服务 `im-message-service` 提供，用于群内公告的发布和展示。

#### 4.17.1 发布群公告

**接口地址**: `POST /api/message/announcement`

**权限**: 群主、管理员

**请求参数**:
```json
{
  "groupId": 1,
  "title": "群公告标题",
  "content": "群公告内容",
  "isTop": true   // 是否置顶，true/false，支持多条同时置顶
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": 1001   // 公告ID
}
```

#### 4.17.2 更新群公告

**接口地址**: `PUT /api/message/announcement/{id}`

**权限**: 群主、管理员

**请求参数**:
```json
{
  "title": "新的公告标题",
  "content": "新的公告内容",
  "isTop": true
}
```

#### 4.17.3 删除群公告

**接口地址**: `DELETE /api/message/announcement/{id}`

**权限**: 群主、管理员

#### 4.17.4 获取群公告列表

**接口地址**: `GET /api/message/announcement/list/{groupId}`

**说明**:
- 返回该群的所有公告，已置顶公告优先，其次按创建时间倒序。

#### 4.17.5 获取最新群公告

**接口地址**: `GET /api/message/announcement/latest/{groupId}`

**说明**:
- 返回该群当前最新的一条公告（优先返回置顶公告）。

---

## 5. 消息服务 API (im-message-service)

### 5.1 WebSocket 连接 ✅

**连接地址**: `ws://domain/ws?token={jwt_token}`

**说明**: 
- 使用 STOMP 协议
- 订阅地址: `/user/queue/messages`
- 发送地址: `/app/chat`

---

### 5.2 获取历史消息 ✅

**接口地址**: `GET /api/message/history`

**请求参数**:
- `targetId`: 对方ID（用户ID或群组ID）
- `chatType`: 聊天类型（1-单聊 2-群聊）
- `page`: 页码（默认1）
- `size`: 每页大小（默认20）

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 123456,
        "fromUserId": 1,
        "toId": 2,
        "chatType": 1,
        "msgType": 1,
        "content": "Hello",
        "url": null,
        "status": 1,
        "sendTime": "2024-01-01 12:00:00",
        "recallTime": null
      }
    ]
  }
}
```

---

### 5.3 撤回消息 ✅

**接口地址**: `POST /api/message/recall`

**请求参数**:
```json
{
  "messageId": 123456
}
```

**说明**: 
- 只能撤回自己的消息
- 撤回时间限制5分钟

---

### 5.4 标记消息已读 ✅

**接口地址**: `POST /api/message/read`

**请求参数**:
```json
{
  "messageIds": [123456, 123457, 123458]
}
```

---

### 5.5 删除单条消息 ✅

**接口地址**: `DELETE /api/message/{messageId}`

**说明**: 
- **单向删除**：只对当前用户生效，对方不受影响
- 删除后该消息不会在历史记录中显示
- 单聊：发送方和接收方都可以删除
- 群聊：群成员可以删除

**响应数据**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 5.6 文件上传（媒体/文件） ✅

**接口前缀**: `im-message-service`

#### 5.6.1 上传语音

**接口地址**: `POST /api/files/upload/audio`

**请求类型**: `multipart/form-data`

**请求参数**:

| 字段 | 类型 | 说明 |
|------|------|------|
| file | File | 语音文件，建议格式 mp3/aac/m4a |

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "url": "https://oss-bucket/voice/2025/12/02/xxx.mp3",
    "size": 12345,
    "fileName": "voice.mp3"
  }
}
```

#### 5.6.2 上传图片

**接口地址**: `POST /api/files/upload/image`

**请求类型**: `multipart/form-data`

**说明**: 聊天图片、用户头像等均使用该接口上传到 OSS。

**响应结构**与上传语音一致。

#### 5.6.3 上传视频

**接口地址**: `POST /api/files/upload/video`

**请求类型**: `multipart/form-data`

**说明**: 聊天视频消息上传到 OSS。

**响应结构**与上传语音一致。

#### 5.6.4 上传通用文件

**接口地址**: `POST /api/files/upload/file`

**请求类型**: `multipart/form-data`

**说明**: 聊天文件消息（如 pdf/doc/zip）；前端根据 `url` 提供下载/打开功能。

**响应结构**与上传语音一致。

---

### 5.7 消息搜索 ✅

**接口地址**: `GET /api/message/search`

**请求参数**:
- `keyword` *(必填)*: 搜索关键词，按消息内容模糊匹配
- `chatType` *(可选)*: 聊天类型，1-单聊 2-群聊；不传则在所有会话中搜索
- `targetId` *(可选)*: 对方ID（用户ID或群ID），与 `chatType` 组合限定搜索范围
- `page` *(可选)*: 页码，默认 1
- `size` *(可选)*: 每页大小，默认 20

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "total": 123,
    "page": 1,
    "size": 20,
    "list": [
      {
        "id": "187654321987654321",   // 消息ID，字符串，避免前端精度丢失
        "fromUserId": "5",
        "toId": "6",
        "chatType": 1,
        "msgType": 1,
        "content": "搜索结果中的消息内容",
        "sendTime": "2025-12-03 20:00:00"
      }
    ]
  }
}
```

**说明**:
- 仅返回当前登录用户有权限看到的消息
- `id` / `fromUserId` / `toId` 为字符串类型，前端请按字符串处理以避免 Snowflake ID 精度丢失
- 搜索结果可结合「消息上下文」接口实现精确跳转和高亮

---

### 5.8 获取消息上下文 ✅

**接口地址**: `GET /api/message/context`

**请求参数**:
- `messageId` *(必填)*: 目标消息ID（Long 类型）
- `contextSize` *(可选)*: 上下文消息数量，默认 50（表示返回目标消息附近的一段消息）

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "messageId": 187654321987654321,
    "chatType": 1,
    "targetId": 6,
    "list": [
      {
        "id": 187654321987654320,
        "fromUserId": 5,
        "toId": 6,
        "chatType": 1,
        "msgType": 1,
        "content": "前一条消息",
        "sendTime": "2025-12-03 19:59:58"
      },
      {
        "id": 187654321987654321,
        "fromUserId": 5,
        "toId": 6,
        "chatType": 1,
        "msgType": 1,
        "content": "命中的目标消息",
        "sendTime": "2025-12-03 20:00:00"
      }
    ]
  }
}
```

**说明**:
- 根据 `messageId` 推导会话信息，返回该消息附近的一段上下文消息列表
- 后端会自动将上下文消息写入 Redis 缓存，以提升后续滚动加载性能
- 返回的 `list` 按时间倒序排列（最新消息在最前）

---

## 6. 会话管理 API

### 6.1 获取会话列表 ✅

**接口地址**: `GET /api/conversation/list`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "conversationId": "1-2",
      "targetId": 2,
      "chatType": 1,
      "targetName": "李四",
      "targetAvatar": "https://xxx.com/avatar.jpg",
      "lastMessage": "Hello",
      "lastMsgTime": "2024-01-01 12:00:00",
      "unreadCount": 5,
      "top": false
    }
  ]
}
```

---

### 6.2 清空未读数 ✅

**接口地址**: `POST /api/conversation/clear-unread`

**请求参数**:
```json
{
  "targetId": 2,
  "chatType": 1
}
```

---

### 6.3 置顶会话 ✅

**接口地址**: `POST /api/conversation/top`

**请求参数**:
```json
{
  "conversationId": 1,
  "top": true
}
```

---

### 6.4 隐藏会话 ✅

**接口地址**: `POST /api/conversation/hide`

**说明**：
- 会话从列表中移除，但不删除聊天记录
- 收到新消息时会话自动恢复显示
- 保留未读数和置顶状态

**请求参数**:
```json
{
  "conversationId": "1-2"  // 支持格式: chatType-targetId
}
```

**💡 使用场景**：暂时不想看某个会话，但保留聊天记录

---

### 6.5 删除会话 ✅

**接口地址**: `DELETE /api/conversation/{conversationId}`

**说明**: 
- **只删除会话记录，不删除消息**
- 删除后你将看不到这个会话和所有聊天记录
- **对方完全不受影响**，对方仍能看到所有消息
- 如果对方再发消息，会话会重新出现（但之前的消息也会重新可见）
- 删除后不可恢复

**响应数据**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

**⚠️ 重要提示**：
- 这是**单向删除**：只影响你自己，对方完全不受影响
- 消息实际上还在数据库中，只是你看不到了
- 如果需要彻底清空，应该使用"隐藏会话"功能

---

## 7. 通知服务 API

### 7.1 推送通知 ✅

**接口地址**: `POST /api/notification/push`

**说明**: 内部服务调用，用于WebSocket通知推送

**请求参数**:
```json
{
  "type": "FRIEND_REQUEST",
  "receiverId": 2,
  "data": {
    "requestId": 1,
    "fromUserId": 1,
    "fromUserName": "张三",
    "message": "你好"
  }
}
```

**通知类型**:
- `FRIEND_REQUEST`: 好友申请
- `FRIEND_REQUEST_ACCEPTED`: 好友申请通过
- `GROUP_INVITATION`: 群组邀请
- `GROUP_ADMIN_CHANGE`: 群管理员变更
- `GROUP_MEMBER_REMOVED`: 群成员被移除
- `GROUP_MEMBER_QUIT`: 群成员退出
- `GROUP_DIRECT_JOIN`: 直接加入群组
- `GROUP_DISSOLVED`: 群组解散

---

## 8. 错误码说明

### 8.1 通用错误码

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数 |
| 401 | 未认证 | 重新登录获取 Token |
| 403 | 无权限 | 检查用户权限 |
| 404 | 资源不存在 | 检查资源 ID |
| 500 | 服务器内部错误 | 联系管理员 |

### 8.2 业务错误码

| 模块 | 错误码 | 说明 |
|------|--------|------|
| 用户 | 1001 | 用户名已存在 |
| 用户 | 1002 | 用户名或密码错误 |
| 用户 | 1003 | Token 已过期 |
| 好友 | 2001 | 好友已存在 |
| 好友 | 2002 | 好友申请已发送 |
| 好友 | 2003 | 好友申请不存在 |
| 好友 | 2004 | 不能添加自己为好友 |
| 好友 | 2005 | 对方已拉黑你 |
| 消息 | 3001 | 消息发送失败 |
| 消息 | 3002 | 消息不存在 |
| 消息 | 3003 | 消息已撤回 |
| 消息 | 3004 | 撤回时间已过 |
| 消息 | 3005 | 不是消息发送者 |
| 群组 | 4001 | 群组不存在 |
| 群组 | 4002 | 不是群成员 |
| 群组 | 4003 | 无操作权限 |
| 群组 | 4004 | 群组已满 |
| 群组 | 4005 | 已是群成员 |
| 广场 | 7001 | 广场帖子不存在 |
| 广场 | 7002 | 广场评论不存在 |
| 广场 | 7003 | 已经点赞过该帖子 |
| 广场 | 7004 | 内容审核未通过 |

---

## 9. 广场服务 API (im-square-service)

### 9.1 发布广场帖子 ✅

**接口地址**: `POST /api/square/posts`

**说明**: 发布一条广场帖子，支持文本、多图、可选视频和标签。内容将经过可配置的内容审核。

**请求参数**:

```json
{
  "title": "可选标题",
  "content": "正文内容",
  "images": ["https://xxx.com/image1.jpg"],
  "video": null,
  "tags": ["编程", "生活"]
}
```

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "postId": 1,
    "createTime": "2025-01-01 12:00:00"
  }
}
```

---

### 9.2 获取广场帖子列表 ✅

**接口地址**: `GET /api/square/posts`

**说明**: 分页获取广场中的公开帖子列表，只返回审核通过且未删除的帖子。

**请求参数**:

- `page` *(可选)*: 页码，默认 1
- `size` *(可选)*: 每页大小，默认 20

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "postId": 1,
        "userId": 1,
        "title": "标题",
        "content": "正文内容",
        "images": ["https://xxx.com/a.jpg"],
        "video": null,
        "tags": ["编程"],
        "likeCount": 10,
        "commentCount": 5,
        "liked": false,
        "createTime": "2025-01-01 12:00:00"
      }
    ],
    "total": 100,
    "size": 20,
    "current": 1,
    "pages": 5
  }
}
```

---

### 9.3 获取帖子详情 ✅

**接口地址**: `GET /api/square/posts/{postId}`

**说明**: 获取单条广场帖子的详细信息。

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "postId": 1,
    "userId": 1,
    "title": "标题",
    "content": "正文内容",
    "images": ["https://xxx.com/a.jpg"],
    "video": null,
    "tags": ["编程"],
    "likeCount": 10,
    "commentCount": 5,
    "liked": false,
    "createTime": "2025-01-01 12:00:00"
  }
}
```

---

### 9.4 删除帖子 ✅

**接口地址**: `DELETE /api/square/posts/{postId}`

**说明**:

- 仅帖子作者本人可以删除
- 实现为软删除：将 `status` 置为 0

**响应数据**:

```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 9.5 点赞 / 取消点赞 ✅

**点赞接口地址**: `POST /api/square/posts/{postId}/like`

**取消点赞接口地址**: `DELETE /api/square/posts/{postId}/like`

**说明**:

- 同一用户对同一帖子只能点赞一次
- 点赞/取消点赞会同步更新 `likeCount`

**点赞成功响应**:

```json
{
  "code": 200,
  "message": "success"
}
```

---

### 9.6 获取评论列表 ✅

**接口地址**: `GET /api/square/posts/{postId}/comments`

**说明**: 分页获取指定帖子下的评论列表，仅返回正常状态评论。

**请求参数**:

- `page` *(可选)*: 页码，默认 1
- `size` *(可选)*: 每页大小，默认 20

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "commentId": 1,
        "postId": 1,
        "userId": 2,
        "parentId": null,
        "content": "评论内容",
        "createTime": "2025-01-01 12:01:00"
      }
    ],
    "total": 10,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

---

### 9.7 发表评论 / 回复评论 ✅

**接口地址**: `POST /api/square/posts/{postId}/comments`

**请求参数**:

```json
{
  "content": "评论内容",
  "parentId": null
}
```

**说明**:

- `parentId` 为空表示对帖子发表评论
- `parentId` 不为空表示回复某条评论
- 内容将经过内容审核

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "commentId": 1
  }
}
```

---

### 9.8 删除评论 ✅

**接口地址**: `DELETE /api/square/comments/{commentId}`

**说明**:

- 仅评论作者本人可以删除评论（后续可扩展为帖子作者也可删除）
- 实现为软删除：将 `status` 置为 0，并同步减少帖子评论数

**响应数据**:

```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 9.9 我的帖子列表 ✅

**接口地址**: `GET /api/square/my/posts`

**说明**: 分页获取当前登录用户自己发布的广场帖子列表，可看到审核状态。

**请求参数**:

- `page` *(可选)*: 页码，默认 1
- `size` *(可选)*: 每页大小，默认 20

**响应数据**:

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "postId": 1,
        "userId": 1,
        "title": "标题",
        "content": "正文内容",
        "images": ["https://xxx.com/a.jpg"],
        "video": null,
        "tags": ["编程"],
        "likeCount": 10,
        "commentCount": 5,
        "liked": false,
        "createTime": "2025-01-01 12:00:00"
      }
    ],
    "total": 10,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

---

## 10. 待实现功能清单

### 优先级 P0（核心功能）

- [ ] 在线状态显示
- [ ] 群主转让
- [ ] 群昵称修改

### 优先级 P1（重要功能）

- [ ] 拉黑功能
- [ ] 群成员禁言
- [ ] 消息@提醒
- [ ] 群公告历史记录

### 优先级 P2（优化功能）

- [ ] 消息搜索
- [ ] 消息转发
- [ ] 群组搜索
- [ ] 消息置顶
- [ ] 文件下载管理

### 优先级 P3（高级功能）

- [ ] 阅后即焚
- [ ] 语音通话
- [ ] 视频通话
- [ ] 朋友圈功能

---

## 11. 版本更新记录

### v2.0.0 (2025-11-27)
- ✅ 完善群组管理功能
  - 修改群信息（群主专用）
  - 移除群成员
  - 设置/取消管理员
  - 解散群组
  - 群组邀请审批流程
- ✅ 完善好友功能
  - 删除好友
- ✅ 完善消息功能
  - 消息撤回
  - 消息已读
  - 历史消息分页
  - 删除单条消息（单向删除）
- ✅ 完善会话功能
  - 会话置顶
  - 会话隐藏
  - 清空未读
  - 删除会话（单向删除，只影响当前用户）
- ✅ WebSocket 通知系统
  - 好友申请通知
  - 群组通知
  - 系统通知
- 📝 更新完整接口文档

### v1.0.0 (2024-01-01)
- 初始版本
- 基础用户、好友、群组、消息功能

---

**文档维护**: 请在实现新功能或修改接口时及时更新此文档  
**联系方式**: dev@example.com
