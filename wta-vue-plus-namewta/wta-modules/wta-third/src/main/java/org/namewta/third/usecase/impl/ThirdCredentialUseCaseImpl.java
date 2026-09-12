package org.namewta.third.usecase.impl;

import lombok.RequiredArgsConstructor;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.third.domain.bo.ThirdCredentialBo;
import org.namewta.third.domain.vo.ThirdCredentialVo;
import org.namewta.third.service.ThirdCredentialService;
import org.namewta.third.usecase.ThirdCredentialUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdCredentialUseCaseImpl implements ThirdCredentialUseCase {
    private final ThirdCredentialService service;

    public List<ThirdCredentialVo> list(String providerCode, String endpointCode) { return service.list(providerCode, endpointCode); }
    @DSTransactional public void save(ThirdCredentialBo bo) { service.save(bo); }
    @DSTransactional public void remove(Long credentialId) { service.remove(credentialId); }
}
