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

    /** 有界删除；异常不能证明供应商未执行，调用者必须保留持久预约。 */
    void delete(SysOss oss, Duration timeout);

    /** 仅在对象缺失且 Bucket 可达时返回 false；不可确认时抛异常。 */
    boolean exists(SysOss oss, Duration timeout);
}
