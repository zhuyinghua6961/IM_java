package com.im.message.service.impl;

import com.aliyun.oss.OSS;
import com.im.message.service.OssFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于阿里云 OSS 的文件服务实现
 */
@Slf4j
@Service
public class OssFileServiceImpl implements OssFileService {

    @Autowired
    private OSS ossClient;

    @Value("${im.oss.bucket-name}")
    private String bucketName;
    
    @Value("${im.oss.endpoint}")
    private String endpoint;

    @Override
    public Map<String, Object> uploadAudio(MultipartFile file) throws IOException {
        return uploadFileToOss(file, "audio");
    }

    @Override
    public Map<String, Object> uploadImage(MultipartFile file) throws IOException {
        return uploadFileToOss(file, "image");
    }

    @Override
    public Map<String, Object> uploadVideo(MultipartFile file) throws IOException {
        return uploadFileToOss(file, "video");
    }

    @Override
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {
        return uploadFileToOss(file, "file");
    }

    private Map<String, Object> uploadFileToOss(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();

        // 按日期分目录存储：{folder}/yyyy/MM/dd/{uuid}.{ext}
        LocalDate today = LocalDate.now();
        String datePath = today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + String.format("%02d", today.getDayOfMonth());

        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String objectName = folder + "/" + datePath + "/" + UUID.randomUUID() + ext;

        log.info("上传文件到OSS: bucket={}, objectName={}, originalName={}, size={}",
                bucketName, objectName, originalFilename, size);

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, objectName, inputStream);
        }

        // 这里使用 "bucketName.endpoint/objectName" 的路径形式，如：https://your-bucket.oss-cn-hangzhou.aliyuncs.com/object
        String normalizedEndpoint = endpoint;
        if (normalizedEndpoint.endsWith("/")) {
            normalizedEndpoint = normalizedEndpoint.substring(0, normalizedEndpoint.length() - 1);
        }
        String scheme = "https";
        String hostPart = normalizedEndpoint;
        int schemeIndex = normalizedEndpoint.indexOf("://");
        if (schemeIndex > 0) {
            scheme = normalizedEndpoint.substring(0, schemeIndex);
            hostPart = normalizedEndpoint.substring(schemeIndex + 3);
        }
        String url = scheme + "://" + bucketName + "." + hostPart + "/" + objectName;

        Map<String, Object> result = new HashMap<>(4);
        result.put("url", url);
        result.put("size", size);
        result.put("fileName", originalFilename);
        return result;
    }
}
