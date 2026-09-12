package org.namewta.third.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.third.usecase.ThirdObservabilityUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/third")
public class ThirdObservabilityController {
    private final ThirdObservabilityUseCase observabilityService;

    @GetMapping("/invocation/list")
    @SaCheckPermission("third:invocation:list")
    public R<?> invocations(@RequestParam(required = false) String providerCode) {
        return R.ok(observabilityService.invocations(providerCode));
    }

    @GetMapping("/statistics/list")
    @SaCheckPermission("third:statistics:list")
    public R<?> statistics(@RequestParam(required = false) String providerCode) {
        return R.ok(observabilityService.statistics(providerCode));
    }
}
