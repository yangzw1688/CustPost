package com.kaiyun.infrastructure.storage;

import com.kaiyun.common.exception.BusinessException;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.TemporarySignatureRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.springframework.web.multipart.MultipartFile;

/** 私有桶上传与临时签名访问；对象键可持久化，签名链接按请求生成。 */
@Service
public class ObsStorageService {
    private final String bucket;
    private final long expiry;
    private final ObsClient client;

    public ObsStorageService(@Value("${obs.endpoint:}") String endpoint,
            @Value("${obs.access-key:}") String accessKey,
            @Value("${obs.secret-key:}") String secretKey,
            @Value("${obs.bucket:}") String bucket,
            @Value("${obs.url-expiry-seconds:3600}") long expiry) {
        this.bucket = bucket == null ? "" : bucket.trim();
        if (expiry < 60 || expiry > 86400) {
            throw new IllegalArgumentException("OBS 图片链接有效期必须为60至86400秒");
        }
        this.expiry = expiry;
        if (blank(endpoint) || blank(accessKey) || blank(secretKey) || blank(this.bucket)) {
            this.client = null;
        } else {
            // 兼容区域域名形式的配置，统一使用 HTTPS 连接。
            String normalizedEndpoint = endpoint.trim();
            if (!normalizedEndpoint.contains("://")) {
                normalizedEndpoint = "https://" + normalizedEndpoint;
            }
            URI uri = URI.create(normalizedEndpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
                throw new IllegalArgumentException("OBS Endpoint 必须是有效的 HTTPS 区域地址");
            }
            ObsConfiguration config = new ObsConfiguration();
            config.setEndPoint(normalizedEndpoint);
            config.setConnectionTimeout(10000);
            config.setSocketTimeout(30000);
            config.setConnectionRequestTimeout(10000);
            config.setMaxErrorRetry(1);
            this.client = new ObsClient(accessKey.trim(), secretKey.trim(), config);
        }
    }

    /** 延迟检查 OBS 配置，让历史本地附件在未配置 OBS 时仍可查询。 */
    public void validateConfiguration() {
        if (client == null) {
            throw new BusinessException(503, "图片存储服务尚未配置，请联系管理员");
        }
    }

    private ObsClient client() {
        validateConfiguration();
        return client;
    }

    public void upload(String key, MultipartFile file) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(key.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") ? "application/pdf" : file.getContentType());
        metadata.setContentLength(file.getSize());
        try (InputStream input = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(bucket, key, input);
            request.setMetadata(metadata);
            // 附件包含病例信息，访问通过有时效的签名 URL 完成。
            request.setAcl(AccessControlList.REST_CANNED_PRIVATE);
            client().putObject(request);
        }
    }

    public String signedUrl(String key) {
        if (blank(key)) throw new IllegalStateException("OBS 对象标识为空");
        TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.GET, expiry);
        request.setBucketName(bucket);
        request.setObjectKey(key);
        return client().createTemporarySignature(request).getSignedUrl();
    }

    public void delete(String key) {
        client().deleteObject(bucket, key);
    }

    @PreDestroy
    public void close() throws IOException {
        if (client != null) client.close();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
