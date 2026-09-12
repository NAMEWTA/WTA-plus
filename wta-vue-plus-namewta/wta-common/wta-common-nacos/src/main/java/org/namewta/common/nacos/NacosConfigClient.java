package org.namewta.common.nacos;

import java.util.function.Consumer;

interface NacosConfigClient extends AutoCloseable {

    String fetchAndListen(Consumer<String> listener) throws Exception;

    void listen(Consumer<String> listener) throws Exception;

    String serverStatus();

    @Override
    void close() throws Exception;
}
