package org.namewta.common.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.client.config.utils.SnapShotSwitch;
import org.springframework.util.StringUtils;

import java.util.Properties;
import java.util.function.Consumer;

final class NacosSdkConfigClient implements NacosConfigClient {

    private final NacosConfigSettings settings;
    private final ConfigService service;
    private AbstractListener listener;

    private NacosSdkConfigClient(NacosConfigSettings settings, ConfigService service) {
        this.settings = settings;
        this.service = service;
    }

    static NacosConfigClient create(NacosConfigSettings settings) throws Exception {
        disableSnapshotFallback();
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, settings.serverAddr());
        properties.put(PropertyKeyConst.NAMESPACE, settings.namespace());
        if (StringUtils.hasText(settings.username())) {
            properties.put(PropertyKeyConst.USERNAME, settings.username());
            properties.put(PropertyKeyConst.PASSWORD, settings.password() == null ? "" : settings.password());
        }
        return new NacosSdkConfigClient(settings, NacosFactory.createConfigService(properties));
    }

    static void disableSnapshotFallback() {
        SnapShotSwitch.setIsSnapShot(false);
    }

    @Override
    public String fetchAndListen(Consumer<String> consumer) throws Exception {
        listener = listener(consumer);
        return service.getConfigAndSignListener(settings.dataId(), settings.group(), settings.timeoutMs(), listener);
    }

    @Override
    public void listen(Consumer<String> consumer) throws Exception {
        listener = listener(consumer);
        service.addListener(settings.dataId(), settings.group(), listener);
    }

    @Override
    public String serverStatus() {
        return service.getServerStatus();
    }

    @Override
    public void close() throws Exception {
        if (listener != null) {
            service.removeListener(settings.dataId(), settings.group(), listener);
        }
        service.shutDown();
    }

    private static AbstractListener listener(Consumer<String> consumer) {
        return new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                consumer.accept(configInfo);
            }
        };
    }
}
