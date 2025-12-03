package com.im.user.service.impl;

import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.user.context.UserContext;
import com.im.user.dto.FriendRequestDTO;
import com.im.user.dto.FriendRemarkDTO;
import com.im.user.entity.Blacklist;
import com.im.user.entity.Friend;
import com.im.user.entity.FriendRequest;
import com.im.user.entity.User;
import com.im.user.mapper.BlacklistMapper;
import com.im.user.mapper.FriendMapper;
import com.im.user.mapper.FriendRequestMapper;
import com.im.user.mapper.UserMapper;
import com.im.user.service.FriendService;
import com.im.user.utils.WebSocketUtil;
import com.im.user.vo.BlacklistVO;
import com.im.user.vo.FriendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FriendServiceImpl implements FriendService {
    
    @Autowired
    private FriendMapper friendMapper;
    
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private WebSocketUtil webSocketUtil;
    
    @Autowired
    private BlacklistMapper blacklistMapper;
    
    @Override
    public List<FriendVO> getFriendList() {
        // 从UserContext获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        log.info("获取好友列表，userId: {}", userId);
        
        // 查询好友列表（包含用户信息）
        List<Map<String, Object>> friendList = friendMapper.selectFriendListWithUserInfo(userId);
        
        // 转换为VO
        List<FriendVO> result = new ArrayList<>();
        for (Map<String, Object> map : friendList) {
            FriendVO vo = new FriendVO();
            vo.setUserId(((Number) map.get("userId")).longValue());
            vo.setUsername((String) map.get("username"));
            vo.setNickname((String) map.get("nickname"));
            vo.setAvatar((String) map.get("avatar"));
            vo.setGender(map.get("gender") != null ? ((Number) map.get("gender")).intValue() : 0);
            vo.setSignature((String) map.get("signature"));
            vo.setRemark((String) map.get("remark"));
            vo.setOnline(false); // TODO: 后续实现在线状态
            result.add(vo);
        }
        
        log.info("获取好友列表成功，数量: {}", result.size());
        return result;
    }


    @Override
    public FriendRequest addFriendRequest(FriendRequestDTO dto) {
        // 获取当前登录用户ID
        Long userId = UserContext.getCurrentUserId();
        Long friendId = dto.getFriendId();
        
        log.info("发送好友申请，fromUserId: {}, toUserId: {}", userId, friendId);
        
        // 1. 验证不能加自己为好友
        if (userId.equals(friendId)) {
            throw new BusinessException(ResultCode.CANNOT_ADD_SELF);
        }
        
        // 2. 验证对方用户是否存在
        User targetUser = userMapper.selectById(friendId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 3. 检查是否已经是好友（只检查有效的好友关系）
        Friend existFriend = friendMapper.selectActiveFriendship(userId, friendId);
        if (existFriend != null) {
            throw new BusinessException(ResultCode.FRIEND_EXIST);
        }
        
        // 4. 检查是否已经发送过申请（待处理状态）
        FriendRequest pendingRequest = friendRequestMapper.selectPendingRequest(userId, friendId);
        if (pendingRequest != null) {
            log.warn("已存在待处理的好友申请，requestId: {}", pendingRequest.getId());
            return pendingRequest; // 返回已存在的申请
        }
        
        // 5. 创建好友申请记录
        FriendRequest newRequest = new FriendRequest();
        newRequest.setFromUserId(userId);
        newRequest.setToUserId(friendId);
        newRequest.setMessage(dto.getMessage());
        newRequest.setStatus(0); // 0-待处理
        
        friendRequestMapper.insert(newRequest);
        
        log.info("好友申请发送成功，requestId: {}", newRequest.getId());
        
        // 通过WebSocket推送通知给对方
        User currentUser = userMapper.selectById(userId);
        String nickname = currentUser != null ? currentUser.getNickname() : "未知用户";
        webSocketUtil.pushFriendRequest(
            friendId, 
            userId, 
            nickname, 
            dto.getMessage()
        );
        
        return newRequest;
    }

    @Override
    public List<FriendRequest> getFriendRequestList() {
        // 获取当前用户ID
        Long userId = UserContext.getCurrentUserId();
        log.info("获取好友申请列表，userId: {}", userId);
        
        // 查询发给当前用户的所有好友申请（待处理和已处理的）
        List<FriendRequest> requests = friendRequestMapper.selectByToUserId(userId);
        
        log.info("获取好友申请列表成功，数量: {}", requests.size());
        return requests;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendRequest handleFriendRequest(FriendRequestDTO dto) {
        // 1. 获取当前用户ID（被申请方）
        Long userId = UserContext.getCurrentUserId();
        log.info("处理好友申请，userId: {}, requestId: {}, status: {}", userId, dto.getRequestId(), dto.getStatus());
        
        // 2. 根据申请ID查询好友申请记录
        FriendRequest request = friendRequestMapper.selectById(dto.getRequestId());
        if (request == null) {
            throw new BusinessException(ResultCode.FRIEND_REQUEST_NOT_EXIST);
        }
        
        // 3. 验证当前用户是否是被申请方（权限校验）
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        
        // 4. 验证申请状态（只能处理待处理的申请）
        if (request.getStatus() != 0) {
            log.warn("好友申请已处理，requestId: {}, status: {}", request.getId(), request.getStatus());
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已处理");
        }
        
        // 5. 根据操作类型处理
        if (dto.getStatus() == 1) {
            // 同意好友申请
            log.info("同意好友申请，fromUserId: {}, toUserId: {}", request.getFromUserId(), request.getToUserId());
            
            // 5.1 检查好友关系是否已存在（避免重复插入）
            Friend existFriend1 = friendMapper.selectByUserIdAndFriendId(request.getFromUserId(), request.getToUserId());
            Friend existFriend2 = friendMapper.selectByUserIdAndFriendId(request.getToUserId(), request.getFromUserId());
            
            // 5.2 更新申请状态为"已同意"
            friendRequestMapper.updateStatus(request.getId(), 1);
            
            // 5.3 处理好友关系（如果存在就更新状态，不存在就插入）
            if (existFriend1 != null) {
                if (existFriend1.getStatus() != 1) {
                    // 更新为正常状态
                    existFriend1.setStatus(1);
                    friendMapper.updateById(existFriend1);
                    log.info("更新好友关系状态: {} -> {}", request.getFromUserId(), request.getToUserId());
                }
            } else {
                Friend friend1 = new Friend();
                friend1.setUserId(request.getFromUserId());
                friend1.setFriendId(request.getToUserId());
                friend1.setStatus(1);
                friendMapper.insert(friend1);
                log.info("插入好友关系: {} -> {}", request.getFromUserId(), request.getToUserId());
            }
            
            if (existFriend2 != null) {
                if (existFriend2.getStatus() != 1) {
                    // 更新为正常状态
                    existFriend2.setStatus(1);
                    friendMapper.updateById(existFriend2);
                    log.info("更新好友关系状态: {} -> {}", request.getToUserId(), request.getFromUserId());
                }
            } else {
                Friend friend2 = new Friend();
                friend2.setUserId(request.getToUserId());
                friend2.setFriendId(request.getFromUserId());
                friend2.setStatus(1);
                friendMapper.insert(friend2);
                log.info("插入好友关系: {} -> {}", request.getToUserId(), request.getFromUserId());
            }
            
            log.info("好友关系处理完成");
            
            // WebSocket推送给申请方："XXX同意了你的好友申请"
            User currentUser = userMapper.selectById(userId);
            String nickname = currentUser != null ? currentUser.getNickname() : "未知用户";
            webSocketUtil.pushFriendRequestResult(
                request.getFromUserId(), 
                userId, 
                nickname, 
                true
            );
            
        } else if (dto.getStatus() == 2) {
            // 拒绝好友申请
            log.info("拒绝好友申请，requestId: {}", request.getId());
            
            // 5.3 更新申请状态为"已拒绝"
            friendRequestMapper.updateStatus(request.getId(), 2);
            
            // WebSocket推送给申请方："XXX拒绝了你的好友申请"
            User currentUser = userMapper.selectById(userId);
            String nickname = currentUser != null ? currentUser.getNickname() : "未知用户";
            webSocketUtil.pushFriendRequestResult(
                request.getFromUserId(), 
                userId, 
                nickname, 
                false
            );
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的操作类型");
        }
        
        // 6. 返回更新后的申请记录
        request.setStatus(dto.getStatus());
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long friendId) {
        // 1. 获取当前用户ID
        Long userId = UserContext.getCurrentUserId();
        log.info("删除好友，userId: {}, friendId: {}", userId, friendId);
        
        // 2. 验证有效好友关系是否存在
        Friend friendship = friendMapper.selectActiveFriendship(userId, friendId);
        if (friendship == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "好友关系不存在或已删除");
        }
        
        // 3. 软删除双向好友关系
        // 删除：当前用户 → 对方
        friendMapper.deleteById(friendship.getId());
        log.info("删除好友关系: {} -> {}", userId, friendId);
        
        // 删除：对方 → 当前用户
        Friend reverseFriendship = friendMapper.selectActiveFriendship(friendId, userId);
        if (reverseFriendship != null) {
            friendMapper.deleteById(reverseFriendship.getId());
            log.info("删除好友关系: {} -> {}", friendId, userId);
        } else {
            log.warn("反向好友关系不存在或已删除: {} -> {}", friendId, userId);
        }
        
        log.info("删除好友成功，双向关系已解除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(FriendRemarkDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (dto == null || dto.getFriendId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "好友ID不能为空");
        }

        Long friendId = dto.getFriendId();
        log.info("更新好友备注，userId: {}, friendId: {}, remark: {}", userId, friendId, dto.getRemark());

        Friend friendship = friendMapper.selectActiveFriendship(userId, friendId);
        if (friendship == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "好友关系不存在或已删除");
        }

        friendship.setRemark(dto.getRemark());
        friendship.setStatus(1);
        friendMapper.updateById(friendship);

        log.info("更新好友备注成功，userId: {}, friendId: {}", userId, friendId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void blockUser(Long targetUserId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("拉黑用户，userId: {}, targetUserId: {}", userId, targetUserId);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户ID不能为空");
        }
        
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能拉黑自己");
        }
        
        // 检查目标用户是否存在
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标用户不存在");
        }
        
        // 检查是否已经拉黑
        Blacklist existing = blacklistMapper.selectByUserIdAndBlockedUserId(userId, targetUserId);
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "已经拉黑该用户");
            }
            // 重新激活拉黑记录
            blacklistMapper.updateStatus(existing.getId(), 1);
        } else {
            // 创建新的拉黑记录
            Blacklist blacklist = new Blacklist();
            blacklist.setUserId(userId);
            blacklist.setBlockedUserId(targetUserId);
            blacklist.setStatus(1);
            blacklistMapper.insert(blacklist);
        }
        
        log.info("拉黑用户成功，userId: {}, targetUserId: {}", userId, targetUserId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unblockUser(Long targetUserId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("取消拉黑，userId: {}, targetUserId: {}", userId, targetUserId);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        if (targetUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户ID不能为空");
        }
        
        // 查找拉黑记录
        Blacklist existing = blacklistMapper.selectByUserIdAndBlockedUserId(userId, targetUserId);
        if (existing == null || existing.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未拉黑该用户");
        }
        
        // 更新状态为已解除
        blacklistMapper.updateStatus(existing.getId(), 0);
        
        log.info("取消拉黑成功，userId: {}, targetUserId: {}", userId, targetUserId);
    }
    
    @Override
    public List<BlacklistVO> getBlacklist() {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取黑名单列表，userId: {}", userId);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 查询黑名单
        List<Blacklist> blacklist = blacklistMapper.selectByUserId(userId);
        
        // 转换为VO
        List<BlacklistVO> result = new ArrayList<>();
        for (Blacklist item : blacklist) {
            BlacklistVO vo = new BlacklistVO();
            vo.setId(item.getId());
            vo.setBlockedUserId(item.getBlockedUserId());
            vo.setCreateTime(item.getCreateTime());
            
            // 获取被拉黑用户信息
            User blockedUser = userMapper.selectById(item.getBlockedUserId());
            if (blockedUser != null) {
                vo.setUsername(blockedUser.getUsername());
                vo.setNickname(blockedUser.getNickname());
                vo.setAvatar(blockedUser.getAvatar());
            }
            
            result.add(vo);
        }
        
        log.info("获取黑名单列表成功，userId: {}, count: {}", userId, result.size());
        return result;
    }
    
    @Override
    public boolean isBlocked(Long targetUserId) {
        Long userId = UserContext.getCurrentUserId();
        
        if (userId == null || targetUserId == null) {
            return false;
        }
        
        // 双向检查是否被拉黑
        Blacklist blocked = blacklistMapper.checkBlocked(userId, targetUserId);
        return blocked != null;
    }
    
    @Override
    public boolean checkBlockedInternal(Long blockerId, Long blockedId) {
        log.info("=== 内部黑名单检查: blockerId={}, blockedId={} ===", blockerId, blockedId);
        
        if (blockerId == null || blockedId == null) {
            log.warn("=== 参数为空，返回false ===");
            return false;
        }
        
        // 单向检查：blockerId 是否拉黑了 blockedId
        Blacklist blocked = blacklistMapper.selectByUserIdAndBlockedUserId(blockerId, blockedId);
        boolean result = blocked != null && blocked.getStatus() == 1;
        log.info("=== 黑名单记录: {}, 结果: {} ===", blocked, result);
        return result;
    }
    
    @Override
    public void setFriendMuted(Long friendId, boolean muted) {
        Long userId = UserContext.getCurrentUserId();
        log.info("设置好友免打扰，userId: {}, friendId: {}, muted: {}", userId, friendId, muted);
        
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        
        // 检查是否是好友
        Friend friend = friendMapper.selectActiveFriendship(userId, friendId);
        if (friend == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "对方不是你的好友");
        }
        
        // 更新免打扰状态
        int updated = friendMapper.updateMuted(userId, friendId, muted ? 1 : 0);
        if (updated > 0) {
            log.info("设置好友免打扰成功，userId: {}, friendId: {}, muted: {}", userId, friendId, muted);
        }
    }
    
    @Override
    public boolean isFriendMuted(Long friendId) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        
        Friend friend = friendMapper.selectActiveFriendship(userId, friendId);
        return friend != null && friend.getMuted() != null && friend.getMuted() == 1;
    }
    
    @Override
    public List<Map<String, Object>> getMutedFriendList() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return friendMapper.selectMutedFriendList(userId);
    }
}
