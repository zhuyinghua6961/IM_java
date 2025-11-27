package com.im.message.interceptor;

import com.im.common.context.UserContext;
import com.im.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器 - 验证 Token 并提取用户信息
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求头获取Token
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            // 2. 提取Token（去掉"Bearer "前缀）
            String token = authHeader.substring(BEARER_PREFIX.length());
            
            try {
                // 3. 解析Token获取用户ID
                Long userId = JwtUtil.getUserIdFromToken(token);
                
                log.debug("Token验证通过，userId: {}", userId);
                
                // 4. 将用户ID存入ThreadLocal
                UserContext.setCurrentUserId(userId);
                
            } catch (Exception e) {
                log.warn("JWT解析失败: {}", e.getMessage());
                // Token无效，但仍然放行，让Controller层处理
            }
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求完成后清除ThreadLocal，防止内存泄漏
        UserContext.clear();
        log.debug("JWT拦截器：清除当前用户信息");
    }
}
