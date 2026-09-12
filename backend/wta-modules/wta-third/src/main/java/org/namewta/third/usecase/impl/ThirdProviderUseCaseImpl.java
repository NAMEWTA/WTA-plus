package org.namewta.third.usecase.impl;

import lombok.RequiredArgsConstructor;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.namewta.third.domain.bo.ThirdProviderBo;
import org.namewta.third.domain.vo.ThirdProviderVo;
import org.namewta.third.service.ThirdProviderService;
import org.namewta.third.usecase.ThirdProviderUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThirdProviderUseCaseImpl implements ThirdProviderUseCase {
    private final ThirdProviderService service;

    public List<ThirdProviderVo> list(String keyword) { return service.list(keyword); }
    public ThirdProviderVo get(Long providerId) { return service.get(providerId); }
    @DSTransactional public void save(ThirdProviderBo bo) { service.save(bo); }
    @DSTransactional public void changeStatus(Long providerId, String status) { service.changeStatus(providerId, status); }
    @DSTransactional public void remove(Long providerId) { service.remove(providerId); }
}
