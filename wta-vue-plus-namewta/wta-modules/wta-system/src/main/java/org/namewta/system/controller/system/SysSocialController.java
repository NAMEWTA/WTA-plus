package org.namewta.system.controller.system;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.common.web.core.BaseController;
import org.namewta.system.domain.vo.SysSocialVo;
import org.namewta.system.service.ISysSocialService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社会化关系
 *
 * @author thiszhc
 * @date 2023-06-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/social")
public class SysSocialController extends BaseController {

    private final ISysSocialService socialUserService;

    /**
     * 查询当前登录用户的社会化账号绑定列表。
     *
     * @return 绑定关系列表
     */
    @GetMapping("/list")
    public R<List<SysSocialVo>> list() {
        return R.ok(socialUserService.queryListByUserId(LoginHelper.getUserId()));
    }

}
