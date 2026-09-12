package org.namewta.third.service;

import lombok.RequiredArgsConstructor;
import org.namewta.third.dao.ThirdInvocationDao;
import org.namewta.third.dao.ThirdStatisticDao;
import org.namewta.third.domain.vo.ThirdInvocationVo;
import org.namewta.third.domain.vo.ThirdStatisticVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdObservabilityService {
    private final ThirdInvocationDao invocationDao;
    private final ThirdStatisticDao statisticDao;

    public List<ThirdInvocationVo> invocations(String providerCode) {
        providerCode = normalize(providerCode);
        return invocationDao.findRecent(providerCode, LocalDateTime.now().minusDays(7)).stream()
            .map(x -> new ThirdInvocationVo(x.getInvocationId(), x.getRequestId(), x.getProviderCode(), x.getEndpointCode(), x.getAttemptCount(), x.getLogicalStatus(), x.getFailureCategory(), x.getHttpStatus(), x.getDurationMs(), x.getSanitizedRequestJson(), x.getSanitizedResponseJson(), x.getCreateTime())).toList();
    }

    public List<ThirdStatisticVo> statistics(String providerCode) {
        providerCode = normalize(providerCode);
        return statisticDao.findRecent(providerCode).stream().map(x -> new ThirdStatisticVo(x.getProviderCode(), x.getEndpointCode(), x.getStatDate(), x.getAttemptCount(), x.getSuccessCount(), x.getFailureCount(), x.getTimeoutCount(), x.getRejectedCount(), x.getQuotaValue())).toList();
    }

    private String normalize(String providerCode) {
        return providerCode == null || providerCode.isBlank() ? null : providerCode.strip();
    }
}
