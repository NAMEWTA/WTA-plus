package org.namewta.common.web.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.namewta.common.core.http.CapturedRequestBody;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.annotation.Validated;

/** 普通JSON及可记录正文的入口缓存预算；独立于机器入口及日志前缀。 */
@Data
@Validated
@ConfigurationProperties(prefix = "namewta.web.request-body")
public class RequestBodyProperties {
    @NotNull
    @DataSizeUnit(DataUnit.BYTES)
    private DataSize maxSize = DataSize.ofBytes(CapturedRequestBody.DEFAULT_MAX_BYTES);

    @AssertTrue(message = "namewta.web.request-body.max-size 必须为正数且在JVM数组范围内")
    public boolean isMaxSizeValid() {
        return maxSize != null && maxSize.toBytes() > 0
            && maxSize.toBytes() <= CapturedRequestBody.MAX_CONFIGURED_BYTES;
    }

    public int maxBytes() {
        int value = Math.toIntExact(maxSize.toBytes());
        CapturedRequestBody.requireLimit(value);
        return value;
    }
}
