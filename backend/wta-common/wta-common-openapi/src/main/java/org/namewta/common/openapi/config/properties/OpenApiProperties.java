package org.namewta.common.openapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * NAMEWTA OpenAPI runtime properties. The feature remains disabled unless explicitly enabled.
 */
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

    private boolean enabled;
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
