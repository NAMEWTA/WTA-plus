package org.namewta.sso.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;

import lombok.RequiredArgsConstructor;

import org.namewta.sso.api.SsoRuntimeSettings;
import org.namewta.sso.service.SsoConfigurationService;
import org.springframework.stereotype.Service;

/** 中央配置用例；跨模块管理聚合只调用API适配器。 */
@Service
@RequiredArgsConstructor
public class SsoConfigurationUseCase {
    private final SsoConfigurationService service;

    /** 返回进程实际生效配置。 */
    public SsoRuntimeSettings current() {
        return service.current();
    }

    /** 返回数据库目标配置。 */
    public SsoRuntimeSettings saved() {
        return service.saved();
    }

    /** 在主库事务中保存配置。 */
    @DSTransactional
    public void save(SsoRuntimeSettings settings) {
        service.save(settings);
    }
}
