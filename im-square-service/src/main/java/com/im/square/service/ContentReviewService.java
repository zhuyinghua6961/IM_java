package com.im.square.service;

/**
 * 内容审核服务接口
 *
 * 目前默认实现为"全部通过"，后续可接入实际的第三方内容审核平台。
 */
public interface ContentReviewService {

    /**
     * 审核文本内容，如不通过应抛出业务异常。
     *
     * @param content 文本内容
     */
    void reviewText(String content);
}
