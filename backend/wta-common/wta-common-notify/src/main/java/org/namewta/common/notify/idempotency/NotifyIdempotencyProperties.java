package org.namewta.common.notify.idempotency;

import org.namewta.common.nacos.NacosConfigAccessor;
import org.namewta.common.nacos.NacosConfigParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

import java.time.Duration;
import java.util.Set;

/**
 * 通知业务幂等窗口配置。
 */
@ConfigurationProperties(prefix = "notify.idempotency")
public class NotifyIdempotencyProperties implements NacosConfigParticipant<NotifyIdempotencyProperties.Snapshot> {

    private Duration defaultWindow = Duration.ofMinutes(5);
    private Duration minWindow = Duration.ofSeconds(30);
    private Duration maxWindow = Duration.ofHours(24);
    private volatile NacosConfigAccessor nacosConfigAccessor;

    @Override
    public String id() {
        return "notify-idempotency";
    }

    @Override
    public Set<String> prefixes() {
        return Set.of("notify.idempotency.");
    }

    @Override
    public Snapshot prepare(Binder binder) {
        return validate(binder.bind("notify.idempotency", Bindable.of(Snapshot.class))
            .orElseGet(this::localSnapshot));
    }

    public Snapshot currentSnapshot() {
        NacosConfigAccessor accessor = nacosConfigAccessor;
        return accessor == null
            ? localSnapshot()
            : accessor.configuration(id(), Snapshot.class).orElseGet(this::localSnapshot);
    }

    public Snapshot localSnapshot() {
        return new Snapshot(defaultWindow, minWindow, maxWindow);
    }

    @Autowired(required = false)
    public void setNacosConfigAccessor(NacosConfigAccessor nacosConfigAccessor) {
        this.nacosConfigAccessor = nacosConfigAccessor;
    }

    public Duration getDefaultWindow() {
        return currentSnapshot().defaultWindow();
    }

    public void setDefaultWindow(Duration defaultWindow) {
        this.defaultWindow = defaultWindow;
    }

    public Duration getMinWindow() {
        return currentSnapshot().minWindow();
    }

    public void setMinWindow(Duration minWindow) {
        this.minWindow = minWindow;
    }

    public Duration getMaxWindow() {
        return currentSnapshot().maxWindow();
    }

    public void setMaxWindow(Duration maxWindow) {
        this.maxWindow = maxWindow;
    }

    private Snapshot validate(Snapshot snapshot) {
        if (!positive(snapshot.defaultWindow()) || !positive(snapshot.minWindow())
            || !positive(snapshot.maxWindow()) || snapshot.maxWindow().compareTo(snapshot.minWindow()) < 0
            || snapshot.defaultWindow().compareTo(snapshot.minWindow()) < 0
            || snapshot.defaultWindow().compareTo(snapshot.maxWindow()) > 0) {
            throw new IllegalArgumentException("Invalid notify idempotency runtime configuration");
        }
        return snapshot;
    }

    private boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }

    /**
     * 单次通知幂等操作使用的不可变配置。
     */
    public record Snapshot(Duration defaultWindow, Duration minWindow, Duration maxWindow) {
    }
}
