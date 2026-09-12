package org.namewta.common.openapi.catalog;

import org.namewta.common.openapi.registry.OpenApiAccessRule;
import org.namewta.common.openapi.registry.OpenApiParameterDefinition;

import java.util.List;

/**
 * Credential-independent catalog view of one callable operation.
 */
public record OpenApiCatalogItem(
    String interfaceId,
    String summary,
    String method,
    String path,
    OpenApiAccessRule accessRule,
    List<OpenApiParameterDefinition> parameters,
    String requestSchema,
    String responseSchema,
    String curlExample,
    String javaExample
) {
    public OpenApiCatalogItem {
        parameters = List.copyOf(parameters);
    }
}
