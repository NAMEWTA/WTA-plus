package org.namewta.third.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ThirdProviderBo {
    private Long providerId;
    @NotBlank @Size(max = 64) private String providerCode;
    @NotBlank @Size(max = 128) private String providerName;
    @NotBlank @Size(max = 512) private String baseUrl;
    private String status = "0";
    @Min(100) @Max(120000) private Integer timeoutConnectMs = 3000;
    @Min(100) @Max(300000) private Integer timeoutReadMs = 10000;
    @Min(0) @Max(100000) private Integer rateLimit = 0;
    @Min(0) @Max(10000) private Integer concurrencyLimit = 0;
    @Size(max = 10000) private String sharedHeadersJson;
    @Size(max = 500) private String remark;
}
