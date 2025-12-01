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

    /**
     * 对外访问域名，如：https://your-bucket.oss-cn-hangzhou.aliyuncs.com
     */
    @Value("${im.oss.domain}")
    private String domain;

    @Override
    public Map<String, Object> uploadAudio(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();

        // 按日期分目录存储：audio/yyyy/MM/dd/{uuid}.{ext}
        LocalDate today = LocalDate.now();
        String datePath = today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + String.format("%02d", today.getDayOfMonth());

        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String objectName = "audio/" + datePath + "/" + UUID.randomUUID() + ext;

        log.info("上传文件到OSS: bucket={}, objectName={}, originalName={}, size={}",
                bucketName, objectName, originalFilename, size);

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, objectName, inputStream);
        }

        String url = domain.endsWith("/") ? (domain + objectName) : (domain + "/" + objectName);

        Map<String, Object> result = new HashMap<>(4);
        result.put("url", url);
        result.put("size", size);
        result.put("fileName", originalFilename);
        return result;
    }
}
