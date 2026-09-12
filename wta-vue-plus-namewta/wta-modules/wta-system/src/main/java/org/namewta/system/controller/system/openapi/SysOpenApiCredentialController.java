package org.namewta.system.controller.system.openapi;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.constant.HttpStatus;
import org.namewta.common.core.domain.R;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.openapi.credential.model.OpenApiCredentialCreateRequest;
import org.namewta.system.openapi.credential.model.OpenApiCredentialIssued;
import org.namewta.system.openapi.credential.model.OpenApiCredentialSummary;
import org.namewta.system.openapi.credential.model.OpenApiCredentialUserSummary;
import org.namewta.system.openapi.credential.service.SystemOpenApiCredentialService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Current-user and super-admin credential lifecycle endpoints.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/openApi")
@ConditionalOnProperty(prefix = "openapi", name = "enabled", havingValue = "true")
public class SysOpenApiCredentialController {

    private static final String LOG_TITLE = "OpenAPI凭据";

    private final SystemOpenApiCredentialService credentialService;

    @SaCheckPermission("system:openApi:self")
    @GetMapping("/self/credential")
    public R<OpenApiCredentialSummary> getSelfCredential() {
        return R.ok(requiredCredential(credentialService.get(LoginHelper.getUserId())));
    }

    @SaCheckPermission("system:openApi:self")
    @Log(title = LOG_TITLE, businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/self/credential/create")
    public R<OpenApiCredentialIssued> createSelf(@Valid @RequestBody OpenApiCredentialCreateRequest request,
                                                  HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return R.ok(credentialService.create(LoginHelper.getUserId(), request));
    }

    @SaCheckPermission("system:openApi:self")
    @Log(title = LOG_TITLE, businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/self/credential/reset")
    public R<OpenApiCredentialIssued> resetSelf(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return R.ok(credentialService.reset(LoginHelper.getUserId()));
    }

    @SaCheckPermission("system:openApi:self")
    @Log(title = LOG_TITLE, businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/self/credential/enable")
    public R<OpenApiCredentialSummary> enableSelf() {
        return R.ok(credentialService.enable(LoginHelper.getUserId()));
    }

    @SaCheckPermission("system:openApi:self")
    @Log(title = LOG_TITLE, businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/self/credential/disable")
    public R<OpenApiCredentialSummary> disableSelf() {
        return R.ok(credentialService.disable(LoginHelper.getUserId()));
    }

    @SaCheckPermission("system:openApi:self")
    @Log(title = LOG_TITLE, businessType = BusinessType.DELETE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/self/credential/delete")
    public R<Void> deleteSelf() {
        credentialService.delete(LoginHelper.getUserId());
        return R.ok();
    }

    @SaCheckPermission("system:openApi:list")
    @GetMapping("/users")
    public R<List<OpenApiCredentialUserSummary>> users(
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "50") int limit) {
        requireSuperAdmin();
        return R.ok(credentialService.users(keyword, limit));
    }

    @SaCheckPermission("system:openApi:query")
    @GetMapping("/users/{userId}/credential")
    public R<OpenApiCredentialSummary> getUserCredential(@PathVariable Long userId) {
        requireSuperAdmin();
        return R.ok(requiredCredential(credentialService.get(userId)));
    }

    @SaCheckPermission("system:openApi:add")
    @Log(title = LOG_TITLE, businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/users/{userId}/credential/create")
    public R<OpenApiCredentialIssued> createUser(@PathVariable Long userId,
                                                  @Valid @RequestBody OpenApiCredentialCreateRequest request,
                                                  HttpServletResponse response) {
        requireSuperAdmin();
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return R.ok(credentialService.create(userId, request));
    }

    @SaCheckPermission("system:openApi:edit")
    @Log(title = LOG_TITLE, businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/users/{userId}/credential/reset")
    public R<OpenApiCredentialIssued> resetUser(@PathVariable Long userId, HttpServletResponse response) {
        requireSuperAdmin();
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return R.ok(credentialService.reset(userId));
    }

    @SaCheckPermission("system:openApi:edit")
    @Log(title = LOG_TITLE, businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/users/{userId}/credential/enable")
    public R<OpenApiCredentialSummary> enableUser(@PathVariable Long userId) {
        requireSuperAdmin();
        return R.ok(credentialService.enable(userId));
    }

    @SaCheckPermission("system:openApi:edit")
    @Log(title = LOG_TITLE, businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/users/{userId}/credential/disable")
    public R<OpenApiCredentialSummary> disableUser(@PathVariable Long userId) {
        requireSuperAdmin();
        return R.ok(credentialService.disable(userId));
    }

    @SaCheckPermission("system:openApi:remove")
    @Log(title = LOG_TITLE, businessType = BusinessType.DELETE,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/users/{userId}/credential/delete")
    public R<Void> deleteUser(@PathVariable Long userId) {
        requireSuperAdmin();
        credentialService.delete(userId);
        return R.ok();
    }

    private static void requireSuperAdmin() {
        if (!LoginHelper.isSuperAdmin()) {
            throw new ServiceException("OpenAPI credential management is unavailable");
        }
    }

    private static OpenApiCredentialSummary requiredCredential(OpenApiCredentialSummary credential) {
        if (credential == null) {
            throw new ServiceException("OpenAPI credential is unavailable", HttpStatus.NOT_FOUND);
        }
        return credential;
    }
}
