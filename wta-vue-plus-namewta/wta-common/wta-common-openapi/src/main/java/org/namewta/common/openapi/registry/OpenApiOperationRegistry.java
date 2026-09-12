package org.namewta.common.openapi.registry;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckOr;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import org.namewta.common.openapi.annotation.OpenApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Startup registry built only from real MVC mappings and method-level opt-in.
 */
public final class OpenApiOperationRegistry implements InitializingBean {

    private final RequestMappingHandlerMapping handlerMapping;
    private final SpringDocOperationSchemaResolver schemaResolver;
    private volatile Map<String, OpenApiOperationDefinition> operations = Map.of();
    private volatile int rejectedMappings;

    public OpenApiOperationRegistry(RequestMappingHandlerMapping handlerMapping,
                                    SpringDocOperationSchemaResolver schemaResolver) {
        this.handlerMapping = handlerMapping;
        this.schemaResolver = schemaResolver;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, OpenApiOperationDefinition> resolved = new LinkedHashMap<>();
        int rejected = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> mapping : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = mapping.getValue();
            OpenApi annotation = handler.getMethod().getDeclaredAnnotation(OpenApi.class);
            if (annotation == null) {
                continue;
            }
            try {
                SpringDocOperationSchemaResolver.ResolvedOperation schemas = schemaResolver.resolve(handler);
                Map<String, OpenApiOperationDefinition> handlerOperations = new LinkedHashMap<>();
                for (String path : paths(mapping.getKey())) {
                    for (RequestMethod method : methods(mapping.getKey())) {
                        OpenApiOperationDefinition operation = new OpenApiOperationDefinition(
                            interfaceId(method.name(), path), annotation.value(), method.name(), path,
                            accessRule(handler), schemas.parameters(), schemas.requestSchema(), schemas.responseSchema());
                        if (resolved.containsKey(operation.interfaceId())
                            || handlerOperations.putIfAbsent(operation.interfaceId(), operation) != null) {
                            throw new IllegalStateException("Duplicate OpenAPI interface identity");
                        }
                    }
                }
                resolved.putAll(handlerOperations);
            } catch (RuntimeException exception) {
                rejected++;
            }
        }
        operations = resolved.values().stream()
            .sorted(Comparator.comparing(OpenApiOperationDefinition::path)
                .thenComparing(OpenApiOperationDefinition::method))
            .collect(LinkedHashMap::new, (map, item) -> map.put(item.interfaceId(), item), Map::putAll);
        operations = Collections.unmodifiableMap(new LinkedHashMap<>(operations));
        rejectedMappings = rejected;
    }

    public List<OpenApiOperationDefinition> all() {
        return List.copyOf(operations.values());
    }

    public OpenApiOperationDefinition find(String interfaceId) {
        return operations.get(interfaceId);
    }

    public int rejectedMappings() {
        return rejectedMappings;
    }

    private static List<String> paths(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() == null || info.getPathPatternsCondition().getPatterns().isEmpty()) {
            throw new IllegalStateException("OpenAPI path unavailable");
        }
        return info.getPathPatternsCondition().getPatternValues().stream().sorted().toList();
    }

    private static List<RequestMethod> methods(RequestMappingInfo info) {
        if (info.getMethodsCondition().getMethods().isEmpty()) {
            throw new IllegalStateException("OpenAPI method unavailable");
        }
        return info.getMethodsCondition().getMethods().stream().sorted().toList();
    }

    private static OpenApiAccessRule accessRule(HandlerMethod handler) {
        if (handler.getBeanType().isAnnotationPresent(SaIgnore.class)
            || handler.getMethod().isAnnotationPresent(SaIgnore.class)
            || handler.getBeanType().isAnnotationPresent(SaCheckOr.class)
            || handler.getMethod().isAnnotationPresent(SaCheckOr.class)) {
            throw new IllegalStateException("Unsupported Sa-Token annotation composition");
        }
        List<OpenApiAccessRule.PermissionRule> permissions = new ArrayList<>();
        List<OpenApiAccessRule.RoleRule> roles = new ArrayList<>();
        addPermission(permissions, handler.getBeanType().getAnnotation(SaCheckPermission.class));
        addPermission(permissions, handler.getMethod().getAnnotation(SaCheckPermission.class));
        addRole(roles, handler.getBeanType().getAnnotation(SaCheckRole.class));
        addRole(roles, handler.getMethod().getAnnotation(SaCheckRole.class));
        return new OpenApiAccessRule(permissions, roles);
    }

    private static void addPermission(List<OpenApiAccessRule.PermissionRule> target, SaCheckPermission value) {
        if (value != null) {
            requireDefaultLoginType(value.type());
            target.add(new OpenApiAccessRule.PermissionRule(List.of(value.value()),
                OpenApiAccessRule.Mode.valueOf(value.mode().name()), List.of(value.orRole())));
        }
    }

    private static void addRole(List<OpenApiAccessRule.RoleRule> target, SaCheckRole value) {
        if (value != null) {
            requireDefaultLoginType(value.type());
            target.add(new OpenApiAccessRule.RoleRule(List.of(value.value()),
                OpenApiAccessRule.Mode.valueOf(value.mode().name())));
        }
    }

    private static void requireDefaultLoginType(String type) {
        if (!type.isBlank()) {
            throw new IllegalStateException("Unsupported Sa-Token login type");
        }
    }

    private static String interfaceId(String method, String path) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((method + "\n" + path).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

}
