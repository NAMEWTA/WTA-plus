package org.namewta.system.controller.system.openapi;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.openapi.catalog.OpenApiCatalogItem;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.openapi.catalog.SystemOpenApiCatalogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Credential-independent self and super-admin catalog endpoints.
 */
@RestController
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@RequestMapping("/system/openApi")
public class SysOpenApiCatalogController {

    private final SystemOpenApiCatalogService catalogService;

    @SaCheckPermission("system:openApi:self")
    @GetMapping("/self/interfaces")
    public R<List<OpenApiCatalogItem>> selfInterfaces() {
        return R.ok(catalogService.list(LoginHelper.getUserId()));
    }

    @SaCheckPermission("system:openApi:self")
    @GetMapping("/self/interfaces/{interfaceId}")
    public R<OpenApiCatalogItem> selfInterface(@PathVariable String interfaceId) {
        return R.ok(catalogService.detail(LoginHelper.getUserId(), interfaceId));
    }

    @SaCheckPermission("system:openApi:list")
    @GetMapping("/users/{userId}/interfaces")
    public R<List<OpenApiCatalogItem>> userInterfaces(@PathVariable Long userId) {
        requireSuperAdmin();
        return R.ok(catalogService.list(userId));
    }

    @SaCheckPermission("system:openApi:query")
    @GetMapping("/users/{userId}/interfaces/{interfaceId}")
    public R<OpenApiCatalogItem> userInterface(@PathVariable Long userId,
                                               @PathVariable String interfaceId) {
        requireSuperAdmin();
        return R.ok(catalogService.detail(userId, interfaceId));
    }

    private static void requireSuperAdmin() {
        if (!LoginHelper.isSuperAdmin()) {
            throw new ServiceException("OpenAPI target catalog is unavailable");
        }
    }

}
