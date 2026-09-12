package org.namewta.profile.person.controller.admin;

import jakarta.validation.Valid;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.profile.person.usecase.ProfileMaterialUseCase;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeCommand;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialNodeView;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialScope;
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
 * MaterialTagController HTTP 接口，负责参数校验和响应包装。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile/material-tags")
public class MaterialTagController {

    private final ProfileMaterialUseCase materialPort;

    /**
     * 处理 tree HTTP 请求。
     */
    @GetMapping("/tree")
    @SaCheckPermission(value = {"profile:material-tag:query", "profile:person:material",
        "profile:enterprise:material", "profile:person:override", "profile:enterprise:override"}, mode = SaMode.OR)
    public R<List<MaterialNodeView>> tree(@RequestParam MaterialScope scope,
                                          @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return R.ok(materialPort.tree(scope, includeDisabled));
    }

    /**
     * 处理 create HTTP 请求。
     */
    @PostMapping
    @SaCheckPermission("profile:material-tag:manage")
    @Log(title = "新增档案材料节点", businessType = BusinessType.INSERT,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<MaterialNodeView> create(@Valid @RequestBody MaterialNodeCommand command) {
        return R.ok(materialPort.createNode(command));
    }

    /**
     * 处理 update HTTP 请求。
     */
    @PostMapping("/{materialNodeId}")
    @SaCheckPermission("profile:material-tag:manage")
    @Log(title = "修改档案材料节点", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<MaterialNodeView> update(@PathVariable Long materialNodeId,
                                      @Valid @RequestBody MaterialNodeCommand command) {
        return R.ok(materialPort.updateNode(materialNodeId, command));
    }

    /**
     * 处理 status HTTP 请求。
     */
    @PostMapping("/{materialNodeId}/status")
    @SaCheckPermission("profile:material-tag:manage")
    @Log(title = "变更档案材料节点状态", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> status(@PathVariable Long materialNodeId, @Valid @RequestBody StatusCommand command) {
        materialPort.changeStatus(materialNodeId, command.enabled(), command.expectedVersion());
        return R.ok();
    }

    /**
     * 处理 archive HTTP 请求。
     */
    @PostMapping("/{materialNodeId}/archive")
    @SaCheckPermission("profile:material-tag:manage")
    @Log(title = "归档档案材料节点", businessType = BusinessType.UPDATE,
        isSaveRequestData = false, isSaveResponseData = false)
    public R<Void> archive(@PathVariable Long materialNodeId, @Valid @RequestBody VersionCommand command) {
        materialPort.archiveNode(materialNodeId, command.expectedVersion());
        return R.ok();
    }

    /** 材料节点状态变更请求。 */
    public record StatusCommand(boolean enabled, int expectedVersion) {
    }

    /** 材料节点版本变更请求。 */
    public record VersionCommand(int expectedVersion) {
    }
}
