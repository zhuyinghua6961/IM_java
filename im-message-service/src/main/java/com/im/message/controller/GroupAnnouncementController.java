package com.im.message.controller;

import com.im.common.context.UserContext;
import com.im.common.vo.Result;
import com.im.message.entity.GroupAnnouncement;
import com.im.message.service.GroupAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 群公告 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/message/announcement")
@RequiredArgsConstructor
public class GroupAnnouncementController {
    
    private final GroupAnnouncementService announcementService;
    
    /**
     * 发布群公告
     */
    @PostMapping
    public Result<Long> publish(@RequestBody Map<String, Object> params) {
        Long userId = UserContext.getCurrentUserId();
        Long groupId = Long.parseLong(params.get("groupId").toString());
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        Boolean isTop = params.get("isTop") != null && (Boolean) params.get("isTop");
        
        log.info("发布群公告: userId={}, groupId={}, title={}", userId, groupId, title);
        
        Long announcementId = announcementService.publish(userId, groupId, title, content, isTop);
        return Result.success(announcementId);
    }
    
    /**
     * 更新群公告
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long announcementId, 
                               @RequestBody Map<String, Object> params) {
        Long userId = UserContext.getCurrentUserId();
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        Boolean isTop = params.get("isTop") != null && (Boolean) params.get("isTop");
        
        log.info("更新群公告: userId={}, announcementId={}", userId, announcementId);
        
        announcementService.update(userId, announcementId, title, content, isTop);
        return Result.success();
    }
    
    /**
     * 删除群公告
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long announcementId) {
        Long userId = UserContext.getCurrentUserId();
        
        log.info("删除群公告: userId={}, announcementId={}", userId, announcementId);
        
        announcementService.delete(userId, announcementId);
        return Result.success();
    }
    
    /**
     * 获取群公告列表
     */
    @GetMapping("/list/{groupId}")
    public Result<List<GroupAnnouncement>> getList(@PathVariable("groupId") Long groupId) {
        log.info("获取群公告列表: groupId={}", groupId);
        
        List<GroupAnnouncement> list = announcementService.getList(groupId);
        return Result.success(list);
    }
    
    /**
     * 获取群最新公告
     */
    @GetMapping("/latest/{groupId}")
    public Result<GroupAnnouncement> getLatest(@PathVariable("groupId") Long groupId) {
        log.info("获取群最新公告: groupId={}", groupId);
        
        GroupAnnouncement announcement = announcementService.getLatest(groupId);
        return Result.success(announcement);
    }
    
    /**
     * 获取公告详情
     */
    @GetMapping("/{id}")
    public Result<GroupAnnouncement> getById(@PathVariable("id") Long announcementId) {
        log.info("获取公告详情: announcementId={}", announcementId);
        
        GroupAnnouncement announcement = announcementService.getById(announcementId);
        return Result.success(announcement);
    }
}
