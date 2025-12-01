package com.im.message.controller;

import com.im.common.vo.Result;
import com.im.message.service.OssFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 文件上传控制器（语音等 IM 媒体文件）
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private OssFileService ossFileService;

    /**
     * 上传语音文件到 OSS
     */
    @PostMapping("/upload/audio")
    public Result<Map<String, Object>> uploadAudio(@RequestPart("file") MultipartFile file) {
        try {
            Map<String, Object> data = ossFileService.uploadAudio(file);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            log.warn("上传语音文件参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("上传语音文件到OSS失败", e);
            return Result.error("上传语音文件失败，请稍后重试");
        }
    }
}
