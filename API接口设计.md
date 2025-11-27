# IM Chat System - API 接口设计文档

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

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户名已存在 |
| 1002 | 用户名或密码错误 |
| 1003 | Token 已过期 |
| 2001 | 好友已存在 |
| 2002 | 好友申请已发送 |
| 3001 | 消息发送失败 |
| 3002 | 消息不存在 |

### 1.3 请求头

```
Authorization: Bearer {token}
Content-Type: application/json
```

## 2. 用户服务 API (im-user-service)

### 2.1 用户注册

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

### 2.2 用户登录

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

### 2.3 获取用户信息

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

### 2.4 修改用户信息

**接口地址**: `PUT /api/user/info`

**请求参数**:
```json
{
  "nickname": "张三三",
  "avatar": "https://xxx.com/new-avatar.jpg",
  "gender": 1,
  "signature": "新的个性签名"
}
```

### 2.5 搜索用户

**接口地址**: `GET /api/user/search?keyword={keyword}`

**说明**: 根据用户名或手机号精确搜索用户（用于添加好友）

**请求参数**:
- `keyword`: 用户名或手机号（必填）

**响应数据（找到用户）**:
```json
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "user001",
    "nickname": "张三",
    "avatar": "https://xxx.com/avatar.jpg",
    "gender": 1,
    "signature": "这是个性签名"
  }
}
```

**响应数据（未找到）**:
```json
{
  "code": 404,
  "message": "用户不存在"
}
```

### 2.6 获取好友列表

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
      "online": true
    }
  ]
}
```

### 2.7 添加好友

**接口地址**: `POST /api/friend/request`

**请求参数**:
```json
{
  "friendId": 2,
  "message": "你好，我想加你为好友"
}
```

### 2.8 处理好友申请

**接口地址**: `POST /api/friend/request/handle`

**请求参数**:
```json
{
  "requestId": 1,
  "status": 1  // 1-同意 2-拒绝
}
```

### 2.9 删除好友

**接口地址**: `DELETE /api/friend/{friendId}`

### 2.10 创建群组

**接口地址**: `POST /api/group/create`

**请求参数**:
```json
{
  "groupName": "技术交流群",
  "avatar": "https://xxx.com/group-avatar.jpg",
  "notice": "欢迎大家加入",
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
    "memberCount": 4
  }
}
```

### 2.11 获取群组列表

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
      "memberCount": 100,
      "notice": "欢迎大家"
    }
  ]
}
```

### 2.12 获取群组详情

**接口地址**: `GET /api/group/{groupId}`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "groupId": 1,
    "groupName": "技术交流群",
    "avatar": "https://xxx.com/group-avatar.jpg",
    "ownerId": 1,
    "notice": "欢迎大家",
    "memberCount": 100,
    "maxMembers": 500,
    "createTime": "2024-01-01 12:00:00"
  }
}
```

### 2.13 获取群成员列表

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

### 2.14 邀请加入群组

**接口地址**: `POST /api/group/{groupId}/invite`

**请求参数**:
```json
{
  "userIds": [5, 6, 7]
}
```

### 2.15 退出群组

**接口地址**: `POST /api/group/{groupId}/quit`

## 3. 消息服务 API (im-message-service)

### 3.1 WebSocket 连接

**连接地址**: `ws://domain/ws?token={jwt_token}`

**消息格式**:
```json
{
  "type": "MESSAGE",  // MESSAGE, ACK, HEARTBEAT, NOTIFICATION
  "data": {}
}
```

### 3.2 发送单聊消息

**WebSocket 消息**:
```json
{
  "type": "MESSAGE",
  "data": {
    "toUserId": 2,
    "chatType": 1,
    "msgType": 1,  // 1-文本 2-图片 3-视频 4-文件 5-语音
    "content": "Hello"
  }
}
```

**响应消息**:
```json
{
  "type": "ACK",
  "data": {
    "messageId": 123456,
    "status": "success",
    "sendTime": "2024-01-01 12:00:00"
  }
}
```

### 3.3 发送群聊消息

**WebSocket 消息**:
```json
{
  "type": "MESSAGE",
  "data": {
    "groupId": 1,
    "chatType": 2,
    "msgType": 1,
    "content": "Hello everyone"
  }
}
```

### 3.4 获取会话列表

**接口地址**: `GET /api/conversation/list`

