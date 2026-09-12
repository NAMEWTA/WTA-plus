package org.namewta.third.usecase;

import org.namewta.third.domain.bo.ThirdEndpointBo;
import org.namewta.third.domain.vo.ThirdEndpointVo;

import java.util.List;

public interface ThirdEndpointUseCase {
    List<ThirdEndpointVo> list(Long providerId, String keyword);
    ThirdEndpointVo get(Long endpointId);
    void save(ThirdEndpointBo bo);
    void changeStatus(Long endpointId, String status);
    void remove(Long endpointId);
}
