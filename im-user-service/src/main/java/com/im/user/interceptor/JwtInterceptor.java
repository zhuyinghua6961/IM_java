package com.im.user.interceptor;

import com.im.common.utils.JwtUtil;
import com.im.user.constant.RedisConstant;
import com.im.user.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * JWT拦截器 - 解析Token并设置当前用户信息
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
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
                
                // 4. 验证Redis中是否存在该Token
                String redisKey = RedisConstant.getUserTokenKey(userId);
                String cachedToken = redisTemplate.opsForValue().get(redisKey);
                
                if (cachedToken == null) {
                    log.warn("Token未在Redis中找到或已过期，userId: {}", userId);
                    return true; // 放行，由Controller层处理
                }
                
                if (!token.equals(cachedToken)) {
                    log.warn("Token不匹配，可能已在其他设备登录，userId: {}", userId);
                    return true; // 放行，由Controller层处理
                }
                
                // 5. Token验证通过，刷新过期时间（滑动过期）
                redisTemplate.expire(redisKey, RedisConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
                // log.debug("Token验证通过并续期，userId: {}", userId);  // 频繁请求，已关闭日志
                
                // 6. 将用户ID存入ThreadLocal
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
        // 5. 请求完成后清除ThreadLocal，防止内存泄漏
        UserContext.clear();
        // log.debug("JWT拦截器：清除当前用户信息");  // 频繁请求，已关闭日志
    }
}
