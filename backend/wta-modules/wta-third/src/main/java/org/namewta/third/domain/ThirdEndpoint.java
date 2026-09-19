package org.namewta.third.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("third_endpoint")
public class ThirdEndpoint extends BaseEntity {
    @TableId private Long endpointId;
    private Long providerId;
    private String providerCode;
    private String endpointCode;
    private String endpointName;
    private String httpMethod;
    private String relativePath;
    private String requestMode;
    private String responseMode;
    private String pathSchemaJson;
    private String querySchemaJson;
    private String headerSchemaJson;
    private String bodySchemaJson;
    private String responseSchemaJson;
    private String overrideJson;
    private String status;
    private Boolean idempotent;
    private Integer rateLimit;
    private Integer concurrencyLimit;
    private Integer retryCount;
    private String sensitiveFieldsJson;
    private String adapterCode;
    @Version private Integer version;
    private String delFlag;
}
