# AI 客服机器人实现文档

## 概述

实现一个基于 AI 的智能客服机器人，用户可以添加官方客服号为好友，发送消息自动获得 AI 回复。

### 技术方案

- **LLM 服务**: DeepSeek API（性价比高，效果好）
- **知识库**: MySQL FAQ 表存储常见问题
- **消息处理**: 在 WebSocket 消息处理中识别客服消息

---

## 步骤一：创建 FAQ 知识库表

### 1.1 创建 FAQ 表 SQL

在 `sql/` 目录下创建 `faq_table.sql`:

```sql
-- ----------------------------
-- AI客服知识库表
-- ----------------------------
DROP TABLE IF EXISTS `faq`;
CREATE TABLE `faq` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `question` VARCHAR(500) NOT NULL COMMENT '问题',
  `answer` TEXT NOT NULL COMMENT '答案',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
  `keywords` VARCHAR(255) DEFAULT NULL COMMENT '关键词(逗号分隔)',
  `priority` INT DEFAULT 0 COMMENT '优先级(越大越优先)',
  `status` TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI客服知识库表';
```

### 1.2 初始化 FAQ 数据

```sql
INSERT INTO `faq` (`question`, `answer`, `category`, `keywords`, `priority`, `status`) VALUES
('如何修改密码？', '进入"我的-设置-账号安全-修改密码"，输入旧密码和新密码即可完成修改。', '账号', '密码 修改 找回', 10, 1),
('如何注销账号？', '进入"我的-设置-账号安全-注销账号"，注销后所有数据将被清除且不可恢复，请谨慎操作。', '账号', '注销 删除账号 销号', 10, 1),
('会员多少钱？', '月度会员19元，年度会员168元，连续包月享9折优惠。开通后享受专属表情、个性装饰等特权。', '会员', '会员 收费 价格 开通 付费', 10, 1),
('如何退款？', '未使用的虚拟服务可在7天内申请退款，请联系客服处理。实物商品请按退换货政策处理。', '财务', '退款 退钱 退还', 5, 1),
('如何添加好友？', '在搜索框输入对方用户名或手机号，找到后点击"添加好友"，等待对方同意即可。', '功能', '添加好友 加好友 朋友', 5, 1),
('如何创建群聊？', '点击右上角"+"，选择"创建群聊"，选择好友后点击"完成"即可创建群聊。', '功能', '创建群聊 建群 群组', 5, 1),
('消息撤回有时间限制吗？', '消息发出后2分钟内可以撤回，超时后无法撤回。', '功能', '撤回 删除消息 收回', 5, 1),
('如何联系人工客服？', '请发送"转人工"或"人工客服"，我们将为您转接专人服务。', '客服', '人工 客服 转人工', 20, 1),
('如何设置免打扰？', '进入聊天详情页，点击右上角设置，开启"消息免打扰"即可。', '功能', '免打扰 静音 消息免打扰', 5, 1),
('如何删除聊天记录？', '进入聊天详情页，点击"清空聊天记录"即可删除所有聊天记录。', '功能', '删除 清空 聊天记录', 5, 1);
```

---

## 步骤二：创建 Faq 实体类

在 `im-common` 模块中创建：

**文件路径**: `im-common/src/main/java/com/im/common/entity/Faq.java`

```java
package com.im.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI客服知识库实体
 */
@Data
@TableName("faq")
public class Faq {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 问题
     */
    private String question;

    /**
     * 答案
     */
    private String answer;

    /**
     * 分类
     */
    private String category;

    /**
     * 关键词(逗号分隔)
     */
    private String keywords;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

---

## 步骤三：创建 FaqMapper

在 `im-user-service` 模块中创建：

**文件路径**: `im-user-service/src/main/java/com/im/user/mapper/FaqMapper.java`

```java
package com.im.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.im.common.entity.Faq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FAQ Mapper
 */
@Mapper
public interface FaqMapper extends BaseMapper<Faq> {

    /**
     * 根据关键词搜索FAQ
     */
    @Select("SELECT * FROM faq WHERE status = 1 " +
            "AND (question LIKE CONCAT('%', #{keyword}, '%') " +
            "OR keywords LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY priority DESC LIMIT #{limit}")
    List<Faq> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 根据分类查询FAQ
     */
    @Select("SELECT * FROM faq WHERE status = 1 AND category = #{category} ORDER BY priority DESC")
    List<Faq> findByCategory(@Param("category") String category);
}
```

---

## 步骤四：创建 DeepSeek API 配置和客户端

### 4.1 添加依赖

在 `im-common/pom.xml` 中添加：

```xml
<!-- DeepSeek API 调用 -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>

