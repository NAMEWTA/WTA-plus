package org.namewta.third.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("third_invocation")
public class ThirdInvocation {
    @TableId private Long invocationId;
    private String requestId;
    private String providerCode;
    private String endpointCode;
    private Integer attemptCount;
    private String logicalStatus;
    private String failureCategory;
    private Integer httpStatus;
    private String providerErrorCode;
    private Long durationMs;
    private String sanitizedRequestJson;
    private String sanitizedResponseJson;
    private LocalDateTime createTime;
}
