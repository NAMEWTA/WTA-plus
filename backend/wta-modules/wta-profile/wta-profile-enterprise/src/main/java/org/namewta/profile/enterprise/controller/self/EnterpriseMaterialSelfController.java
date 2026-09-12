package org.namewta.profile.enterprise.controller.self;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.enterprise.usecase.ProfileMaterialUseCase;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialAttachCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EnterpriseMaterialSelfController HTTP 接口，负责参数校验和响应包装。
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/profile/enterprise/materials")
public class EnterpriseMaterialSelfController {

    private final ProfileMaterialUseCase materialPort;

    /**
     * 处理 attach HTTP 请求。
     */
    @PostMapping("/{ownerType}/{ownerId}")
    @SaCheckPermission("profile:enterprise:material")
    @Log(title = "挂接企业认证材料", businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<MaterialReferenceView> attach(@PathVariable MaterialOwnerType ownerType,
                                           @Positive @PathVariable Long ownerId,
                                           @Valid @RequestBody AttachRequest request) {
        return R.ok(materialPort.attach(new MaterialAttachCommand(owner(ownerType, ownerId),
            request.ossId(), request.materialNodeId())));
    }

    /**
     * 处理 detach HTTP 请求。
     */
    @PostMapping("/{ownerType}/{ownerId}/{materialRefId}/detach")
    @SaCheckPermission("profile:enterprise:material")
    @Log(title = "解除企业认证材料", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> detach(@PathVariable MaterialOwnerType ownerType, @Positive @PathVariable Long ownerId,
                          @Positive @PathVariable Long materialRefId) {
        materialPort.detach(owner(ownerType, ownerId), materialRefId);
        return R.ok();
    }

    /**
     * 处理 profileType HTTP 请求。
     */
    ProfileType profileType() {
        return ProfileType.ENTERPRISE;
    }

    /**
     * 处理 owner HTTP 请求。
     */
    private MaterialOwnerKey owner(MaterialOwnerType ownerType, Long ownerId) {
        return new MaterialOwnerKey(profileType(), ownerType, ownerId);
    }

    /**
     * AttachRequest HTTP 接口，负责参数校验和响应包装。
     */
    public record AttachRequest(@Positive Long ossId, @Positive Long materialNodeId) {
    }
}