<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>2.0.43</version>
</dependency>
```

### 4.2 添加配置

在 `im-common/src/main/java/com/im/common/config/` 创建 `DeepSeekConfig.java`:

```java
package com.im.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API 地址
     */
    private String apiUrl = "https://api.deepseek.com/chat/completions";

    /**
     * 模型名称
     */
    private String model = "deepseek-chat";

    /**
     * 超时时间(秒)
     */
    private int timeout = 30;

    /**
     * 最大返回长度
     */
    private int maxTokens = 512;
}
```

### 4.3 添加配置项

在各服务的 `application.yml` 或 `application-dev.yml` 中添加：

```yaml
deepseek:
  api-key: sk-your-api-key-here
  api-url: https://api.deepseek.com/chat/completions
  model: deepseek-chat
  timeout: 30
  max-tokens: 512
```

### 4.4 创建 DeepSeek 客户端

在 `im-common` 模块中创建 `DeepSeekClient.java`:

**文件路径**: `im-common/src/main/java/com/im/common/util/DeepSeekClient.java`

```java
package com.im.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.im.common.config.DeepSeekConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API 客户端
 */
@Slf4j
@Component
public class DeepSeekClient {

    @Autowired
    private DeepSeekConfig deepSeekConfig;

    private final OkHttpClient client;

    public DeepSeekClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 简单对话调用
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * 带系统提示词的对话调用
     */
    public String chat(String userMessage, String systemPrompt) {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", deepSeekConfig.getModel());

        // 构建消息列表
        JSONArray messages = new JSONArray();

        // 系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
        }

        // 用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("temperature", 0.7);

        // 发送请求
        return sendRequest(requestBody.toJSONString());
    }

    /**
     * 发送请求
     */
    private String sendRequest(String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(deepSeekConfig.getApiUrl())
                .addHeader("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DeepSeek API 调用失败, code: {}", response.code());
                return "抱歉，我现在有点忙，请稍后再试。";
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseResponse(responseBody);

        } catch (IOException e) {
            log.error("DeepSeek API 调用异常", e);
            return "抱歉，服务器出了点问题，请稍后再试。";
        }
    }

    /**
     * 解析响应
     */
    private String parseResponse(String responseBody) {
        try {
            JSONObject jsonObject = JSON.parseObject(responseBody);
            JSONArray choices = jsonObject.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    return message.getString("content");
                }
            }
            return "抱歉，我没有理解您的问题。";
        } catch (Exception e) {
            log.error("解析 DeepSeek 响应失败", e);
            return "抱歉，出了点问题，请稍后再试。";
        }
    }
}
```

---

## 步骤五：创建 AiService

在 `im-user-service` 模块中创建：

**文件路径**: `im-user-service/src/main/java/com/im/user/service/AiService.java`

```java
package com.im.user.service;

import com.im.common.entity.Faq;
import com.im.common.util.DeepSeekClient;
import com.im.user.mapper.FaqMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * AI客服服务
 */
@Slf4j
@Service
public class AiService {

    @Autowired
    private FaqMapper faqMapper;

    @Autowired
    private DeepSeekClient deepSeekClient;

    /**
     * 客服账号ID（固定为0表示官方客服）
     */
    public static final long CUSTOMER_SERVICE_ID = 0L;

    /**
     * 生成客服回复
     */
    public String generateReply(String userQuestion) {
        try {
            // 1. 检索知识库，获取相关FAQ
            List<Faq> relatedFaqs = searchRelatedFaqs(userQuestion);

            // 2. 构建提示词
            String prompt = buildPrompt(userQuestion, relatedFaqs);

            // 3. 调用LLM
            String answer = deepSeekClient.chat(userQuestion, prompt);

            return answer;
        } catch (Exception e) {
            log.error("生成AI回复失败", e);
            return "抱歉，我现在有点忙，请稍后再试。";
        }
    }

    /**
     * 检索相关FAQ
     */
    private List<Faq> searchRelatedFaqs(String question) {
        // 简单关键词匹配：提取问题中的关键词
        String keyword = extractKeyword(question);
        if (keyword == null || keyword.isEmpty()) {
            keyword = question;
        }

        // 限制返回5条
        return faqMapper.searchByKeyword(keyword, 5);
    }

