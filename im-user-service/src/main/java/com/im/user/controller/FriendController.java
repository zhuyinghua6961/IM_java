package com.im.user.controller;

import java.util.List;

import com.im.common.vo.Result;
import com.im.user.dto.FriendRemarkDTO;
import com.im.user.dto.FriendRequestDTO;
import com.im.user.entity.FriendRequest;
import com.im.user.service.FriendService;
import com.im.user.vo.BlacklistVO;
import com.im.user.vo.FriendVO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 好友管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;
    
    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendVO>> getFriendList() {
        List<FriendVO> friendList = friendService.getFriendList();
        return Result.success(friendList);
    }

    /**
     * 发送好友申请
     */
    @PostMapping("/request")
    public Result<FriendRequest> addFriendRequest(@RequestBody FriendRequestDTO dto) {
        FriendRequest friendRequest = friendService.addFriendRequest(dto);
        return Result.success(friendRequest);
    }

    /**
     * 获取好友申请列表
     */
    @GetMapping("/request/list")
    public Result<List<FriendRequest>> getFriendRequestList() {
        List<FriendRequest> requests = friendService.getFriendRequestList();
        return Result.success(requests);
    }
    
    /**
     * 处理好友申请
     */
    @PostMapping("/request/handle")
    public Result<FriendRequest> handleFriendRequest(@RequestBody FriendRequestDTO dto) {
        FriendRequest friendRequest = friendService.handleFriendRequest(dto);
        return Result.success(friendRequest);
    }


    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@PathVariable("friendId") Long friendId) {
        friendService.deleteFriend(friendId);
        return Result.success();
    }

    /**
     * 更新好友备注
     */
    @PostMapping("/remark")
    public Result<Void> updateRemark(@RequestBody FriendRemarkDTO dto) {
        friendService.updateRemark(dto);
        return Result.success();
    }
    
    /**
     * 拉黑用户
     */
    @PostMapping("/block/{targetUserId}")
    public Result<Void> blockUser(@PathVariable("targetUserId") Long targetUserId) {
        friendService.blockUser(targetUserId);
        return Result.success();
    }
    
    /**
     * 取消拉黑
     */
    @PostMapping("/unblock/{targetUserId}")
    public Result<Void> unblockUser(@PathVariable("targetUserId") Long targetUserId) {
        friendService.unblockUser(targetUserId);
        return Result.success();
    }
    
    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    public Result<List<BlacklistVO>> getBlacklist() {
        List<BlacklistVO> blacklist = friendService.getBlacklist();
        return Result.success(blacklist);
    }
    
    /**
     * 检查是否被拉黑
     */
    @GetMapping("/blocked/{targetUserId}")
    public Result<Boolean> isBlocked(@PathVariable("targetUserId") Long targetUserId) {
        boolean blocked = friendService.isBlocked(targetUserId);
        return Result.success(blocked);
    }
    
    /**
     * 内部API：检查用户A是否拉黑了用户B（供消息服务调用）
     */
    @GetMapping("/internal/check-blocked")
    public Result<Boolean> checkBlocked(
            @org.springframework.web.bind.annotation.RequestParam("blockerId") Long blockerId,
            @org.springframework.web.bind.annotation.RequestParam("blockedId") Long blockedId) {
        boolean blocked = friendService.checkBlockedInternal(blockerId, blockedId);
        return Result.success(blocked);
    }
}
