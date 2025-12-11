# 广场 & 聊天模块现状与候选新功能

> 本文档用于记录广场 & 聊天相关：
>
> - **目前已经实现的核心能力（简要说明架构与技术点）**；
> - **尚未实现但已讨论过的功能方向**，方便后续按需排期实现。

---

## 一、广场模块后端已实现能力概览

- **核心帖子模型与可见性控制**
  - 业务实体：`square_post` 表 + `SquarePost` 实体 + `SquarePostVO` 视图对象。
  - 字段包含：标题、正文、图片列表（JSON 数组）、视频 URL、标签（JSON 数组）、可见范围 `visible_type`（0 公开 / 1 仅好友）、`exclude_users`（不给谁看）、状态 `status`、审核状态 `audit_status`、点赞数、评论数、时间戳等。
  - 统一可见性判断方法：`SquareServiceImpl.isPostVisibleToUser` 封装复杂的“仅好友可见 + 排除用户 + 后续黑名单扩展”的可见性规则，在：
    - 列表流（全部 / 热门 / 关注）
    - 个人主页帖子
    - 收藏列表 / 点赞列表
    - 帖子详情
    - 评论级联展示
    中统一复用，保证后端安全边界。

- **帖子发布 / 编辑 / 删除链路**
  - 接口：`/api/square/posts` 发布，`PUT /posts/{postId}` 编辑，`DELETE /posts/{postId}` 删除。
  - 服务：`SquareService.publishPost` / `updatePost` / `deletePost`：
    - 文本内容通过 `ContentReviewService.reviewText` 做审核拦截。
    - 媒体数量校验：图片数量 + 视频数量之和不超过 18，违反直接抛 `BusinessException`。
    - 标签、图片、可见范围、exclude 列表统一做 JSON 序列化入库，VO 层再反序列化成 `List<String>` / `List<Long>`。
    - 软删除（`status = 0`）而不是物理删除，便于后续审计与数据统计。

- **广场主时间线 + Redis 分页缓存**
  - 接口：`GET /api/square/posts` 默认「全部」时间线。
  - Service：`SquareServiceImpl.listPublicPosts`：
    - 使用 `SquarePostMapper.selectPublicList` + `countPublic` 做基础分页查询，仅返回 `status=1` 且 `audit_status=1` 的公开帖子。
    - 使用 `StringRedisTemplate` 维护 `square:post:list:page:{page}:{size}` 缓存：
      - 命中则直接反序列化返回，刷新 TTL；
      - 未命中则走 DB 查询并回写缓存；
      - 包含 `total` 总数一起缓存，避免重复 count。
    - 当发帖、编辑、删帖、点赞/取消点赞、评论/删评、收藏/取消收藏等操作发生时，通过 `clearPublicPostListCache` 清理相关 key，保证列表一致性。

- **热门帖子：Redis ZSet + 数据库回退的热度榜实现**
  - 接口：`GET /api/square/posts/hot`，Service：`SquareServiceImpl.listHotPosts`。
  - 技术要点：
    - 使用 Redis ZSet `square:post:hot:zset` 维护热门帖子 ID → 热度分数：
      - 点赞：`adjustHotScore(post, +2)`
      - 取消点赞：`adjustHotScore(post, -2)`
      - 收藏：`adjustHotScore(post, +4)`
      - 取消收藏：`adjustHotScore(postId, -4)`
      - 评论：`adjustHotScore(post, +3)`
      - 删除评论：`adjustHotScore(comment.postId, -3)`
    - 热度 ZSet 只保留最近 3 天内的帖子：
      - `adjustHotScore` 在新增时检查 `create_time`，超过 3 天直接从 ZSet 移除；
      - `listHotPosts` 在按 ZSet 回读时再次过滤 create_time，避免脏数据。
    - ZSet 容量控制：超过 `HOT_POST_ZSET_MAX_SIZE` 时，自动删除分数最低的多余元素，保证内存可控。
    - 降级策略：
      - 若 Redis 不可用 / ZSet 为空，则回退到 `SquarePostMapper.selectPublicHotList` 的 SQL 排序逻辑：
        - `like_count * 2 + comment_count * 3 + 收藏数 * 4` 作为热度分，带 3 天时间窗口。

- **搜索 & 筛选能力（关键词 / 标签 / 媒体 / 可见范围 / 排序）**
  - 同一接口 `/api/square/posts` 支持：
    - 关键词 `keyword`（标题 + 内容模糊匹配）。
    - 标签 `tags`（逗号分隔多标签，MyBatis XML 中按 JSON 字符串字段 LIKE 匹配）。
    - 是否有图 `hasImage`。
    - 是否有视频 `hasVideo`。
    - 仅好友可见 `visibleType = 1`。
    - 排序 `sort`：按时间 / 评论数 / 点赞数排序。
  - 实现方式：
    - Controller 中根据是否存在关键词/标签/筛选项判断：
      - 无任何筛选 → 走缓存版 `listPublicPosts`；
      - 有任意筛选 → 走 `searchPublicPosts`，不使用 Redis 列表缓存，保证实时性。
    - Mapper：`SquarePostMapper.searchPublic` / `countSearchPublic` 使用 MyBatis 动态 SQL 在单条查询中组合多种条件和排序，避免大量 if/else 分支。

