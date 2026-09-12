package org.namewta.third.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.third.domain.bo.ThirdEndpointBo;
import org.namewta.third.domain.vo.ThirdEndpointVo;
import org.namewta.third.usecase.ThirdEndpointUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/third/endpoint")
public class ThirdEndpointController {
    private final ThirdEndpointUseCase useCase;
    @SaCheckPermission("third:endpoint:list") @GetMapping("/list") public R<List<ThirdEndpointVo>> list(@RequestParam(required = false) Long providerId, @RequestParam(required = false) String keyword) { return R.ok(useCase.list(providerId, keyword)); }
    @SaCheckPermission("third:endpoint:query") @GetMapping("/{endpointId}") public R<ThirdEndpointVo> get(@PathVariable Long endpointId) { return R.ok(useCase.get(endpointId)); }
    @SaCheckPermission("third:endpoint:add") @Log(title = "第三方Endpoint", businessType = BusinessType.INSERT) @PostMapping public R<Void> add(@Valid @RequestBody ThirdEndpointBo bo) { useCase.save(bo); return R.ok(); }
    @SaCheckPermission("third:endpoint:edit") @Log(title = "第三方Endpoint", businessType = BusinessType.UPDATE) @PostMapping("/save") public R<Void> save(@Valid @RequestBody ThirdEndpointBo bo) { useCase.save(bo); return R.ok(); }
    @SaCheckPermission("third:endpoint:edit") @Log(title = "第三方Endpoint状态", businessType = BusinessType.UPDATE) @PostMapping("/{endpointId}/status") public R<Void> status(@PathVariable Long endpointId, @RequestParam String status) { useCase.changeStatus(endpointId, status); return R.ok(); }
    @SaCheckPermission("third:endpoint:remove") @Log(title = "第三方Endpoint", businessType = BusinessType.DELETE) @PostMapping("/{endpointId}/remove") public R<Void> remove(@PathVariable Long endpointId) { useCase.remove(endpointId); return R.ok(); }
}