    /**
     * 从问题中提取关键词
     */
    private String extractKeyword(String question) {
        // 简单处理：取前两个词作为关键词
        // 实际可以使用分词工具如 IK Analyzer
        String[] words = question.split("[，。？！、\\s]+");
        if (words.length > 0 && !words[0].isEmpty()) {
            return words[0];
        }
        return question;
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String question, List<Faq> faqs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能客服，请根据以下知识库回答用户问题。\n\n");

        if (faqs != null && !faqs.isEmpty()) {
            sb.append("【知识库】\n");
            for (Faq faq : faqs) {
                sb.append("Q: ").append(faq.getQuestion()).append("\n");
                sb.append("A: ").append(faq.getAnswer()).append("\n\n");
            }
        }

        sb.append("【用户问题】\n");
        sb.append(question).append("\n\n");

        sb.append("请用简洁友好的语气回答。如果知识库中有相关内容，优先使用知识库中的答案；");
        sb.append("如果没有找到相关信息，请说\"这个问题我暂时无法解答，建议您联系人工客服\"。");
        sb.append("回答不要过长，尽量控制在100字以内。");

        return sb.toString();
    }
}
```

---

## 步骤六：修改 WebSocket 消息处理

修改 `im-message-service` 中的消息处理器：

**文件路径**: `im-message-service/src/main/java/com/im/message/handler/WebSocketMessageHandler.java`

在 `handleMessage` 方法中添加客服消息处理逻辑：

```java
// 在 handleMessage 方法中，消息保存之后、推送之前添加以下逻辑

// 判断是否是发给客服的消息
if (toUserId.equals(AiService.CUSTOMER_SERVICE_ID)) {
    // 是客服消息，调用AI处理
    String aiReply = aiService.generateReply(content);

    // 构造AI回复消息
    Message replyMsg = new Message();
    replyMsg.setFromUserId(AiService.CUSTOMER_SERVICE_ID);
    replyMsg.setToUserId(fromUserId);
    replyMsg.setChatType(ChatType.SINGLE.getCode());
    replyMsg.setMsgType(MessageType.TEXT.getCode());
    replyMsg.setContent(aiReply);
    replyMsg.setSendTime(System.currentTimeMillis());
    replyMsg.setStatus(MessageStatus.SENT.getCode());

    // 保存消息到数据库
    messageService.saveMessage(replyMsg);

    // 更新会话
    messageService.updateOrCreateConversation(fromUserId, AiService.CUSTOMER_SERVICE_ID,
            ChatType.SINGLE.getCode(), replyMsg);

    // 发送ACK给发送方（可选）
    sendMessageAck(fromUserId, messageId);

    // 通过WebSocket推送给用户
    messagingTemplate.convertAndSendToUser(
            fromUserId.toString(), "/queue/messages", replyMsg);

    log.info("AI客服回复用户: from={}, to={}, content={}",
            AiService.CUSTOMER_SERVICE_ID, fromUserId, aiReply);
    return;
}
```

### 6.1 添加依赖注入

在 `WebSocketMessageHandler` 中注入 `AiService`:

```java
@Autowired
private AiService aiService;
```

---

## 步骤七：配置和使用

### 7.1 获取 DeepSeek API Key

1. 访问 [DeepSeek 开放平台](https://platform.deepseek.com/)
2. 注册账号并获取 API Key

### 7.2 配置 API Key

在各服务的 `application-dev.yml` 中配置：

```yaml
deepseek:
  api-key: sk-your-deepseek-api-key
  api-url: https://api.deepseek.com/chat/completions
  model: deepseek-chat
  timeout: 30
  max-tokens: 512
```

### 7.3 使用方式

1. 用户搜索用户名为 `0` 或 `客服` 的官方账号
2. 添加客服为好友
3. 发送消息，AI 会自动回复

---

## 常见问题

### Q1: 如何判断用户是否在询问知识库中的问题？

当前方案是简单的关键词匹配，可以优化为：
- 使用分词工具（IK Analyzer、HanLP）
- 使用向量数据库 + 语义搜索
- 使用 ElasticSearch

### Q2: 如何处理敏感内容？

可以在 `AiService` 中添加内容审核：
- 调用阿里云内容安全 API
- 敏感词过滤
- 违规内容拦截

### Q3: 如何转人工客服？

在 FAQ 中添加引导：
- 用户输入"转人工"、"人工客服"等关键词
- 返回人工客服联系方式
- 或者创建一个工单系统

### Q4: API 调用失败怎么办？

当前已做基础容错：
- 可以添加重试机制
- 添加降级策略（API 失败时返回默认回复）
- 添加限流保护

---

## 后续优化

1. **对话历史**: 支持多轮对话上下文
2. **用户画像**: 根据用户历史行为优化回复
3. **满意度评价**: 用户可以对回复进行评价
4. **数据分析**: 分析用户常见问题，优化知识库
5. **多渠道支持**: 支持微信公众号、小程序等