**响应数据**:
```json
{
  "code": 200,
  "data": [
    {
      "conversationId": 1,
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

### 3.5 获取历史消息

**接口地址**: `GET /api/message/history?targetId={targetId}&chatType={chatType}&page=1&size=20`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "messageId": 123456,
        "fromUserId": 1,
        "fromUserName": "张三",
        "fromUserAvatar": "https://xxx.com/avatar.jpg",
        "msgType": 1,
        "content": "Hello",
        "url": null,
        "sendTime": "2024-01-01 12:00:00",
        "status": 1
      }
    ]
  }
}
```

### 3.6 撤回消息

**接口地址**: `POST /api/message/recall`

**请求参数**:
```json
{
  "messageId": 123456
}
```

### 3.7 标记消息已读

**接口地址**: `POST /api/message/read`

**请求参数**:
```json
{
  "messageIds": [123456, 123457, 123458]
}
```

### 3.8 清空未读数

**接口地址**: `POST /api/conversation/clear-unread`

**请求参数**:
```json
{
  "targetId": 2,
  "chatType": 1
}
```

### 3.9 置顶会话

**接口地址**: `POST /api/conversation/top`

**请求参数**:
```json
{
  "conversationId": 1,
  "top": true
}
```

### 3.10 删除会话

**接口地址**: `DELETE /api/conversation/{conversationId}`

## 4. 文件服务 API (im-file-service)

### 4.1 上传文件

**接口地址**: `POST /api/file/upload`

**请求参数**: `multipart/form-data`
- file: 文件

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "url": "https://minio.xxx.com/im-chat/xxx.jpg",
    "fileName": "avatar.jpg",
    "fileSize": 102400,
    "fileType": "image/jpeg"
  }
}
```

### 4.2 上传图片

**接口地址**: `POST /api/file/upload/image`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "url": "https://minio.xxx.com/im-chat/xxx.jpg",
    "thumbnailUrl": "https://minio.xxx.com/im-chat/xxx_thumb.jpg"
  }
}
```

### 4.3 上传视频

**接口地址**: `POST /api/file/upload/video`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "url": "https://minio.xxx.com/im-chat/xxx.mp4",
    "coverUrl": "https://minio.xxx.com/im-chat/xxx_cover.jpg",
    "duration": 120
  }
}
```

### 4.4 下载文件

**接口地址**: `GET /api/file/download?url={fileUrl}`

## 5. 朋友圈服务 API (im-moments-service)

### 5.1 发布动态

**接口地址**: `POST /api/moments/publish`

**请求参数**:
```json
{
  "content": "今天天气真好",
  "images": [
    "https://xxx.com/image1.jpg",
    "https://xxx.com/image2.jpg"
  ],
  "video": null,
  "location": "北京市朝阳区",
  "visibleType": 0,  // 0-公开 1-私密 2-部分可见
  "visibleUsers": []
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "momentsId": 1,
    "createTime": "2024-01-01 12:00:00"
  }
}
```

### 5.2 获取动态列表

**接口地址**: `GET /api/moments/list?page=1&size=20`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "list": [
      {
        "momentsId": 1,
        "userId": 1,
        "userName": "张三",
        "userAvatar": "https://xxx.com/avatar.jpg",
        "content": "今天天气真好",
        "images": ["https://xxx.com/image1.jpg"],
        "video": null,
        "location": "北京市朝阳区",
        "likeCount": 10,
        "commentCount": 5,
        "liked": false,
        "createTime": "2024-01-01 12:00:00"
      }
    ]
  }
}
```

### 5.3 获取动态详情

**接口地址**: `GET /api/moments/{momentsId}`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "momentsId": 1,
    "userId": 1,
    "userName": "张三",
    "userAvatar": "https://xxx.com/avatar.jpg",
    "content": "今天天气真好",
    "images": ["https://xxx.com/image1.jpg"],
    "video": null,
    "location": "北京市朝阳区",
    "likeCount": 10,
    "commentCount": 5,
    "liked": false,
    "likes": [
      {
        "userId": 2,
        "userName": "李四",
        "userAvatar": "https://xxx.com/avatar.jpg"
      }
    ],
    "comments": [
      {
        "commentId": 1,
        "userId": 3,
        "userName": "王五",
        "userAvatar": "https://xxx.com/avatar.jpg",
        "content": "真不错",
        "createTime": "2024-01-01 12:05:00"
      }
    ],
    "createTime": "2024-01-01 12:00:00"
  }
}
```

### 5.4 点赞动态

**接口地址**: `POST /api/moments/{momentsId}/like`

### 5.5 取消点赞

**接口地址**: `DELETE /api/moments/{momentsId}/like`

### 5.6 评论动态

**接口地址**: `POST /api/moments/{momentsId}/comment`

**请求参数**:
```json
{
  "content": "真不错",
  "replyToId": null  // 回复某条评论时填写
}
```

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "commentId": 1,
    "createTime": "2024-01-01 12:05:00"
  }
}
```