- **互动与计数：点赞 / 收藏 / 评论**
  - 点赞/取消点赞：
    - 表：`square_like`，使用用户 + 帖子联合约束避免重复点赞。
    - 通过 `updateCounters` 对 `like_count` 做增量更新，使用 `GREATEST` 防止负数。
    - 每次操作会触发：
      - Redis 热度 ZSet 更新；
      - 清空列表缓存；
      - 发送点赞通知（见通知模块）。
  - 收藏/取消收藏：
    - 表：`square_favorite`，支持按用户分页查询收藏列表。
    - 热度 ZSet 调整 + 列表缓存清理逻辑与点赞类似。
  - 评论：
    - 表：`square_comment`，字段支持 parentId 以构建二级评论树。
    - 后端按平铺结果构造树形结构返回前端，前端再做按需折叠。
    - 评论也参与热度计算和缓存失效策略。

- **通知模块与广场消息点击联动**
  - 点赞、评论、发帖等事件通过 `RemoteSquareNotificationService` 发送通知。
  - 前端“我的广场消息”页按通知 ID / 类型区分，点击单条通知时：
    - 后端 `getPostDetail` 提供帖子详情。
    - 前端回到「全部」Tab，通过普通帖子列表 API 拉取并滚动定位，避免“单帖详情突兀”问题。

- **用户关系与关注 Feed**
  - 表：`square_follow`，封装为 `SquareFollowMapper` + `SquareService.followUser/unfollowUser`。
  - 接口：`GET /api/square/feed` 返回我关注用户的帖子流，内部基于：
    - 关注表筛选作者 ID；
    - 帖子表按时间排序分页；
    - 复用可见性校验逻辑。
  - 同时通过 Kafka 主题 `im-square-feed` 异步推送新帖事件，可进一步用于做粉丝动态或通知。

- **个人广场主页与统计**
  - 接口：`GET /api/square/profile/{userId}`。
  - 综合调用用户服务 + 关注表 + 帖子表：
    - 粉丝数、关注数、发帖数、总点赞数。
    - 当前登录用户与目标用户的关注关系。
  - 个人帖子 / 收藏 / 点赞过的帖子均有独立分页接口（`/user/{userId}/posts|favorites|likes`），并统一附加：
    - 是否已关注作者、是否已点赞/收藏、收藏数等衍生字段。

---

## 二、广场 & 聊天候选新功能列表

## 1. 帖子搜索 / 标签搜索

- 支持按关键字搜索广场帖子标题和内容。
- 可选扩展：
  - 给帖子增加标签字段（发帖时可多选标签）。
  - 在广场页增加标签筛选 / 搜索入口。
  - 后端提供基于标签的筛选接口（支持多标签 AND/OR 模式）。

## 2. 帖子排序与过滤扩展

- 在现有「全部 / 热门 / 我的关注」基础上增加：
  - 只看带图片的帖子。
  - 只看带视频的帖子。
  - 只看仅好友可见的帖子。
- 支持组合筛选（例如：热门 + 仅图片）。

## 3. 帖子置顶 / 精选区

- 增加“精选”或“置顶”标记字段：
  - 可由管理员或特定角色在后台设置。
- 前端：
  - 在广场顶部增加一个“精选区”模块，展示若干精选帖子。
  - 普通列表中可以选择是否排除已置顶的帖子，避免重复。

## 4. 广场评论更丰富的展示形态

- 在当前二级评论的基础上，进一步增强：
  - 支持“只看作者评论”。
  - 支持“只看我与某人的对话线程”。
  - 评论数很多时，提供“按热度排序 / 按时间排序”切换。

## 5. 广场黑名单 / 屏蔽功能

- 增加用户级屏蔽能力：
  - A 可以拉黑 B：B 看不到 A 的广场帖子，A 的列表中也不出现 B 的帖子。
  - 与现有“不给谁看”列表形成互补：
    - “不给谁看”是针对单条帖子的；
    - 黑名单是全局维度的。
- 后端：
  - 新增黑名单表或复用现有用户黑名单结构。
  - 在广场帖子可见性判断中引入黑名单逻辑。

## 6. 广场主页统计信息丰富化

- 在个人广场主页增加更多统计：
  - 总收藏数、总评论数、近 7 天新增点赞曲线。
  - 粉丝增长趋势、发帖活跃度等。
- 前端可用简单折线图 / 柱状图展示（例如基于 ECharts 或轻量手写）。

## 7. 聊天消息引用 / 回复某条消息

- 支持类似 IM 应用中的“引用回复”：
  - 在某条消息上点击“回复”，新消息气泡中带上一段引用预览（原文、图片缩略图等）。
- 需要：
  - 消息结构中增加 `replyToMessageId` 字段。
  - 前端在渲染时根据该字段展示一段“引用区域”。

## 8. 聊天图片 / 视频查看器优化

- 目前点击图片会在新窗口打开。
- 计划：
  - 在聊天内集成一个图片/视频预览弹层：
    - 支持左右切换同一会话中的所有图片。
    - 支持 ESC 关闭、键盘左右切换。

## 9. 热门榜 & 关注 Feed 的前端预加载

- 在现有 Redis + 后端分页基础上：
  - 对首次加载到的热门列表和关注 Feed 做前端缓存；
  - Tab 切换时优先使用缓存，后台静默刷新，减少闪烁与接口压力。

---

> 以上为当前阶段的候选功能，后续新增想法也可以继续补充到这个文档中。
