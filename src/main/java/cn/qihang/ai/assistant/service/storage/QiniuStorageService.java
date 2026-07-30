package cn.qihang.ai.assistant.service.storage;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class QiniuStorageService {

    private static final Logger log = LoggerFactory.getLogger(QiniuStorageService.class);

    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final String domain;
    private final Auth auth;
    private final UploadManager uploadManager;

    public QiniuStorageService(
            @Value("${qiniu.access-key:}") String accessKey,
            @Value("${qiniu.secret-key:}") String secretKey,
            @Value("${qiniu.bucket:}") String bucket,
            @Value("${qiniu.domain:}") String domain) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.domain = domain;
        if (isConfigured()) {
            this.auth = Auth.create(accessKey, secretKey);
            Configuration cfg = Configuration.create(Region.autoRegion());
            cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
            this.uploadManager = new UploadManager(cfg);
        } else {
            this.auth = null;
            this.uploadManager = null;
        }
    }

    public boolean isConfigured() {
        return accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()
                && bucket != null && !bucket.isEmpty();
    }

    /**
     * Upload bytes to Qiniu. Returns the key on success, empty string on failure.
     */
    public String upload(byte[] data, String key) {
        if (!isConfigured()) {
            log.warn("Qiniu not configured, skipping upload");
            return "";
        }
        try {
            String upToken = auth.uploadToken(bucket, key, 3600, new StringMap().put("insertOnly", 0));
            Response response = uploadManager.put(data, key, upToken);
            DefaultPutRet ret = response.jsonToObject(DefaultPutRet.class);
            log.info("Qiniu upload success: key={}, hash={}", ret.key, ret.hash);
            return ret.key;
        } catch (QiniuException e) {
            log.error("Qiniu upload failed: key={}, error={}", key, e.getMessage());
            if (e.response != null) {
                try { log.error("Qiniu response: {}", e.response.bodyString()); } catch (Exception ignored) {}
            }
            return "";
        }
    }

    /**
     * Upload a local file path to Qiniu.
     */
    public String uploadFile(String localFilePath, String key) {
        if (!isConfigured()) {
            log.warn("Qiniu not configured, skipping upload");
            return "";
        }
        try {
            String upToken = auth.uploadToken(bucket, key, 3600, new StringMap().put("insertOnly", 0));
            Response response = uploadManager.put(localFilePath, key, upToken);
            DefaultPutRet ret = response.jsonToObject(DefaultPutRet.class);
            log.info("Qiniu upload success: key={}, hash={}", ret.key, ret.hash);
            return ret.key;
        } catch (QiniuException e) {
            log.error("Qiniu upload failed: key={}, error={}", key, e.getMessage());
            if (e.response != null) {
                try { log.error("Qiniu response: {}", e.response.bodyString()); } catch (Exception ignored) {}
            }
            return "";
        }
    }

    /**
     * Generate a download URL with token for private bucket.
     * For public buckets, just returns domain/key directly.
     */
    public String downloadUrl(String key) {
        if (!isConfigured() || key == null || key.isEmpty()) return "";
        String baseUrl = domain + "/" + key;
        if (auth != null) {
            // Private bucket: signed URL, valid for 1 hour
            return auth.privateDownloadUrl(baseUrl, 3600);
        }
        return baseUrl;
    }
}
