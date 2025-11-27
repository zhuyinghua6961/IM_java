# IM Chat 前端项目

## 项目说明

基于 Vue 3 + Element Plus 的即时通讯前端应用。

## 已创建的文件

### ✅ 配置文件
- `package.json` - 项目依赖配置
- `vite.config.js` - Vite 构建配置
- `index.html` - HTML 入口文件

### ✅ 核心文件
- `src/main.js` - 应用入口
- `src/App.vue` - 根组件
- `src/router/index.js` - 路由配置

### ✅ 状态管理 (Pinia)
- `src/stores/user.js` - 用户状态
- `src/stores/chat.js` - 聊天状态

### ✅ 工具类
- `src/utils/request.js` - Axios 封装
- `src/utils/websocket.js` - WebSocket 封装

### ✅ API 接口
- `src/api/user.js` - 用户相关接口
- `src/api/friend.js` - 好友相关接口
- `src/api/message.js` - 消息相关接口

### ✅ 视图组件
- `src/views/Login.vue` - 登录页面
- `src/views/Home.vue` - 主页布局

## 待创建的视图组件

需要继续创建以下组件：

### 📝 Chat.vue - 聊天页面
```vue
<template>
  <div class="chat-container">
    <!-- 会话列表 -->
    <div class="conversation-list">
      <!-- 搜索框 -->
      <!-- 会话列表 -->
    </div>
    
    <!-- 聊天窗口 -->
    <div class="chat-window">
      <!-- 聊天头部 -->
      <!-- 消息列表 -->
      <!-- 输入框 -->
    </div>
  </div>
</template>
```

### 📝 Contacts.vue - 通讯录页面
```vue
<template>
  <div class="contacts-container">
    <!-- 好友列表 -->
    <!-- 群组列表 -->
    <!-- 添加好友按钮 -->
  </div>
</template>
```

### 📝 Moments.vue - 朋友圈页面
```vue
<template>
  <div class="moments-container">
    <!-- 发布动态按钮 -->
    <!-- 动态列表 -->
  </div>
</template>
```

### 📝 Profile.vue - 个人中心页面
```vue
<template>
  <div class="profile-container">
    <!-- 个人信息 -->
    <!-- 设置选项 -->
  </div>
</template>
```

## 快速开始

### 1. 安装依赖
```bash
cd im-frontend
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```

访问: http://localhost:5173

### 3. 构建生产版本
```bash
npm run build
```

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vue Router** - 官方路由管理器
- **Pinia** - 新一代状态管理
- **Element Plus** - Vue 3 UI 组件库
- **Axios** - HTTP 客户端
- **SockJS + Stomp** - WebSocket 通信
- **Vite** - 下一代前端构建工具

## 项目结构

```
im-frontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API 接口
│   │   ├── user.js
│   │   ├── friend.js
│   │   └── message.js
│   ├── assets/            # 资源文件
│   ├── components/        # 公共组件
│   ├── router/            # 路由配置
│   │   └── index.js
│   ├── stores/            # 状态管理
│   │   ├── user.js
│   │   └── chat.js
│   ├── utils/             # 工具类
│   │   ├── request.js
│   │   └── websocket.js
│   ├── views/             # 页面组件
│   │   ├── Login.vue
│   │   ├── Home.vue
│   │   ├── Chat.vue       # 待创建
│   │   ├── Contacts.vue   # 待创建
│   │   ├── Moments.vue    # 待创建
│   │   └── Profile.vue    # 待创建
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

## 功能特性

### ✅ 已实现
- 用户登录/注册
- 路由守卫
- Token 认证
- WebSocket 连接
- 状态管理
- 请求拦截

### 🚧 待实现
- 聊天界面
- 消息发送/接收
- 文件上传
- 好友管理
- 群组管理
- 朋友圈功能
- 个人设置

## 开发规范

### 组件命名
- 使用 PascalCase 命名组件文件
- 使用 kebab-case 命名组件标签

### 代码风格
- 使用 Composition API
- 使用 `<script setup>` 语法
- 使用 ESLint 检查代码

### 提交规范
```bash
feat: 新功能
fix: Bug修复
docs: 文档更新
style: 代码格式
refactor: 重构
perf: 性能优化
test: 测试
chore: 构建/工具
```

## 环境变量

创建 `.env.development` 文件：
```
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8082
```

创建 `.env.production` 文件：
```
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_WS_URL=wss://ws.yourdomain.com
```

## 常见问题

### Q1: 如何调试 WebSocket？
A: 打开浏览器开发者工具 -> Network -> WS 标签

### Q2: 如何处理跨域？
A: 在 vite.config.js 中配置 proxy

### Q3: 如何优化打包体积？
A: 使用按需导入、代码分割、压缩等

## 下一步

1. 创建 Chat.vue 聊天组件
2. 创建 Contacts.vue 通讯录组件
3. 创建 Moments.vue 朋友圈组件
4. 创建 Profile.vue 个人中心组件
5. 完善消息发送功能
6. 添加文件上传功能
7. 优化用户体验

## 参考资料

- [Vue 3 文档](https://vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)
- [Pinia 文档](https://pinia.vuejs.org/)
- [Vite 文档](https://vitejs.dev/)
