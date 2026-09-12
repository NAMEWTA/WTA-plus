package org.namewta.third.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThirdCredentialBo {
    private Long credentialId;
    @NotBlank private String providerCode;
    private String endpointCode;
    @NotBlank private String credentialType;
    @NotBlank private String secretJson;
    @NotNull private Boolean enabled;
    private LocalDateTime expiresAt;
}
