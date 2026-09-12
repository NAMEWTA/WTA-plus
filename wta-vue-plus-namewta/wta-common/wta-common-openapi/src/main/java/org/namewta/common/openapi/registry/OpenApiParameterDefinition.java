package org.namewta.common.openapi.registry;

/**
 * Spring MVC parameter with a Swagger-resolved schema.
 */
public record OpenApiParameterDefinition(String name, String location, boolean required, String schema) {
}
