package com.im.message.service;

import com.im.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 黑名单检查服务（调用用户服务API）
 */
@Slf4j
@Service
public class BlacklistCheckService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${user-service.url:http://localhost:8081}")
    private String userServiceUrl;
    
    /**
     * 检查用户是否被拉黑（单向）
     * @param blockerId 拉黑方用户ID（接收者）
     * @param blockedId 被拉黑方用户ID（发送者）
     * @return 是否被拉黑
     */
    public boolean isBlockedBy(Long blockerId, Long blockedId) {
        try {
            String url = userServiceUrl + "/api/friend/internal/check-blocked?blockerId=" + blockerId + "&blockedId=" + blockedId;
            log.info("=== 调用用户服务检查黑名单: {} ===", url);
            
            ResponseEntity<Result<Boolean>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result<Boolean>>() {}
            );
            
            log.info("=== 用户服务响应: {} ===", response.getBody());
            
            if (response.getBody() != null && response.getBody().getData() != null) {
                boolean blocked = response.getBody().getData();
                log.info("=== 黑名单检查结果: blockerId={}, blockedId={}, blocked={} ===", blockerId, blockedId, blocked);
                return blocked;
            }
            log.warn("=== 用户服务响应为空 ===");
            return false;
        } catch (Exception e) {
            log.error("=== 调用用户服务检查黑名单失败: blockerId={}, blockedId={} ===", blockerId, blockedId, e);
            // 调用失败时默认不拦截
            return false;
        }
    }
}
