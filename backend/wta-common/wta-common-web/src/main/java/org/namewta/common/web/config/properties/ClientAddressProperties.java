package org.namewta.common.web.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 来源地址信任配置；发布时必须按实际代理拓扑提供CIDR。 */
@Data
@ConfigurationProperties(prefix = "namewta.web.client-address")
public class ClientAddressProperties {
    /** 默认只信任socket peer，不隐式信任环回地址或私网。 */
    private List<String> trustedProxies = List.of();
}
