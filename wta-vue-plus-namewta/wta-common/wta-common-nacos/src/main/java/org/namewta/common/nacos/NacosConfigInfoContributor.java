package org.namewta.common.nacos;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.LinkedHashMap;
import java.util.Map;

final class NacosConfigInfoContributor implements InfoContributor {

    private final NacosConfigManager manager;

    NacosConfigInfoContributor(NacosConfigManager manager) {
        this.manager = manager;
    }

    @Override
    public void contribute(Info.Builder builder) {
        NacosConfigState state = manager.state();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("enabled", state.enabled());
        details.put("connected", state.connected());
        details.put("result", state.result());
        if (state.digest() != null) {
            details.put("digest", state.digest());
        }
        if (state.lastSuccessAt() != null) {
            details.put("lastSuccessAt", state.lastSuccessAt().toString());
        }
        details.put("immediateKeyCount", state.immediateKeyCount());
        details.put("restartKeyCount", state.restartKeyCount());
        if (state.errorCode() != null) {
            details.put("errorCode", state.errorCode());
        }
        builder.withDetail("nacosConfig", details);
    }
}
