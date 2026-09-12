package org.namewta.third.usecase;

import org.namewta.third.domain.vo.ThirdInvocationVo;
import org.namewta.third.domain.vo.ThirdStatisticVo;

import java.util.List;

/** Read-only management contract for invocation and statistic views. */
public interface ThirdObservabilityUseCase {
    List<ThirdInvocationVo> invocations(String providerCode);

    List<ThirdStatisticVo> statistics(String providerCode);
}
