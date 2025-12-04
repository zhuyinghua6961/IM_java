package com.im.square.service;

import com.im.common.vo.Result;
import com.im.square.dto.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 远程用户服务客户端，用于获取用户昵称和头像
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserService {

    private final RestTemplate restTemplate;

    @Value("${user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    /**
     * 根据用户ID获取用户基础信息
     */
    public UserProfileDTO getUserProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            String url = userServiceUrl + "/api/user/info/" + userId;
            ResponseEntity<Result<UserProfileDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<UserProfileDTO>>() {}
            );

            Result<UserProfileDTO> body = response.getBody();
            if (body != null && body.getData() != null) {
                return body.getData();
            }
        } catch (Exception e) {
            log.warn("调用用户服务获取用户信息失败, userId={}", userId, e);
        }
        return null;
    }
}
