package org.namewta.common.openapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.namewta.common.core.http.CapturedRequestBody;

import java.time.Duration;

/**
 * NAMEWTA OpenAPI runtime properties. The feature remains disabled unless explicitly enabled.
 */
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

    private boolean enabled;
    private DataSize maxBodySize = DataSize.ofBytes(CapturedRequestBody.DEFAULT_MAX_BYTES);
    private Duration clockSkew = Duration.ofSeconds(60);
    private Duration nonceTtl = Duration.ofSeconds(60);
    private int appRateLimitPerMinute = 1000;
    private int interfaceRateLimitPerMinute = 100;
    private Duration machineSessionTtl = Duration.ofHours(8);
    private String kek;
    private String kekVersion;

    public boolean isEnabled() {
        return enabled;
    }

    /** 机器签名正文的独立上限，不受普通JSON或日志前缀配置覆盖。 */
    public DataSize getMaxBodySize() { return maxBodySize; }
    public void setMaxBodySize(DataSize maxBodySize) { this.maxBodySize = maxBodySize; }

    public int maxBodyBytes() {
        if (maxBodySize == null || maxBodySize.toBytes() <= 0
            || maxBodySize.toBytes() > CapturedRequestBody.MAX_CONFIGURED_BYTES) {
            throw new IllegalArgumentException("openapi.max-body-size 必须为正数且在JVM数组范围内");
        }
        return Math.toIntExact(maxBodySize.toBytes());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    public Duration getNonceTtl() {
        return nonceTtl;
    }

    public void setNonceTtl(Duration nonceTtl) {
        this.nonceTtl = nonceTtl;
    }

    public int getAppRateLimitPerMinute() {
        return appRateLimitPerMinute;
    }

    public void setAppRateLimitPerMinute(int appRateLimitPerMinute) {
        this.appRateLimitPerMinute = appRateLimitPerMinute;
    }

    public int getInterfaceRateLimitPerMinute() {
        return interfaceRateLimitPerMinute;
    }

    public void setInterfaceRateLimitPerMinute(int interfaceRateLimitPerMinute) {
        this.interfaceRateLimitPerMinute = interfaceRateLimitPerMinute;
    }

    public Duration getMachineSessionTtl() {
        return machineSessionTtl;
    }

    public void setMachineSessionTtl(Duration machineSessionTtl) {
        this.machineSessionTtl = machineSessionTtl;
    }

    public String getKek() {
        return kek;
    }

    public void setKek(String kek) {
        this.kek = kek;
    }

    public String getKekVersion() {
        return kekVersion;
    }

    public void setKekVersion(String kekVersion) {
        this.kekVersion = kekVersion;
    }

}
