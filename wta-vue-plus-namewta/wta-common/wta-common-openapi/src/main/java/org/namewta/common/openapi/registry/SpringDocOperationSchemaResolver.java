package org.namewta.common.openapi.registry;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.Json;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves schemas with the same Swagger ModelConverters used by SpringDoc.
 */
public final class SpringDocOperationSchemaResolver {

    public ResolvedOperation resolve(HandlerMethod handlerMethod) {
        List<OpenApiParameterDefinition> parameters = new ArrayList<>();
        String requestSchema = null;
        Parameter[] javaParameters = handlerMethod.getMethod().getParameters();
        for (int index = 0; index < javaParameters.length; index++) {
            Parameter parameter = javaParameters[index];
            if (ServletRequest.class.isAssignableFrom(parameter.getType())
                || ServletResponse.class.isAssignableFrom(parameter.getType())) {
                continue;
            }
            MethodParameter methodParameter = handlerMethod.getMethodParameters()[index];
            RequestBody body = parameter.getAnnotation(RequestBody.class);
            if (body != null) {
                requestSchema = schema(methodParameter.getGenericParameterType());
                continue;
            }
            ParameterLocation location = location(parameter);
            parameters.add(new OpenApiParameterDefinition(location.name(), location.location(),
                location.required(), schema(methodParameter.getGenericParameterType())));
        }
        return new ResolvedOperation(parameters, requestSchema,
            schema(handlerMethod.getMethod().getGenericReturnType()));
    }

    private static ParameterLocation location(Parameter parameter) {
        PathVariable path = parameter.getAnnotation(PathVariable.class);
        if (path != null) {
            return new ParameterLocation(name(path.name(), path.value(), parameter), "path", true);
        }
        RequestHeader header = parameter.getAnnotation(RequestHeader.class);
        if (header != null) {
            return new ParameterLocation(name(header.name(), header.value(), parameter), "header", header.required());
        }
        RequestParam query = parameter.getAnnotation(RequestParam.class);
        if (query != null) {
            return new ParameterLocation(name(query.name(), query.value(), parameter), "query", query.required());
        }
        return new ParameterLocation(parameter.getName(), "query", false);
    }

    private static String name(String name, String value, Parameter parameter) {
        if (!name.isBlank()) {
            return name;
        }
        return value.isBlank() ? parameter.getName() : value;
    }

    private static String schema(Type type) {
        ResolvedSchema resolved = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type));
        if (resolved == null || resolved.schema == null) {
            throw new IllegalStateException("SpringDoc schema unavailable");
        }
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("schema", resolved.schema);
            document.put("components", Map.of("schemas", resolved.referencedSchemas));
            return Json.mapper().writeValueAsString(document);
        } catch (Exception exception) {
            throw new IllegalStateException("SpringDoc schema unavailable", exception);
        }
    }

    public record ResolvedOperation(List<OpenApiParameterDefinition> parameters, String requestSchema,
                                    String responseSchema) {
    }

    private record ParameterLocation(String name, String location, boolean required) {
    }

}
