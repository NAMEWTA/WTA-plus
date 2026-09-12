package org.namewta.system.oss.migration;

import lombok.RequiredArgsConstructor;
import org.namewta.system.oss.readiness.OssRequiredConfigContributor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OssMigrationRequiredConfigContributor implements OssRequiredConfigContributor {

    private final OssMigrationStore store;

    @Override
    public Map<String, Set<String>> requiredConfigs() {
        return store.activeConfigKeys().stream().collect(Collectors.toUnmodifiableMap(
            key -> key, ignored -> Set.of("OSS_MIGRATION")));
    }
}
