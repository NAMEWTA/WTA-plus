package org.namewta.common.social.config.properties;

import lombok.Data;

import java.util.Map;

/**
 * 按数据库快照构造的 JustAuth 参数；不再绑定 YAML 或环境变量。
 */
@Data
public class SocialProperties {

    /**
     * 授权类型
     */
    private Map<String, SocialLoginConfigProperties> type;

}
