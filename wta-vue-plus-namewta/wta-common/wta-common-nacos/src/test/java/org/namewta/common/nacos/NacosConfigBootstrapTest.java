package org.namewta.common.nacos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class NacosConfigBootstrapTest {

    @AfterEach
    void tearDown() {
        NacosConfigBootstrap.shutdown();
    }

    @Test
    void failedInitialServerReadSubscribesWithoutReadingAClientSnapshot() {
        MockEnvironment environment = new MockEnvironment().withProperty("nacos.config.enabled", "true");
        FakeClient client = new FakeClient();

        NacosConfigBootstrap.start(environment, NacosConfigSettings.from(environment), ignored -> client);

        assertThat(client.fetchAndListenCalls).isEqualTo(1);
        assertThat(client.listenCalls).isEqualTo(1);
        assertThat(environment.getPropertySources().contains(NacosConfigConstants.PROPERTY_SOURCE_NAME)).isTrue();
        assertThat(NacosConfigBootstrap.current(environment).manager().state().result()).isEqualTo("LOCAL_BASELINE");
    }

    private static final class FakeClient implements NacosConfigClient {
        private int fetchAndListenCalls;
        private int listenCalls;

        @Override
        public String fetchAndListen(Consumer<String> listener) {
            fetchAndListenCalls++;
            throw new IllegalStateException("server unavailable");
        }

        @Override
        public void listen(Consumer<String> listener) {
            listenCalls++;
        }

        @Override
        public String serverStatus() {
            return "DOWN";
        }

        @Override
        public void close() {
        }
    }
}
