package org.namewta.common.nacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 在 Bean 创建前持有当前进程唯一的 Nacos 客户端与覆盖状态。
 */
final class NacosConfigBootstrap {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigBootstrap.class);
    private static Session current;

    private NacosConfigBootstrap() {
    }

    static synchronized Session start(ConfigurableEnvironment environment, NacosConfigSettings settings,
                                      NacosConfigClientFactory factory) {
        if (current != null && current.environment() == environment) {
            return current;
        }
        shutdown();
        NacosConfigManager manager = new NacosConfigManager(environment, settings);
        NacosPropertySourceRegistrar.register(environment, manager.propertySource());
        NacosConfigClient client = null;
        try {
            client = factory.create(settings);
            manager.serverStatus(client::serverStatus);
            NacosConfigClient connectedClient = client;
            try {
                String content = connectedClient.fetchAndListen(
                    update -> manager.apply(update, NacosUpdateOrigin.LISTENER));
                manager.apply(content, NacosUpdateOrigin.STARTUP);
            } catch (Exception ex) {
                manager.fetchFailed();
                connectedClient.listen(update -> manager.apply(update, NacosUpdateOrigin.LISTENER));
                log.warn("Nacos 配置启动读取失败，已使用本地配置并等待服务恢复，profile={} dataId={}",
                    settings.profile(), settings.dataId());
            }
        } catch (Exception ex) {
            manager.fetchFailed();
            closeQuietly(client);
            client = null;
            log.warn("Nacos 配置客户端创建失败，已使用本地配置，profile={} dataId={}",
                settings.profile(), settings.dataId());
        }
        current = new Session(environment, manager, client);
        return current;
    }

    static synchronized Session current(ConfigurableEnvironment environment) {
        return current != null && current.environment() == environment ? current : null;
    }

    static synchronized void shutdown() {
        if (current != null) {
            closeQuietly(current.client());
            current = null;
        }
    }

    private static void closeQuietly(NacosConfigClient client) {
        if (client == null) {
            return;
        }
        try {
            client.close();
        } catch (Exception ex) {
            log.debug("关闭 Nacos 配置客户端失败", ex);
        }
    }

    record Session(ConfigurableEnvironment environment, NacosConfigManager manager, NacosConfigClient client) {
    }
}
