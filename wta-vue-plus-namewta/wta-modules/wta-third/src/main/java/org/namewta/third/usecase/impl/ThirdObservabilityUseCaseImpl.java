package org.namewta.third.usecase.impl;

import lombok.RequiredArgsConstructor;
import org.namewta.third.domain.vo.ThirdInvocationVo;
import org.namewta.third.domain.vo.ThirdStatisticVo;
import org.namewta.third.service.ThirdObservabilityService;
import org.namewta.third.usecase.ThirdObservabilityUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdObservabilityUseCaseImpl implements ThirdObservabilityUseCase {
    private final ThirdObservabilityService service;

    @Override
    public List<ThirdInvocationVo> invocations(String providerCode) {
        return service.invocations(providerCode);
    }

    @Override
    public List<ThirdStatisticVo> statistics(String providerCode) {
        return service.statistics(providerCode);
    }
}
