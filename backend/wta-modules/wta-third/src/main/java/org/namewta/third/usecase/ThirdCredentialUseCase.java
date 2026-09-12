package org.namewta.third.usecase;

import org.namewta.third.domain.bo.ThirdCredentialBo;
import org.namewta.third.domain.vo.ThirdCredentialVo;

import java.util.List;

public interface ThirdCredentialUseCase {
    List<ThirdCredentialVo> list(String providerCode, String endpointCode);

    void save(ThirdCredentialBo bo);

    void remove(Long credentialId);
}
