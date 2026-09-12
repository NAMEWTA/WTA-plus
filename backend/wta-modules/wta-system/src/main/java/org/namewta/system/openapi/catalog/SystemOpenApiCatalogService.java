package org.namewta.system.openapi.catalog;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.openapi.catalog.OpenApiCatalogItem;
import org.namewta.common.openapi.protocol.OpenApiCanonicalizer;
import org.namewta.common.openapi.protocol.OpenApiHeaders;
import org.namewta.common.openapi.registry.OpenApiAuthorizationMatcher;
import org.namewta.common.openapi.registry.OpenApiOperationDefinition;
import org.namewta.common.openapi.registry.OpenApiOperationRegistry;
import org.namewta.common.openapi.spi.OpenApiAuthorizationResolver;
import org.namewta.system.api.model.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only catalog filtered solely by the target user's authoritative snapshot.
 */
@Service
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SystemOpenApiCatalogService {

    private final OpenApiOperationRegistry registry;
    private final OpenApiAuthorizationResolver authorizationResolver;
    private final OpenApiAuthorizationMatcher authorizationMatcher;

    public List<OpenApiCatalogItem> list(Long targetUserId) {
        LoginUser target = authorizationResolver.resolve(targetUserId);
        return registry.all().stream()
            .filter(operation -> authorizationMatcher.matches(target, operation.accessRule()))
            .map(SystemOpenApiCatalogService::toCatalogItem)
            .toList();
    }

    public OpenApiCatalogItem detail(Long targetUserId, String interfaceId) {
        LoginUser target = authorizationResolver.resolve(targetUserId);
        OpenApiOperationDefinition operation = registry.find(interfaceId);
        if (operation == null || !authorizationMatcher.matches(target, operation.accessRule())) {
            throw new ServiceException("OpenAPI interface is unavailable");
        }
        return toCatalogItem(operation);
    }

    private static OpenApiCatalogItem toCatalogItem(OpenApiOperationDefinition operation) {
        return new OpenApiCatalogItem(operation.interfaceId(), operation.summary(), operation.method(), operation.path(),
            operation.accessRule(), operation.parameters(), operation.requestSchema(), operation.responseSchema(),
            curlExample(operation), javaExample(operation));
    }

    private static String curlExample(OpenApiOperationDefinition operation) {
        String requestBody = operation.requestSchema() == null ? "" : """
             \\
              -H 'Content-Type: application/json' \\
              --data '<JSON_BODY>'
            """.stripTrailing();
        return """
            curl -X %s 'https://api.example.com%s' \
              -H '%s: %s' \
              -H '%s: <APP_KEY>' \
              -H '%s: <UNIX_SECONDS>' \
              -H '%s: <BASE64URL_128_BIT_NONCE>' \
              -H '%s: <NAMEWTA_V1_SIGNATURE>'%s
            """.formatted(operation.method(), operation.path(),
            OpenApiHeaders.VERSION, OpenApiCanonicalizer.VERSION, OpenApiHeaders.APP_KEY,
            OpenApiHeaders.TIMESTAMP, OpenApiHeaders.NONCE, OpenApiHeaders.SIGNATURE, requestBody).strip();
    }

    private static String javaExample(OpenApiOperationDefinition operation) {
        String bodyPublisher = operation.requestSchema() == null
            ? "HttpRequest.BodyPublishers.noBody()"
            : "HttpRequest.BodyPublishers.ofString(\"<JSON_BODY>\")";
        String contentType = operation.requestSchema() == null
            ? ""
            : "\n    .header(\"Content-Type\", \"application/json\")";
        return """
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.example.com%s"))
                .method("%s", %s)%s
                .header("%s", "%s")
                .header("%s", appKey)
                .header("%s", timestamp)
                .header("%s", nonce)
                .header("%s", namewtaV1Signature)
                .build();
            """.formatted(operation.path(), operation.method(), bodyPublisher, contentType,
            OpenApiHeaders.VERSION, OpenApiCanonicalizer.VERSION, OpenApiHeaders.APP_KEY,
            OpenApiHeaders.TIMESTAMP, OpenApiHeaders.NONCE, OpenApiHeaders.SIGNATURE).strip();
    }

}
