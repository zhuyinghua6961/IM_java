-- 修复好友申请数据：清理状态不一致的数据
USE im_chat;

-- 1. 查看已同意但好友关系不完整的申请
SELECT 
    fr.id,
    fr.from_user_id,
    fr.to_user_id,
    fr.status,
    f1.id as friend1_id,
    f2.id as friend2_id
FROM friend_request fr
LEFT JOIN friend f1 ON (f1.user_id = fr.from_user_id AND f1.friend_id = fr.to_user_id)
LEFT JOIN friend f2 ON (f2.user_id = fr.to_user_id AND f2.friend_id = fr.from_user_id)
WHERE fr.status = 1
  AND (f1.id IS NULL OR f2.id IS NULL);

-- 2. 对于已经有好友关系的申请，保持状态为已同意（不需要重置）
-- 对于没有好友关系的申请，重置为待处理
UPDATE friend_request fr
LEFT JOIN friend f1 ON (f1.user_id = fr.from_user_id AND f1.friend_id = fr.to_user_id)
LEFT JOIN friend f2 ON (f2.user_id = fr.to_user_id AND f2.friend_id = fr.from_user_id)
SET fr.status = 0, fr.update_time = NOW()
WHERE fr.status = 1
  AND (f1.id IS NULL OR f2.id IS NULL);

-- 3. 查看结果
SELECT '已重置的申请:' as info;
SELECT * FROM friend_request WHERE status = 0;
