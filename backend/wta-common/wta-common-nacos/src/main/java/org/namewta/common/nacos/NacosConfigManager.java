package org.namewta.common.nacos;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 原子维护远程覆盖和已经完成类型校验的运行期配置快照。
 */
public final class NacosConfigManager implements NacosConfigAccessor {

    private final ConfigurableEnvironment environment;
    private final NacosConfigSettings settings;
    private final AtomicReference<Snapshot> snapshot;
    private volatile List<NacosConfigParticipant<?>> participants = List.of();
    private volatile Supplier<String> serverStatus = () -> "DOWN";

    NacosConfigManager(ConfigurableEnvironment environment, NacosConfigSettings settings) {
        this.environment = environment;
        this.settings = settings;
        NacosConfigState initial = new NacosConfigState(settings.enabled(), false, null, "LOCAL_BASELINE",
            null, 0, 0, null);
        this.snapshot = new AtomicReference<>(new Snapshot(Map.of(), Map.of(), initial));
    }

    NacosConfigPropertySource propertySource() {
        return new NacosConfigPropertySource(this);
    }

    Map<String, Object> overlay() {
        return snapshot.get().overlay();
    }

    public NacosConfigState state() {
        NacosConfigState state = snapshot.get().state();
        boolean connected;
        try {
            connected = "UP".equalsIgnoreCase(serverStatus.get());
        } catch (RuntimeException ignored) {
            connected = false;
        }
        return state.withConnection(connected);
    }

    @Override
    public <T> Optional<T> configuration(String participantId, Class<T> type) {
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(type, "type");
        Object value = snapshot.get().prepared().get(participantId);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    synchronized void registerParticipants(Collection<NacosConfigParticipant<?>> candidates) {
        Map<String, NacosConfigParticipant<?>> byId = new LinkedHashMap<>();
        for (NacosConfigParticipant<?> participant : candidates) {
            if (participant == null || participant.id() == null || participant.id().isBlank()
                || participant.prefixes() == null || participant.exactKeys() == null
                || (participant.prefixes().isEmpty() && participant.exactKeys().isEmpty())) {
                throw new IllegalStateException("Invalid Nacos config participant");
            }
            if (byId.putIfAbsent(participant.id(), participant) != null) {
                throw new IllegalStateException("Duplicate Nacos config participant: " + participant.id());
            }
        }
        participants = List.copyOf(byId.values());
        Snapshot current = snapshot.get();
        try {
            Map<String, Object> prepared = prepare(current.overlay());
            Counts counts = classify(current.overlay());
            NacosConfigState state = current.state();
            snapshot.set(new Snapshot(current.overlay(), prepared,
                new NacosConfigState(state.enabled(), state.connected(), state.digest(), state.result(),
                    state.lastSuccessAt(), counts.immediate(), counts.restart(), state.errorCode())));
        } catch (RuntimeException ex) {
            restoreLocalBaseline(current, "PARTICIPANT_REJECTED");
        }
    }

    synchronized boolean apply(String yaml, NacosUpdateOrigin origin) {
        Snapshot current = snapshot.get();
        try {
            Map<String, Object> candidate = NacosYamlOverlay.parse(yaml);
            NacosCandidateValidator.validate(candidate, environment);
            Map<String, Object> prepared = prepare(candidate);
            Counts counts = classify(candidate);
            String digest = candidate.isEmpty() ? null : digest(yaml == null ? "" : yaml);
            NacosConfigState nextState = new NacosConfigState(true, true, digest, "APPLIED", Instant.now(),
                counts.immediate(), counts.restart(), null);
            snapshot.set(new Snapshot(candidate, prepared, nextState));
            return true;
        } catch (NacosConfigValidationException ex) {
            reject(current, ex.code());
        } catch (RuntimeException ex) {
            reject(current, "PARTICIPANT_REJECTED");
        }
        return false;
    }

    void serverStatus(Supplier<String> status) {
        this.serverStatus = status == null ? () -> "DOWN" : status;
    }

    synchronized void fetchFailed() {
        Snapshot current = snapshot.get();
        NacosConfigState state = current.state();
        String result = state.digest() == null ? "LOCAL_BASELINE" : state.result();
        snapshot.set(new Snapshot(current.overlay(), current.prepared(),
            new NacosConfigState(state.enabled(), false, state.digest(), result, state.lastSuccessAt(),
                state.immediateKeyCount(), state.restartKeyCount(), "FETCH_FAILED")));
    }

    private Map<String, Object> prepare(Map<String, Object> candidate) {
        if (participants.isEmpty()) {
            return Map.of();
        }
        Binder binder = candidateBinder(candidate);
        Map<String, Object> prepared = new HashMap<>();
        for (NacosConfigParticipant<?> participant : participants) {
            Object value = participant.prepare(binder);
            if (value == null) {
                throw new NacosConfigValidationException("PARTICIPANT_REJECTED");
            }
            prepared.put(participant.id(), value);
        }
        return Map.copyOf(prepared);
    }

    private Binder candidateBinder(Map<String, Object> candidate) {
        MutablePropertySources sources = new MutablePropertySources();
        boolean inserted = false;
        for (org.springframework.core.env.PropertySource<?> source : environment.getPropertySources()) {
            if (NacosConfigConstants.PROPERTY_SOURCE_NAME.equals(source.getName())) {
                sources.addLast(new MapPropertySource(NacosConfigConstants.PROPERTY_SOURCE_NAME, candidate));
                inserted = true;
            } else {
                sources.addLast(source);
            }
        }
        if (!inserted) {
            sources.addFirst(new MapPropertySource(NacosConfigConstants.PROPERTY_SOURCE_NAME, candidate));
        }
        return new Binder(ConfigurationPropertySources.from(sources), new PropertySourcesPlaceholdersResolver(sources));
    }

    private Counts classify(Map<String, Object> candidate) {
        Set<String> prefixes = new LinkedHashSet<>();
        Set<String> exactKeys = new LinkedHashSet<>();
        for (NacosConfigParticipant<?> participant : participants) {
            prefixes.addAll(participant.prefixes());
            exactKeys.addAll(participant.exactKeys());
        }
        int immediate = 0;
        for (String key : candidate.keySet()) {
            if (exactKeys.contains(key) || prefixes.stream().anyMatch(key::startsWith)) {
                immediate++;
            }
        }
        return new Counts(immediate, candidate.size() - immediate);
    }

    private void reject(Snapshot current, String code) {
        NacosConfigState state = current.state();
        snapshot.set(new Snapshot(current.overlay(), current.prepared(),
            new NacosConfigState(state.enabled(), state.connected(), state.digest(), "REJECTED",
                state.lastSuccessAt(), state.immediateKeyCount(), state.restartKeyCount(), code)));
    }

    private void restoreLocalBaseline(Snapshot current, String code) {
        Map<String, Object> prepared;
        try {
            prepared = prepare(Map.of());
        } catch (RuntimeException ignored) {
            prepared = Map.of();
        }
        NacosConfigState state = current.state();
        snapshot.set(new Snapshot(Map.of(), prepared,
            new NacosConfigState(state.enabled(), state.connected(), null, "REJECTED",
                null, 0, 0, code)));
    }

    private static String digest(String content) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                value.append(String.format("%02x", current));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record Snapshot(Map<String, Object> overlay, Map<String, Object> prepared, NacosConfigState state) {
    }

    private record Counts(int immediate, int restart) {
    }
}
