package com.im.user.service.impl;

import com.im.common.enums.ResultCode;
import com.im.common.exception.BusinessException;
import com.im.user.context.UserContext;
import com.im.user.entity.User;
import com.im.user.entity.UserWhitelist;
import com.im.user.mapper.FriendMapper;
import com.im.user.mapper.UserMapper;
import com.im.user.mapper.WhitelistMapper;
import com.im.user.service.WhitelistService;
import com.im.user.vo.WhitelistVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 白名单服务实现
 */
@Slf4j
@Service
public class WhitelistServiceImpl implements WhitelistService {
    
    @Autowired
    private WhitelistMapper whitelistMapper;
    
    @Autowired
    private FriendMapper friendMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public void addToWhitelist(Long friendId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("添加到白名单，userId: {}, friendId: {}", userId, friendId);
        
        // 1. 验证是否是好友关系
        if (friendMapper.selectByUserIdAndFriendId(userId, friendId) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能将好友添加到白名单");
        }
        
        // 2. 检查是否已在白名单中
        if (whitelistMapper.isInWhitelist(userId, friendId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该好友已在白名单中");
        }
        
        // 3. 添加到白名单
        UserWhitelist whitelist = new UserWhitelist();
        whitelist.setUserId(userId);
        whitelist.setFriendId(friendId);
        whitelistMapper.insert(whitelist);
        
        log.info("添加到白名单成功");
    }
    
    @Override
    public void removeFromWhitelist(Long friendId) {
        Long userId = UserContext.getCurrentUserId();
        log.info("从白名单移除，userId: {}, friendId: {}", userId, friendId);
        
        // 检查是否在白名单中
        if (!whitelistMapper.isInWhitelist(userId, friendId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该好友不在白名单中");
        }
        
        whitelistMapper.delete(userId, friendId);
        log.info("从白名单移除成功");
    }
    
    @Override
    public List<WhitelistVO> getWhitelistFriends() {
        Long userId = UserContext.getCurrentUserId();
        log.info("获取白名单列表，userId: {}", userId);
        
        // 1. 获取白名单好友ID列表
        List<Long> friendIds = whitelistMapper.selectWhitelistFriendIds(userId);
        
        // 2. 查询好友详细信息
        List<WhitelistVO> result = new ArrayList<>();
        for (Long friendId : friendIds) {
            User user = userMapper.selectById(friendId);
            if (user != null) {
                WhitelistVO vo = new WhitelistVO();
                vo.setUserId(user.getId());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
                // TODO: 需要查询添加时间
                result.add(vo);
            }
        }
        
        log.info("获取白名单列表成功，数量: {}", result.size());
        return result;
    }
    
    @Override
    public boolean isInWhitelist(Long userId, Long friendId) {
        return whitelistMapper.isInWhitelist(userId, friendId);
    }
}
