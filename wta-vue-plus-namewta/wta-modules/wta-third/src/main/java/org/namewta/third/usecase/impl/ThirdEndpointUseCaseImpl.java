package org.namewta.third.usecase.impl;

import lombok.RequiredArgsConstructor;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.third.domain.bo.ThirdEndpointBo;
import org.namewta.third.domain.vo.ThirdEndpointVo;
import org.namewta.third.service.ThirdEndpointService;
import org.namewta.third.usecase.ThirdEndpointUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdEndpointUseCaseImpl implements ThirdEndpointUseCase {
    private final ThirdEndpointService service;

    public List<ThirdEndpointVo> list(Long providerId, String keyword) { return service.list(providerId, keyword); }
    public ThirdEndpointVo get(Long endpointId) { return service.get(endpointId); }
    @DSTransactional public void save(ThirdEndpointBo bo) { service.save(bo); }
    @DSTransactional public void changeStatus(Long endpointId, String status) { service.changeStatus(endpointId, status); }
    @DSTransactional public void remove(Long endpointId) { service.remove(endpointId); }
}
