package org.namewta.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.web.core.BaseController;
import org.namewta.demo.domain.bo.TestRichTextBo;
import org.namewta.demo.domain.bo.TestRichTextVersionBo;
import org.namewta.demo.domain.vo.TestRichTextAssetVo;
import org.namewta.demo.domain.vo.TestRichTextSummaryVo;
import org.namewta.demo.domain.vo.TestRichTextVo;
import org.namewta.demo.service.ITestRichTextService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 富文本通用能力演示接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/demo/rich-text")
public class TestRichTextController extends BaseController {
    private final ITestRichTextService service;

    @SaCheckPermission("demo:richtext:list")
    @GetMapping("/list")
    public R<PageResult<TestRichTextSummaryVo>> list(PageQuery pageQuery) { return R.ok(service.list(pageQuery)); }

    @SaCheckPermission("demo:richtext:add")
    @Log(title = "富文本演示", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/create")
    public R<TestRichTextVo> create(@Valid @RequestBody TestRichTextBo bo) { return R.ok(service.create(bo)); }

    @SaCheckPermission("demo:richtext:edit")
    @Log(title = "富文本演示", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/update")
    public R<TestRichTextVo> update(@PathVariable Long id, @Valid @RequestBody TestRichTextBo bo) { return R.ok(service.update(id, bo)); }

    @SaCheckPermission("demo:richtext:remove")
    @Log(title = "富文本演示", businessType = BusinessType.DELETE, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/remove")
    public R<Void> remove(@PathVariable Long id, @Valid @RequestBody TestRichTextVersionBo bo) { service.remove(id, bo.getVersion()); return R.ok(); }

    @SaCheckPermission("demo:richtext:query")
    @GetMapping("/assets")
    public R<List<TestRichTextAssetVo>> assets(@RequestParam String ossIds, @RequestParam(required = false) Long richTextId) { return R.ok(service.assets(ossIds, richTextId)); }

    @SaCheckPermission("demo:richtext:query")
    @GetMapping("/{id}")
    public R<TestRichTextVo> get(@NotNull @PathVariable Long id) { return R.ok(service.get(id)); }
}
