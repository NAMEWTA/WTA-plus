package org.namewta.third.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ThirdEndpointBo {
    private Long endpointId;
    @NotNull private Long providerId;
    @NotBlank @Size(max = 64) private String providerCode;
    @NotBlank @Size(max = 64) private String endpointCode;
    @NotBlank @Size(max = 128) private String endpointName;
    @NotBlank @Size(max = 10) private String httpMethod;
    @NotBlank @Size(max = 512) private String relativePath;
    @NotBlank private String requestMode = "JSON";
    @NotBlank private String responseMode = "JSON";
    @Size(max = 20000) private String pathSchemaJson;
    @Size(max = 20000) private String querySchemaJson;
    @Size(max = 20000) private String headerSchemaJson;
    @Size(max = 50000) private String bodySchemaJson;
    @Size(max = 20000) private String responseSchemaJson;
    @Size(max = 20000) private String overrideJson;
    private String status = "0";
    private Boolean idempotent = false;
    @Min(0) @Max(100000) private Integer rateLimit = 0;
    @Min(0) @Max(10000) private Integer concurrencyLimit = 0;
    @Min(0) @Max(3) private Integer retryCount = 0;
    @Size(max = 20000) private String sensitiveFieldsJson;
    @Size(max = 128) private String adapterCode;
}
