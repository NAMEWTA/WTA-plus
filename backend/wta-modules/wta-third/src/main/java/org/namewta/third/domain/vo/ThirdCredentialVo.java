package org.namewta.third.domain.vo;

import java.time.LocalDateTime;

public record ThirdCredentialVo(
    Long credentialId,
    String providerCode,
    String endpointCode,
    String scopeType,
    String credentialType,
    String kekVersion,
    LocalDateTime expiresAt,
    Integer version,
    String enabled
) {
}
