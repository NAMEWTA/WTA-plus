package org.namewta.system.oss.provider;

import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.oss.client.OssClient;
import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.factory.OssFactory;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.system.domain.SysOss;
import org.namewta.system.oss.exception.OssLifecycleError;
import org.namewta.system.oss.exception.OssLifecycleException;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.net.URI;
import java.time.Duration;

/**
 * 基于 common-oss 的默认 Provider 实现。
 */
@Component
public class DefaultOssObjectStore implements OssObjectStore {

    @Override
    public OssPresignedRequest presign(SysOss oss, Duration ttl) {
        return client(oss).presignGet(oss.getFileName(), ttl);
    }

    @Override
    public AccessPolicy accessPolicy(SysOss oss) {
        return client(oss).config().accessControlPolicyConfig().accessPolicy();
    }

    @Override
    public String publicUrl(SysOss oss) {
        var config = client(oss).config();
        String base = config.domain().filter(StringUtils::isNotBlank)
            .map(ignored -> config.getDomainUrl())
            .orElseGet(config::getBucketUrl);
        URI baseUri = URI.create(base);
        if (baseUri.getUserInfo() != null || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null) {
            throw new IllegalStateException("公共 OSS 域名不能包含凭据、查询串或片段");
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + "/" + SdkHttpUtils.urlEncodeIgnoreSlashes(oss.getFileName());
    }

    @Override
    public void delete(SysOss oss) {
        try {
            if (!client(oss).delete(oss.getFileName())) {
                throw new OssLifecycleException(OssLifecycleError.PROVIDER_DELETE_FAILED,
                    "OSS Provider 删除失败: " + oss.getOssId());
            }
        } catch (OssLifecycleException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OssLifecycleException(OssLifecycleError.PROVIDER_DELETE_FAILED,
                "OSS Provider 删除失败: " + oss.getOssId(), e);
        }
    }

    private OssClient client(SysOss oss) {
        return OssFactory.instance(oss.getService());
    }
}
