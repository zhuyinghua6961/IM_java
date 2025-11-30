package com.im.user.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket消息推送工具类
 * 用户服务通过HTTP调用消息服务的推送接口
 */
@Slf4j
@Component
public class WebSocketUtil {
    
    @Value("${message.service.url:http://localhost:8082}")
    private String messageServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 推送好友申请通知
     * @param toUserId 接收方用户ID
     * @param fromUserId 发送方用户ID
     * @param fromUserName 发送方用户名
     * @param message 申请消息
     */
    public void pushFriendRequest(Long toUserId, Long fromUserId, String fromUserName, String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "FRIEND_REQUEST");
            notification.put("toUserId", toUserId);
            notification.put("fromUserId", fromUserId);
            notification.put("fromUserName", fromUserName);
            notification.put("message", message);
            
            sendNotification(notification);
            log.info("好友申请通知推送成功: {} -> {}", fromUserId, toUserId);
        } catch (Exception e) {
            log.error("好友申请通知推送失败", e);
        }
    }
    
    /**
     * 推送好友申请处理结果通知
     * @param toUserId 接收方用户ID（申请方）
     * @param fromUserId 处理方用户ID
     * @param fromUserName 处理方用户名
     * @param accepted 是否同意
     */
    public void pushFriendRequestResult(Long toUserId, Long fromUserId, String fromUserName, boolean accepted) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "FRIEND_REQUEST_RESULT");
            notification.put("toUserId", toUserId);
            notification.put("fromUserId", fromUserId);
            notification.put("fromUserName", fromUserName);
            notification.put("accepted", accepted);
            notification.put("message", accepted ? 
                fromUserName + "同意了你的好友申请" : 
                fromUserName + "拒绝了你的好友申请");
            
            sendNotification(notification);
            log.info("好友申请结果通知推送成功: {} -> {}, accepted: {}", fromUserId, toUserId, accepted);
        } catch (Exception e) {
            log.error("好友申请结果通知推送失败", e);
        }
    }
    
    /**
     * 推送群组邀请通知
     * @param toUserId 被邀请人ID
     * @param inviterId 邀请人ID
     * @param inviterName 邀请人昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     * @param invitationId 邀请记录ID
     */
    public void pushGroupInvitation(Long toUserId, Long inviterId, String inviterName, 
                                    Long groupId, String groupName, Long invitationId) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_INVITATION");
            notification.put("toUserId", toUserId);
            notification.put("inviterId", inviterId);
            notification.put("inviterName", inviterName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("invitationId", invitationId);
            notification.put("message", inviterName + " 邀请你加入群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组邀请通知推送成功: inviter={}, invitee={}, group={}", inviterId, toUserId, groupId);
        } catch (Exception e) {
            log.error("群组邀请通知推送失败", e);
        }
    }
    
    /**
     * 推送群组邀请处理结果通知
     * @param toUserId 接收方ID（邀请人）
     * @param inviteeId 被邀请人ID
     * @param inviteeName 被邀请人昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     * @param accepted 是否接受
     */
    public void pushGroupInvitationResult(Long toUserId, Long inviteeId, String inviteeName,
                                         Long groupId, String groupName, boolean accepted) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_INVITATION_RESULT");
            notification.put("toUserId", toUserId);
            notification.put("inviteeId", inviteeId);
            notification.put("inviteeName", inviteeName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("accepted", accepted);
            notification.put("message", accepted ?
                inviteeName + " 已加入群组 " + groupName :
                inviteeName + " 拒绝加入群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组邀请结果通知推送成功: invitee={}, inviter={}, accepted={}", inviteeId, toUserId, accepted);
        } catch (Exception e) {
            log.error("群组邀请结果通知推送失败", e);
        }
    }
    
    /**
     * 推送群组退出通知（只通知群主和管理员）
     * @param toUserId 接收方ID（群主或管理员）
     * @param quitUserId 退出用户ID
     * @param quitUserName 退出用户昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     */
    public void pushGroupQuitNotification(Long toUserId, Long quitUserId, String quitUserName,
                                         Long groupId, String groupName) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_MEMBER_QUIT");
            notification.put("toUserId", toUserId);
            notification.put("quitUserId", quitUserId);
            notification.put("quitUserName", quitUserName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("message", quitUserName + " 退出了群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组退出通知推送成功: quitUser={}, receiver={}, group={}", quitUserId, toUserId, groupId);
        } catch (Exception e) {
            log.error("群组退出通知推送失败", e);
        }
    }
    
    /**
     * 推送群组成员被移除通知
     * @param toUserId 被移除的用户ID
     * @param operatorId 操作者ID
     * @param removedUserName 被移除用户昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     */
    public void pushGroupRemoveNotification(Long toUserId, Long operatorId, String removedUserName,
                                           Long groupId, String groupName) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_MEMBER_REMOVED");
            notification.put("toUserId", toUserId);
            notification.put("operatorId", operatorId);
            notification.put("removedUserName", removedUserName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("message", "您已被移出群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组成员移除通知推送成功: user={}, operator={}, group={}", toUserId, operatorId, groupId);
        } catch (Exception e) {
            log.error("群组成员移除通知推送失败", e);
        }
    }
    
    /**
     * 推送群组管理员变更通知
     * @param toUserId 接收方ID（被设置的用户）
     * @param operatorId 操作者ID（群主）
     * @param targetUserName 目标用户昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     * @param isAdmin 是否设为管理员
     */
    public void pushGroupAdminNotification(Long toUserId, Long operatorId, String targetUserName,
                                          Long groupId, String groupName, boolean isAdmin) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_ADMIN_CHANGE");
            notification.put("toUserId", toUserId);
            notification.put("operatorId", operatorId);
            notification.put("targetUserName", targetUserName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("isAdmin", isAdmin);
            notification.put("message", isAdmin ? 
                "您已被设为群组 " + groupName + " 的管理员" :
                "您已被取消群组 " + groupName + " 的管理员身份");
            
            sendNotification(notification);
            log.info("群组管理员变更通知推送成功: target={}, operator={}, isAdmin={}, group={}", 
                    toUserId, operatorId, isAdmin, groupId);
        } catch (Exception e) {
            log.error("群组管理员变更通知推送失败", e);
        }
    }
    
    /**
     * 推送群组直接加入通知（白名单用户）
     * @param toUserId 接收方ID（被邀请加入的用户）
     * @param inviterId 邀请人ID
     * @param inviterName 邀请人昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     */
    public void pushGroupJoinNotification(Long toUserId, Long inviterId, String inviterName,
                                         Long groupId, String groupName) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_DIRECT_JOIN");
            notification.put("toUserId", toUserId);
            notification.put("inviterId", inviterId);
            notification.put("inviterName", inviterName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("message", inviterName + " 邀请您加入了群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组直接加入通知推送成功: user={}, inviter={}, group={}", toUserId, inviterId, groupId);
        } catch (Exception e) {
            log.error("群组直接加入通知推送失败", e);
        }
    }
    
    /**
     * 推送群组邀请审批通知（给管理员）
     * @param toUserId 接收方ID（管理员）
     * @param inviterId 邀请人ID
     * @param inviterName 邀请人昵称
     * @param inviteeId 被邀请人ID
     * @param groupId 群组ID
     * @param groupName 群组名称
     * @param invitationId 邀请ID
     */
    public void pushGroupInviteApprovalNotification(Long toUserId, Long inviterId, String inviterName,
                                                   Long inviteeId, Long groupId, String groupName, Long invitationId) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_INVITE_APPROVAL");
            notification.put("toUserId", toUserId);
            notification.put("inviterId", inviterId);
            notification.put("inviterName", inviterName);
            notification.put("inviteeId", inviteeId);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("invitationId", invitationId);
            notification.put("message", inviterName + " 申请邀请新成员加入群组 " + groupName + "，请审批");
            
            sendNotification(notification);
            log.info("群组邀请审批通知推送成功: admin={}, inviter={}, invitee={}, group={}", 
                    toUserId, inviterId, inviteeId, groupId);
        } catch (Exception e) {
            log.error("群组邀请审批通知推送失败", e);
        }
    }
    
    /**
     * 推送群组邀请审批结果通知（给邀请人）
     * @param toUserId 接收方ID（邀请人）
     * @param adminId 审批管理员ID
     * @param inviteeName 被邀请人昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     * @param approved 是否批准
     */
    public void pushGroupInviteApprovalResult(Long toUserId, Long adminId, String inviteeName,
                                             Long groupId, String groupName, boolean approved) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_INVITE_APPROVAL_RESULT");
            notification.put("toUserId", toUserId);
            notification.put("adminId", adminId);
            notification.put("inviteeName", inviteeName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("approved", approved);
            notification.put("message", approved ? 
                "管理员已批准您邀请 " + inviteeName + " 加入群组 " + groupName :
                "管理员已拒绝您邀请 " + inviteeName + " 加入群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组邀请审批结果通知推送成功: inviter={}, admin={}, approved={}, group={}", 
                    toUserId, adminId, approved, groupId);
        } catch (Exception e) {
            log.error("群组邀请审批结果通知推送失败", e);
        }
    }
    
    /**
     * 推送群组解散通知（通知所有成员）
     * @param toUserId 接收方ID（群成员）
     * @param ownerId 群主ID
     * @param ownerName 群主昵称
     * @param groupId 群组ID
     * @param groupName 群组名称
     */
    public void pushGroupDissolveNotification(Long toUserId, Long ownerId, String ownerName,
                                             Long groupId, String groupName) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "GROUP_DISSOLVED");
            notification.put("toUserId", toUserId);
            notification.put("ownerId", ownerId);
            notification.put("ownerName", ownerName);
            notification.put("groupId", groupId);
            notification.put("groupName", groupName);
            notification.put("message", "群主 " + ownerName + " 解散了群组 " + groupName);
            
            sendNotification(notification);
            log.info("群组解散通知推送成功: member={}, owner={}, group={}", toUserId, ownerId, groupId);
        } catch (Exception e) {
            log.error("群组解散通知推送失败", e);
        }
    }
    
    /**
     * 发送通知到消息服务
     */
    private void sendNotification(Map<String, Object> notification) {
        String url = messageServiceUrl + "/api/notification/push";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(notification, headers);
        
        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            log.warn("消息服务可能未启动或不可用: {}", e.getMessage());
        }
    }
}
