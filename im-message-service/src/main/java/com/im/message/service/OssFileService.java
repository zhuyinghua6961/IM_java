package com.im.message.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * OSS 文件服务，用于上传语音等文件
 */
public interface OssFileService {

    /**
     * 上传语音文件到 OSS，并返回文件的访问地址及元信息
     *
     * @param file 语音文件
     * @return 包含 url、size 的元信息
     * @throws IOException IO 异常
     */
    Map<String, Object> uploadAudio(MultipartFile file) throws IOException;
}
