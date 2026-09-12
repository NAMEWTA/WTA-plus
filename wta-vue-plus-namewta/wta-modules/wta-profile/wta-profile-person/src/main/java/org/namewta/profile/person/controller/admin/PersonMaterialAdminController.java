package org.namewta.profile.person.controller.admin;

import jakarta.validation.Valid;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.person.usecase.ProfileMaterialUseCase;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialReferenceView;
import org.namewta.profile.person.domain.vo.PersonProfileAccessUrl;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PersonMaterialAdminController HTTP 接口，负责参数校验和响应包装。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/person/materials")
public class PersonMaterialAdminController {

    private final ProfileMaterialUseCase materialPort;

    /**
     * 处理 list HTTP 请求。
     */
    @GetMapping("/{ownerType}/{ownerId}")
    @SaCheckPermission(value = {"profile:person:material", "profile:person:query",
        "profile:person:review", "profile:person:manage", "profile:person:override"}, mode = SaMode.OR)
    public R<List<MaterialReferenceView>> list(@PathVariable MaterialOwnerType ownerType,
                                               @PathVariable Long ownerId) {
        return R.ok(materialPort.list(owner(ownerType, ownerId)));
    }

    /**
     * 处理 accessUrl HTTP 请求。
     */
    @GetMapping("/{ownerType}/{ownerId}/{materialRefId}/access-url")
    @SaCheckPermission(value = {"profile:person:material", "profile:person:query",
        "profile:person:review", "profile:person:manage", "profile:person:override"}, mode = SaMode.OR)
    public R<PersonProfileAccessUrl> accessUrl(@PathVariable MaterialOwnerType ownerType,
                                                @PathVariable Long ownerId,
                                                @PathVariable Long materialRefId) {
        return R.ok(materialPort.accessUrlView(owner(ownerType, ownerId), materialRefId));
    }

    /**
     * 处理 owner HTTP 请求。
     */
    private MaterialOwnerKey owner(MaterialOwnerType ownerType, Long ownerId) {
        return new MaterialOwnerKey(ProfileType.PERSON, ownerType, ownerId);
    }
}
