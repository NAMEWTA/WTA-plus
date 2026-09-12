package org.namewta.demo.service;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.demo.domain.bo.TestRichTextBo;
import org.namewta.demo.domain.vo.TestRichTextAssetVo;
import org.namewta.demo.domain.vo.TestRichTextSummaryVo;
import org.namewta.demo.domain.vo.TestRichTextVo;

import java.util.List;

/** 富文本演示服务。 */
public interface ITestRichTextService {
    PageResult<TestRichTextSummaryVo> list(PageQuery pageQuery);
    TestRichTextVo get(Long id);
    TestRichTextVo create(TestRichTextBo bo);
    TestRichTextVo update(Long id, TestRichTextBo bo);
    void remove(Long id, Long version);
    List<TestRichTextAssetVo> assets(String ossIds, Long richTextId);
}
