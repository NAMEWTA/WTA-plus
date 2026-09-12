package org.namewta.system.oss.provider;

import org.namewta.common.oss.enums.AccessPolicy;
import org.namewta.common.oss.model.OssPresignedRequest;
import org.namewta.system.domain.SysOss;

import java.time.Duration;

/**
 * 生命周期层使用的对象存储接缝。
 */
public interface OssObjectStore {

    OssPresignedRequest presign(SysOss oss, Duration ttl);

    AccessPolicy accessPolicy(SysOss oss);

    String publicUrl(SysOss oss);

    void delete(SysOss oss);
}
