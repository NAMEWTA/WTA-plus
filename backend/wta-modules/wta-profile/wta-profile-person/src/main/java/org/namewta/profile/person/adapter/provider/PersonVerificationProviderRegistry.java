package org.namewta.profile.person.adapter.provider;
import org.namewta.profile.person.config.PersonVerificationProviderProperties;
import org.namewta.profile.person.port.provider.PersonVerificationProvider;
import org.namewta.profile.person.domain.exception.PersonVerificationException;
import org.namewta.profile.person.domain.verification.PersonVerificationFailureCategory;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/** 个人认证提供方注册表，按编码解析可用的认证适配器。 */
@Component
public class PersonVerificationProviderRegistry implements org.namewta.profile.person.port.provider.PersonVerificationProviderRegistryPort {
    private static final String PROVIDER_CODE_PATTERN = "[a-z0-9][a-z0-9_-]{0,63}";
    private final Map<String, PersonVerificationProvider> providers;
    private final Set<String> enabledProviders;
    /** 创建个人认证提供方注册表。 */
    public PersonVerificationProviderRegistry(List<PersonVerificationProvider> providers,
                                              PersonVerificationProviderProperties properties) {
        this.providers = index(providers);
        this.enabledProviders = properties.getEnabledProviders().stream()
            .map(this::validateCode)
            .collect(Collectors.toUnmodifiableSet());
    }
    /** 校验认证提供方已启用。 */
    public PersonVerificationProvider requireEnabled(String providerCode) {
        String code = validateCode(providerCode);
        PersonVerificationProvider provider = providers.get(code);
        if (provider == null) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.UNKNOWN_PROVIDER, "Unknown person verification provider");
        }
        if (!enabledProviders.contains(code)) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.DISABLED_PROVIDER, "Person verification provider is disabled");
        }
        return provider;
    }
    /** 构建认证提供方索引。 */
    private Map<String, PersonVerificationProvider> index(List<PersonVerificationProvider> candidates) {
        Map<String, PersonVerificationProvider> indexed = new HashMap<>();
        for (PersonVerificationProvider provider : candidates) {
            String code = validateCode(provider.providerCode());
            if (indexed.putIfAbsent(code, provider) != null) {
                throw new PersonVerificationException(
                    PersonVerificationFailureCategory.DUPLICATE_PROVIDER,
                    "Duplicate person verification provider code");
            }
        }
        return Map.copyOf(indexed);
    }
    /** 校验code。 */
    private String validateCode(String providerCode) {
        if (providerCode == null || !providerCode.matches(PROVIDER_CODE_PATTERN)) {
            throw new PersonVerificationException(
                PersonVerificationFailureCategory.INVALID_PROVIDER_CODE,
                "Invalid person verification provider code");
        }
        return providerCode;
    }
}
