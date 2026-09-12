package org.namewta.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.richtext.RichTextAsset;
import org.namewta.common.richtext.RichTextContent;
import org.namewta.common.richtext.RichTextProcessor;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.demo.domain.TestRichText;
import org.namewta.demo.domain.bo.TestRichTextBo;
import org.namewta.demo.domain.vo.TestRichTextAssetVo;
import org.namewta.demo.domain.vo.TestRichTextSummaryVo;
import org.namewta.demo.domain.vo.TestRichTextVo;
import org.namewta.demo.mapper.TestRichTextMapper;
import org.namewta.system.api.OssService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** 富文本演示服务实现，负责文档所有权、内容校验和 OSS 引用对账。 */
@Service
@RequiredArgsConstructor
public class TestRichTextServiceImpl implements org.namewta.demo.service.ITestRichTextService {
    private static final String REF_TYPE = "test_rich_text";
    private final TestRichTextMapper mapper;
    private final OssService ossService;

    @Override
    public PageResult<TestRichTextSummaryVo> list(PageQuery pageQuery) {
        requireUser();
        Long userId = LoginHelper.getUserId();
        Long clientPk = clientPk();
        Page<TestRichTextSummaryVo> page = mapper.selectVoPage(pageQuery.build(),
            new LambdaQueryWrapper<TestRichText>().eq(TestRichText::getCreateBy, userId)
                .eq(TestRichText::getClientPk, clientPk).orderByDesc(TestRichText::getUpdateTime));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public TestRichTextVo get(Long id) {
        TestRichText entity = owned(id);
        return view(entity);
    }

    @Override
    @DSTransactional
    public TestRichTextVo create(TestRichTextBo bo) {
        requireClient();
        RichTextContent content = normalize(bo.getHtml());
        validateAssets(content.assets(), null);
        TestRichText entity = new TestRichText();
        entity.setClientPk(clientPk());
        entity.setTitle(bo.getTitle().strip());
        entity.setContentHtml(content.html());
        if (mapper.insert(entity) <= 0) throw new ServiceException("富文本保存失败");
        reconcile(null, entity, content);
        return view(entity);
    }

    @Override
    @DSTransactional
    public TestRichTextVo update(Long id, TestRichTextBo bo) {
        if (bo.getVersion() == null) throw new ServiceException("版本不能为空");
        TestRichText old = owned(id);
        RichTextContent content = normalize(bo.getHtml());
        validateAssets(content.assets(), old);
        TestRichText entity = new TestRichText();
        entity.setRichTextId(id);
        entity.setTitle(bo.getTitle().strip());
        entity.setContentHtml(content.html());
        entity.setVersion(bo.getVersion());
        entity.setClientPk(old.getClientPk());
        if (mapper.updateById(entity) <= 0) throw new ServiceException("富文本已被其他请求修改，请刷新后重试");
        entity.setVersion(bo.getVersion() + 1);
        reconcile(old, entity, content);
        return view(entity);
    }

    @Override
    @DSTransactional
    public void remove(Long id, Long version) {
        TestRichText old = owned(id);
        TestRichText deleting = new TestRichText();
        deleting.setRichTextId(id); deleting.setVersion(version);
        if (mapper.deleteById(deleting) <= 0) throw new ServiceException("富文本已被其他请求修改，请刷新后重试");
        RichTextContent content = normalize(old.getContentHtml());
        ossService.reconcileReferences(REF_TYPE, id.toString(), content.ossIds(), List.of());
    }

    @Override
    public List<TestRichTextAssetVo> assets(String ossIds, Long richTextId) {
        Collection<Long> ids = parseIds(ossIds);
        if (richTextId != null) {
            RichTextContent content = normalize(owned(richTextId).getContentHtml());
            if (!content.ossIds().containsAll(ids)) throw new ServiceException("资源不属于当前富文本");
        }
        return ids.stream().map(id -> resolve(id, richTextId != null)).toList();
    }

    private TestRichTextAssetVo resolve(Long id, boolean referencedDocument) {
        try {
            OssService.OssObjectMetadata metadata = ossService.objectMetadata(id);
            if (!LoginHelper.getUserId().equals(metadata.uploaderUserId())
                || !clientPk().equals(metadata.uploaderClientPk())) return unavailable(id);
            if (!referencedDocument && !ossService.snapshot(id).temporary()) return unavailable(id);
            OssService.OssAccessUrl access = ossService.resolveAccessUrl(id);
            return TestRichTextAssetVo.builder().ossId(id.toString()).status("available").url(access.url())
                .expiresAt(access.expiresAt()).fileName(metadata.fileName()).contentType(metadata.contentType()).build();
        } catch (RuntimeException ex) {
            return unavailable(id);
        }
    }

    private void validateAssets(List<RichTextAsset> assets, TestRichText existing) {
        List<Long> previousIds = existing == null ? List.of() : normalize(existing.getContentHtml()).ossIds();
        for (RichTextAsset asset : assets) {
            OssService.OssObjectMetadata metadata;
            try { metadata = ossService.objectMetadata(asset.ossId()); }
            catch (RuntimeException ex) { throw new ServiceException("富文本资源不存在"); }
            if (!LoginHelper.getUserId().equals(metadata.uploaderUserId()) || !clientPk().equals(metadata.uploaderClientPk())) {
                throw new ServiceException("富文本资源不属于当前用户或 Client");
            }
            if (!Boolean.TRUE.equals(ossService.snapshot(asset.ossId()).temporary()) && !previousIds.contains(asset.ossId())) {
                throw new ServiceException("新增内容只能引用临时上传资源");
            }
            if (!kindMatches(asset, metadata.contentType())) throw new ServiceException("富文本资源类型不匹配");
        }
    }

    private boolean kindMatches(RichTextAsset asset, String contentType) {
        if (contentType == null) return false;
        return switch (asset.kind()) {
            case IMAGE -> contentType.startsWith("image/");
            case AUDIO -> contentType.startsWith("audio/");
            case VIDEO -> contentType.startsWith("video/");
            case ATTACHMENT -> !contentType.startsWith("image/") && !contentType.startsWith("audio/") && !contentType.startsWith("video/");
        };
    }

    private void reconcile(TestRichText old, TestRichText current, RichTextContent content) {
        List<Long> previous = old == null ? List.of() : normalize(old.getContentHtml()).ossIds();
        ossService.reconcileReferences(REF_TYPE, current.getRichTextId().toString(), previous, content.ossIds());
    }

    private TestRichText owned(Long id) {
        requireClient();
        TestRichText entity = mapper.selectOne(new LambdaQueryWrapper<TestRichText>().eq(TestRichText::getRichTextId, id)
            .eq(TestRichText::getCreateBy, LoginHelper.getUserId()).eq(TestRichText::getClientPk, clientPk()));
        if (entity == null) throw new ServiceException("富文本不存在或无权访问");
        return entity;
    }

    private RichTextContent normalize(String html) { try { return RichTextProcessor.normalize(html); } catch (IllegalArgumentException ex) { throw new ServiceException(ex.getMessage()); } }
    private void requireUser() { if (LoginHelper.getLoginUser() == null || LoginHelper.getUserId() == null) throw new ServiceException("当前会话已失效，请重新登录"); }
    private Long clientPk() { requireUser(); var user = LoginHelper.getLoginUser(); if (user.getClientPk() == null) throw new ServiceException("当前会话缺少 Client"); return user.getClientPk(); }
    private void requireClient() { clientPk(); }
    private TestRichTextVo view(TestRichText e) { TestRichTextVo v = new TestRichTextVo(); v.setRichTextId(e.getRichTextId()); v.setTitle(e.getTitle()); v.setHtml(e.getContentHtml()); v.setVersion(e.getVersion()); v.setUpdateTime(e.getUpdateTime()); return v; }
    private Collection<Long> parseIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            List<Long> ids = Arrays.stream(value.split(",")).map(String::trim).map(Long::valueOf).distinct().toList();
            if (ids.size() > 100 || ids.stream().anyMatch(id -> id <= 0)) throw new ServiceException("OSS ID 参数无效");
            return ids;
        } catch (ServiceException ex) { throw ex; }
        catch (RuntimeException ex) { throw new ServiceException("OSS ID 参数无效"); }
    }
    private TestRichTextAssetVo unavailable(Long id) { return TestRichTextAssetVo.builder().ossId(id.toString()).status("unavailable").build(); }
}
