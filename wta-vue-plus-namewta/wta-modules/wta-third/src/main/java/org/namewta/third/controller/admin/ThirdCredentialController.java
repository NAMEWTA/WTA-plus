package org.namewta.third.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.namewta.third.domain.bo.ThirdCredentialBo;
import org.namewta.third.usecase.ThirdCredentialUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/third/credential")
public class ThirdCredentialController {
    private final ThirdCredentialUseCase credentialUseCase;

    @GetMapping("/list")
    @SaCheckPermission("third:credential:list")
    public R<?> list(@RequestParam String providerCode, @RequestParam(required = false) String endpointCode) {
        return R.ok(credentialUseCase.list(providerCode, endpointCode));
    }

    @PostMapping
    @Log(title = "第三方凭据", businessType = BusinessType.INSERT)
    @SaCheckPermission("third:credential:add")
    public R<Void> save(@Valid @RequestBody ThirdCredentialBo bo) {
        credentialUseCase.save(bo);
        return R.ok();
    }

    @PostMapping("/{credentialId}/remove")
    @Log(title = "第三方凭据", businessType = BusinessType.DELETE)
    @SaCheckPermission("third:credential:remove")
    public R<Void> remove(@PathVariable Long credentialId) {
        credentialUseCase.remove(credentialId);
        return R.ok();
    }
}
