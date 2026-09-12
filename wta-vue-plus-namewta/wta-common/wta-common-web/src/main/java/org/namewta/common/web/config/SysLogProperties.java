package org.namewta.common.web.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.annotation.Validated;

/**
 * 系统 HTTP 日志配置。默认值由 Java 拥有，部署环境仍可通过外部属性覆盖。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "sys.log")
public class SysLogProperties {

    /**
     * 是否启用完整 HTTP 请求与响应日志。
     */
    private boolean enabled = true;

    /**
     * 请求和响应各自允许写入日志的正文上限。
     */
    @NotNull
    @DataSizeUnit(DataUnit.MEGABYTES)
    private DataSize maxBodySize = DataSize.ofMegabytes(1);

    /**
     * 正文日志使用 JVM byte[] 保存前缀，因此拒绝非正数和不可分配的上限。
     *
     * @return 配置是否有效
     */
    @AssertTrue(message = "sys.log.max-body-size 必须大于 0 且不能超过 2GiB")
    public boolean isMaxBodySizeValid() {
        if (maxBodySize == null) {
            return false;
        }
        long bytes = maxBodySize.toBytes();
        return bytes > 0 && bytes <= Integer.MAX_VALUE;
    }

    /**
     * 返回已通过配置校验的正文上限字节数。
     *
     * @return 正文上限
     */
    public int maxBodyBytes() {
        return Math.toIntExact(maxBodySize.toBytes());
    }
}