### 5.7 删除评论

**接口地址**: `DELETE /api/moments/comment/{commentId}`

### 5.8 删除动态

**接口地址**: `DELETE /api/moments/{momentsId}`

## 6. 系统通知 API

### 6.1 获取通知列表

**接口地址**: `GET /api/notification/list?page=1&size=20`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "total": 50,
    "list": [
      {
        "notificationId": 1,
        "type": 1,  // 1-好友申请 2-系统通知 3-群邀请
        "title": "好友申请",
        "content": "张三请求添加你为好友",
        "data": {
          "requestId": 1,
          "fromUserId": 2
        },
        "read": false,
        "createTime": "2024-01-01 12:00:00"
      }
    ]
  }
}
```

### 6.2 标记通知已读

**接口地址**: `POST /api/notification/read`

**请求参数**:
```json
{
  "notificationIds": [1, 2, 3]
}
```

### 6.3 获取未读通知数

**接口地址**: `GET /api/notification/unread-count`

**响应数据**:
```json
{
  "code": 200,
  "data": {
    "count": 5
  }
}
```

## 7. 错误码说明

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数 |
| 401 | 未认证 | 重新登录获取 Token |
| 403 | 无权限 | 检查用户权限 |
| 404 | 资源不存在 | 检查资源 ID |
| 500 | 服务器内部错误 | 联系管理员 |
| 1001 | 用户名已存在 | 更换用户名 |
| 1002 | 用户名或密码错误 | 检查登录信息 |
| 1003 | Token 已过期 | 重新登录 |
| 2001 | 好友已存在 | - |
| 2002 | 好友申请已发送 | 等待对方处理 |
| 3001 | 消息发送失败 | 重试发送 |
| 3002 | 消息不存在 | 检查消息 ID |

## 8. 接口调用示例

### 8.1 完整的登录流程

```javascript
// 1. 用户登录
const loginResponse = await axios.post('/api/user/login', {
  username: 'user001',
  password: '123456'
});

const token = loginResponse.data.data.token;

// 2. 保存 Token
localStorage.setItem('token', token);

// 3. 后续请求携带 Token
axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

// 4. 连接 WebSocket
const ws = new WebSocket(`ws://domain/ws?token=${token}`);
```

### 8.2 发送消息流程

```javascript
// 1. 发送消息
ws.send(JSON.stringify({
  type: 'MESSAGE',
  data: {
    toUserId: 2,
    chatType: 1,
    msgType: 1,
    content: 'Hello'
  }
}));

// 2. 接收 ACK
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.type === 'ACK') {
    console.log('消息发送成功', message.data.messageId);
  }
};
```

### 8.3 上传文件并发送

```javascript
// 1. 上传文件
const formData = new FormData();
formData.append('file', file);

const uploadResponse = await axios.post('/api/file/upload/image', formData);
const imageUrl = uploadResponse.data.data.url;

// 2. 发送图片消息
ws.send(JSON.stringify({
  type: 'MESSAGE',
  data: {
    toUserId: 2,
    chatType: 1,
    msgType: 2,
    url: imageUrl
  }
}));
```

## 9. 接口测试

### 9.1 使用 Swagger 测试
访问: http://localhost:8080/doc.html

### 9.2 使用 Postman 测试
导入 Postman Collection 文件

### 9.3 使用 curl 测试
```bash
# 登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user001","password":"123456"}'

# 获取用户信息
curl -X GET http://localhost:8080/api/user/info/1 \
  -H "Authorization: Bearer {token}"
```

## 10. 版本更新记录

### v1.0.0 (2024-01-01)
- 初始版本
- 实现基础功能

### v1.1.0 (计划中)
- 添加语音通话功能
- 添加视频通话功能
- 优化性能
