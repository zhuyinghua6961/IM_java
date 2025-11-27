package com.im.user.controller;

import com.im.common.vo.Result;
import com.im.user.service.WhitelistService;
import com.im.user.vo.WhitelistVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 白名单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/whitelist")
public class WhitelistController {
    
    @Autowired
    private WhitelistService whitelistService;
    
    /**
     * 获取白名单列表
     */
    @GetMapping("/list")
    public Result<List<WhitelistVO>> getWhitelistFriends() {
        List<WhitelistVO> list = whitelistService.getWhitelistFriends();
        return Result.success(list);
    }
    
    /**
     * 添加好友到白名单
     */
    @PostMapping("/add/{friendId}")
    public Result<Void> addToWhitelist(@PathVariable("friendId") Long friendId) {
        whitelistService.addToWhitelist(friendId);
        return Result.success();
    }
    
    /**
     * 从白名单移除好友
     */
    @DeleteMapping("/remove/{friendId}")
    public Result<Void> removeFromWhitelist(@PathVariable("friendId") Long friendId) {
        whitelistService.removeFromWhitelist(friendId);
        return Result.success();
    }
}
