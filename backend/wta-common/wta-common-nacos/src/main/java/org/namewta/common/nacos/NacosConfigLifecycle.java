package org.namewta.common.nacos;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

final class NacosConfigLifecycle implements SmartInitializingSingleton, DisposableBean {

    private final NacosConfigManager manager;
    private final ObjectProvider<NacosConfigParticipant<?>> participants;

    NacosConfigLifecycle(NacosConfigManager manager, ObjectProvider<NacosConfigParticipant<?>> participants) {
        this.manager = manager;
        this.participants = participants;
    }

    @Override
    public void afterSingletonsInstantiated() {
        manager.registerParticipants(participants.orderedStream().toList());
    }

    @Override
    public void destroy() {
        NacosConfigBootstrap.shutdown();
    }
}
