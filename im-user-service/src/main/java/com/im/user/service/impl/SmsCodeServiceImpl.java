package com.im.user.service.impl;

import com.im.user.constant.RedisConstant;
import com.im.user.service.SmsCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现
 * 注意：这是模拟实现，验证码会打印在控制台，不会真正发送短信
 */
@Slf4j
@Service
public class SmsCodeServiceImpl implements SmsCodeService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRE_MINUTES = 5;
    
    @Override
    public boolean sendCode(String phone) {
        try {
            // 1. 校验手机号格式
            if (!isValidPhone(phone)) {
                log.warn("手机号格式不正确: {}", phone);
                return false;
            }
            
            // 2. 检查是否频繁发送（60秒内只能发送一次）
            String redisKey = RedisConstant.getSmsCodeKey(phone);
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > CODE_EXPIRE_MINUTES * 60 - 60) {
                log.warn("验证码发送过于频繁，请稍后再试: {}", phone);
                return false;
            }
            
            // 3. 生成6位随机验证码
            String code = generateCode();
            
            // 4. 存储到Redis，5分钟过期
            redisTemplate.opsForValue().set(
                redisKey, 
                code, 
                CODE_EXPIRE_MINUTES, 
                TimeUnit.MINUTES
            );
            
            // 5. 模拟发送短信 - 打印到控制台
            log.info("========================================");
            log.info("【短信验证码】");
            log.info("手机号: {}", phone);
            log.info("验证码: {}", code);
            log.info("有效期: {} 分钟", CODE_EXPIRE_MINUTES);
            log.info("========================================");
            
            return true;
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return false;
        }
    }
    
    @Override
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        
        String redisKey = RedisConstant.getSmsCodeKey(phone);
        String savedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (savedCode == null) {
            log.warn("验证码已过期或不存在: {}", phone);
            return false;
        }
        
        boolean isValid = savedCode.equals(code);
        if (isValid) {
            log.info("验证码验证成功: {}", phone);
        } else {
            log.warn("验证码错误: phone={}, input={}, expected={}", phone, code, savedCode);
        }
        
        return isValid;
    }
    
    @Override
    public void clearCode(String phone) {
        String redisKey = RedisConstant.getSmsCodeKey(phone);
        redisTemplate.delete(redisKey);
        log.info("验证码已清除: {}", phone);
    }
    
    /**
     * 生成随机验证码
     */
    private String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * 校验手机号格式
     */
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        // 简单校验：1开头的11位数字
        return phone.matches("^1[3-9]\\d{9}$");
    }
}
