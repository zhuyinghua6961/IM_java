# IM Chat System - 仿微信聊天系统

## 项目简介

基于 Spring Boot + Vue 3 的分布式即时通讯系统，实现了单聊、群聊、朋友圈等核心功能。

## 技术栈

### 后端
- Spring Boot 3.2.x
- Spring WebSocket
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 7.x
- Apache Kafka 3.x
- MinIO
- JWT
- Knife4j (Swagger)

### 前端
- Vue 3
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- Vite

## 功能模块

### 已实现功能
- [ ] 用户注册登录
- [ ] 好友管理(添加、删除、查询)
- [ ] 单聊功能
- [ ] 群聊功能
- [ ] 消息已读/未读
- [ ] 消息撤回
- [ ] 文件上传(图片、视频、文档)
- [ ] 朋友圈发布
- [ ] 朋友圈点赞评论
- [ ] 在线状态显示
- [ ] 离线消息推送

### 待实现功能
- [ ] 语音/视频通话
- [ ] 消息搜索
- [ ] 表情包管理
- [ ] 红包功能

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Kafka 3.x
- MinIO

### 后端启动

1. 克隆项目
```bash
git clone <repository-url>
cd IM_java
```

2. 创建数据库
```sql
CREATE DATABASE im_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改配置文件
```yaml
# 修改各服务的 application.yml
# 配置数据库、Redis、Kafka、MinIO连接信息
```

4. 启动基础服务
```bash
# 启动 Redis
redis-server

# 启动 Kafka (需先启动 Zookeeper)
zookeeper-server-start.sh config/zookeeper.properties
kafka-server-start.sh config/server.properties

# 启动 MinIO
minio server /data --console-address ":9001"
```

5. 启动微服务
```bash
# 按顺序启动各服务
cd im-gateway && mvn spring-boot:run
cd im-user-service && mvn spring-boot:run
cd im-message-service && mvn spring-boot:run
cd im-moments-service && mvn spring-boot:run
cd im-file-service && mvn spring-boot:run
```

### 前端启动

```bash
cd im-frontend
npm install
npm run dev
```

访问: http://localhost:5173

## Docker 部署

```bash
# 使用 docker-compose 一键启动所有服务
docker-compose up -d
```

## API 文档

启动后访问 Swagger 文档:
- 网关: http://localhost:8080/doc.html
- 用户服务: http://localhost:8081/doc.html
- 消息服务: http://localhost:8082/doc.html
- 朋友圈服务: http://localhost:8083/doc.html
- 文件服务: http://localhost:8084/doc.html

## 项目结构

```
im-chat-system/
├── im-common/              # 公共模块
├── im-gateway/             # 网关服务 (8080)
├── im-user-service/        # 用户服务 (8081)
├── im-message-service/     # 消息服务 (8082)
├── im-moments-service/     # 朋友圈服务 (8083)
├── im-file-service/        # 文件服务 (8084)
├── im-frontend/            # 前端项目
└── docker-compose.yml      # Docker编排文件
```

## 核心流程

### 消息发送流程
1. 客户端通过 WebSocket 发送消息
2. 消息服务接收并生成消息ID
3. 发送到 Kafka 消息队列
4. 消费者持久化到 MySQL
5. 推送给在线用户或存储离线消息
6. 更新会话和未读数

### 朋友圈发布流程
1. 上传图片/视频到 MinIO
2. 提交朋友圈内容
3. 持久化到数据库
4. 异步推送给好友
5. 缓存热点数据到 Redis

## 性能指标

- 单机支持 10w+ 并发连接
- 消息延迟 < 100ms
- 接口响应时间 < 200ms
- 支持水平扩展

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 开源协议

本项目采用 MIT 协议

## 联系方式

- 项目地址: https://github.com/yourusername/im-chat-system
- 问题反馈: https://github.com/yourusername/im-chat-system/issues

## 致谢

感谢所有为本项目做出贡献的开发者！
