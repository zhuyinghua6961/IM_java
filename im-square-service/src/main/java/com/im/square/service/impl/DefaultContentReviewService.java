package com.im.square.service.impl;

import com.im.square.service.ContentReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 默认内容审核实现：当前仅打日志，不拦截内容。
 * 后续可以在此处集成第三方内容审核平台。
 */
@Slf4j
@Service
public class DefaultContentReviewService implements ContentReviewService {

    @Value("${content-review.enabled:false}")
    private boolean enabled;

    @Override
    public void reviewText(String content) {
        if (!enabled) {
            // 未开启审核，直接放行
            return;
        }
        // 预留：这里可以调用第三方内容审核API
        log.debug("Content review enabled, but no external service is configured. Content length={}",
                content == null ? 0 : content.length());
    }
}
