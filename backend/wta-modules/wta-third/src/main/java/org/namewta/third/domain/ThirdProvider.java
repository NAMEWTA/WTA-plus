package org.namewta.third.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("third_provider")
public class ThirdProvider extends BaseEntity {
    @TableId private Long providerId;
    private String providerCode;
    private String providerName;
    private String baseUrl;
    private String status;
    private Integer timeoutConnectMs;
    private Integer timeoutReadMs;
    private Integer rateLimit;
    private Integer concurrencyLimit;
    private String sharedHeadersJson;
    private String remark;
    private Integer version;
    private String delFlag;
}
