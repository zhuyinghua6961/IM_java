-- 单聊消息异步持久化方案 - 数据库迁移脚本
-- 执行日期：2025-11-27

USE im_chat;

-- 1. 为message表添加persist_status字段
ALTER TABLE message 
ADD COLUMN persist_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '持久化状态：PENDING-待持久化, PERSISTED-已持久化' 
AFTER recall_time;

-- 2. 为persist_status字段添加索引（用于补偿任务查询）
CREATE INDEX idx_persist_status ON message(persist_status, send_time);

-- 3. 修改message表的id为非自增（使用雪花算法生成ID）
ALTER TABLE message MODIFY COLUMN id BIGINT NOT NULL COMMENT '消息ID（雪花算法生成）';

-- 4. 更新现有数据的persist_status为PERSISTED
UPDATE message SET persist_status = 'PERSISTED' WHERE persist_status IS NULL OR persist_status = '';

-- 验证
SELECT COUNT(*) as total_messages FROM message;
SELECT persist_status, COUNT(*) as count FROM message GROUP BY persist_status;

-- 说明：
-- 1. persist_status字段用于标识消息的持久化状态
-- 2. PENDING表示消息已缓存到Redis，但还未写入MySQL
-- 3. PERSISTED表示消息已成功写入MySQL
-- 4. 补偿任务会定期检查PENDING状态超过5分钟的消息，重新发送到Kafka
-- 5. 撤回操作会根据persist_status决定处理策略
