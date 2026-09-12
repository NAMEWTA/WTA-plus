package org.namewta.third.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.third.domain.bo.ThirdProviderBo;
import org.namewta.third.domain.vo.ThirdProviderVo;
import org.namewta.third.usecase.ThirdProviderUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/third/provider")
public class ThirdProviderController {
    private final ThirdProviderUseCase useCase;
    @SaCheckPermission("third:provider:list") @GetMapping("/list") public R<List<ThirdProviderVo>> list(@RequestParam(required = false) String keyword) { return R.ok(useCase.list(keyword)); }
    @SaCheckPermission("third:provider:query") @GetMapping("/{providerId}") public R<ThirdProviderVo> get(@PathVariable Long providerId) { return R.ok(useCase.get(providerId)); }
    @SaCheckPermission("third:provider:add") @Log(title = "第三方供应商", businessType = BusinessType.INSERT) @PostMapping public R<Void> add(@Valid @RequestBody ThirdProviderBo bo) { useCase.save(bo); return R.ok(); }
    @SaCheckPermission("third:provider:edit") @Log(title = "第三方供应商", businessType = BusinessType.UPDATE) @PostMapping("/save") public R<Void> save(@Valid @RequestBody ThirdProviderBo bo) { useCase.save(bo); return R.ok(); }
    @SaCheckPermission("third:provider:edit") @Log(title = "第三方供应商状态", businessType = BusinessType.UPDATE) @PostMapping("/{providerId}/status") public R<Void> status(@PathVariable Long providerId, @RequestParam String status) { useCase.changeStatus(providerId, status); return R.ok(); }
    @SaCheckPermission("third:provider:remove") @Log(title = "第三方供应商", businessType = BusinessType.DELETE) @PostMapping("/{providerId}/remove") public R<Void> remove(@PathVariable Long providerId) { useCase.remove(providerId); return R.ok(); }
}
