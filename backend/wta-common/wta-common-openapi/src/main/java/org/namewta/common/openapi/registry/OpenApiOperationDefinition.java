package org.namewta.common.openapi.registry;

import java.util.List;

/**
 * Immutable public operation derived from one real MVC method/path mapping.
 */
public record OpenApiOperationDefinition(
    String interfaceId,
    String summary,
    String method,
    String path,
    OpenApiAccessRule accessRule,
    List<OpenApiParameterDefinition> parameters,
    String requestSchema,
    String responseSchema
) {
    public OpenApiOperationDefinition {
        parameters = List.copyOf(parameters);
    }
}
