package org.namewta.system.domain.read;

import lombok.Data;

/** 数据库权威状态投影；只有接入、身份源与业务客户端均有效才会返回。 */
@Data
public class ExternalAuthRuntimeStamp {
    /** 接入主键。 */
    private Long id;
    /** 接入版本。 */
    private Long version;
    /** 身份源主键。 */
    private Long providerId;
    /** 身份源版本。 */
    private Long providerVersion;
}
