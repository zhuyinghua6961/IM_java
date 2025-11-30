package com.im.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.im.common.vo.Result;
import com.im.user.dto.GroupDTO;
import com.im.user.service.GroupService;
import com.im.user.vo.GroupInvitationVO;
import com.im.user.vo.GroupMemberVO;
import com.im.user.vo.GroupVO;

/**
 * 群组控制器
 */
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 创建群组
     */
    @PostMapping("/create")
    public Result<GroupVO> createGroup(@RequestBody GroupDTO groupDTO) {
        GroupVO groupVO = groupService.createGroup(groupDTO);
        return Result.success(groupVO);
    }

    /**
     * 获取群组列表
     */
    @GetMapping("/list")
    public Result<List<GroupVO>> getGroupList() {
        List<GroupVO> groupList = groupService.getGroupList();
        return Result.success(groupList);
    }

    /**
     * 获取群组详情
     */
    @GetMapping("/detail/{groupId}")
    public Result<GroupVO> getGroupById(@PathVariable("groupId") Long groupId) {
        GroupVO groupVO = groupService.getGroupById(groupId);
        return Result.success(groupVO);
    }
    
    /**
     * 修改群组信息（群主专用）
     */
    @PostMapping("/{groupId}/update")
    public Result<GroupVO> updateGroup(@PathVariable("groupId") Long groupId,
                                      @RequestBody GroupDTO groupDTO) {
        GroupVO groupVO = groupService.updateGroup(groupId, groupDTO);
        return Result.success(groupVO);
    }

    /**
     * 邀请用户加入群组
     */
    @PostMapping("/{groupId}/invite")
    public Result<GroupVO> inviteGroup(
            @PathVariable("groupId") Long groupId,
            @RequestBody List<Long> userIds) {
        GroupVO groupVO = groupService.inviteGroup(groupId, userIds);
        return Result.success(groupVO);
    }

    /**
     * 获取我的群组邀请列表
     */
    @GetMapping("/invitations")
    public Result<List<GroupInvitationVO>> getMyInvitations() {
        List<GroupInvitationVO> invitations = groupService.getMyInvitations();
        return Result.success(invitations);
    }
    
    /**
     * 处理群组邀请（同意或拒绝）
     */
    @PostMapping("/invitation/handle")
    public Result<Void> handleInvitation(@RequestBody Map<String, Object> request) {
        Long invitationId = Long.valueOf(request.get("invitationId").toString());
        Boolean accept = (Boolean) request.get("accept");
        
        groupService.handleInvitation(invitationId, accept);
        return Result.success();
    }
    
    /**
     * 退出群组
     */
    @PostMapping("/{groupId}/quit")
    public Result<Void> quitGroup(@PathVariable("groupId") Long groupId) {
        groupService.quitGroup(groupId);
        return Result.success();
    }
    
    /**
     * 获取群组成员列表
     */
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMemberVO>> getGroupMembers(@PathVariable("groupId") Long groupId) {
        List<GroupMemberVO> members = groupService.getGroupMembers(groupId);
        return Result.success(members);
    }
    
    /**
     * 设置/取消管理员
     */
    @PostMapping("/{groupId}/admin")
    public Result<Void> setGroupAdmin(@PathVariable("groupId") Long groupId, 
                                     @RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Boolean isAdmin = (Boolean) request.get("isAdmin");
        
        groupService.setGroupAdmin(groupId, userId, isAdmin);
        return Result.success();
    }
    
    /**
     * 管理员审批邀请
     */
    @PostMapping("/invitation/{invitationId}/approve")
    public Result<Void> approveInvitation(@PathVariable("invitationId") Long invitationId,
                                         @RequestBody Map<String, Object> request) {
        Boolean approve = (Boolean) request.get("approve");
        
        groupService.approveInvitation(invitationId, approve);
        return Result.success();
    }
    
    /**
     * 解散群组（群主专用）
     */
    @PostMapping("/{groupId}/dissolve")
    public Result<Void> dissolveGroup(@PathVariable("groupId") Long groupId) {
        groupService.dissolveGroup(groupId);
        return Result.success();
    }
    
    /**
     * 移除群成员（管理员/群主专用）
     */
    @PostMapping("/{groupId}/remove")
    public Result<Void> removeMember(@PathVariable("groupId") Long groupId,
                                     @RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        groupService.removeMember(groupId, userId);
        return Result.success();
    }
}
