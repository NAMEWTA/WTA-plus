package org.namewta.third.spi;

import org.namewta.common.core.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("local")
class ThirdProviderAdapterRegistryTest {

    @Test
    void resolvesProviderOwnedEndpointHook() {
        ThirdProviderAdapter adapter = adapter("qichacha", "qichacha-signature", "company");

        ThirdProviderAdapter resolved = new ThirdProviderAdapterRegistry(List.of(adapter))
            .find("qichacha", "company", "qichacha-signature");

        assertEquals(adapter, resolved);
    }

    @Test
    void rejectsUnownedOrUnsupportedEndpointHook() {
        ThirdProviderAdapterRegistry registry = new ThirdProviderAdapterRegistry(
            List.of(adapter("qichacha", "qichacha-signature", "company")));

        assertThrows(ServiceException.class, () -> registry.find("qichacha", "company", "other"));
        assertThrows(ServiceException.class, () -> registry.find("qichacha", "person", "qichacha-signature"));
    }

    @Test
    void rejectsDuplicateProviderBeansAtStartup() {
        ThirdProviderAdapter first = adapter("qichacha", "one", "company");
        ThirdProviderAdapter second = adapter("qichacha", "two", "company");

        assertThrows(ServiceException.class, () -> new ThirdProviderAdapterRegistry(List.of(first, second)));
    }

    private static ThirdProviderAdapter adapter(String providerCode, String adapterCode, String endpointCode) {
        return new ThirdProviderAdapter() {
            @Override
            public String providerCode() {
                return providerCode;
            }

            @Override
            public String adapterCode() {
                return adapterCode;
            }

            @Override
            public boolean supportsEndpoint(String requestedEndpointCode) {
                return endpointCode.equals(requestedEndpointCode);
            }
        };
    }
}
