package org.namewta.third.usecase;

import org.namewta.third.domain.bo.ThirdProviderBo;
import org.namewta.third.domain.vo.ThirdProviderVo;

import java.util.List;

public interface ThirdProviderUseCase {
    List<ThirdProviderVo> list(String keyword);
    ThirdProviderVo get(Long providerId);
    void save(ThirdProviderBo bo);
    void changeStatus(Long providerId, String status);
    void remove(Long providerId);
}
