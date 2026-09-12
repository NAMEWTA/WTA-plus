package org.namewta.demo.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.demo.domain.TestRichText;
import org.namewta.demo.domain.vo.TestRichTextSummaryVo;
import org.namewta.demo.domain.vo.TestRichTextVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 证明非空 Entity 列表可经 {@link MapstructUtils#convert} 转为 SummaryVo，
 * 覆盖 {@code BaseMapperPlus.selectVoPage} 在结果非空时的同一转换接缝。
 */
@Tag("dev")
@Tag("local")
class TestRichTextListAutoMapperTest {

    @BeforeAll
    static void configureMapstructConverter() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("converter", new Converter());
        new SpringUtil().postProcessBeanFactory(beanFactory);
    }

    @Test
    void convertsNonEmptyEntityListToSummaryVo() {
        TestRichText entity = sampleEntity();

        List<TestRichTextSummaryVo> records = MapstructUtils.convert(List.of(entity), TestRichTextSummaryVo.class);

        assertNotNull(records);
        assertEquals(1, records.size());
        TestRichTextSummaryVo summary = records.get(0);
        assertEquals(41L, summary.getRichTextId());
        assertEquals("list-automapper", summary.getTitle());
        assertEquals(1L, summary.getVersion());
        assertEquals(entity.getUpdateTime(), summary.getUpdateTime());
    }

    @Test
    void convertsEntityToVoPreservingHtml() {
        TestRichText entity = sampleEntity();

        TestRichTextVo vo = MapstructUtils.convert(entity, TestRichTextVo.class);

        assertNotNull(vo);
        assertEquals(41L, vo.getRichTextId());
        assertEquals("list-automapper", vo.getTitle());
        assertEquals("<p>body</p>", vo.getHtml());
        assertEquals(1L, vo.getVersion());
        assertEquals(entity.getUpdateTime(), vo.getUpdateTime());
    }

    private static TestRichText sampleEntity() {
        TestRichText entity = new TestRichText();
        entity.setRichTextId(41L);
        entity.setTitle("list-automapper");
        entity.setContentHtml("<p>body</p>");
        entity.setVersion(1L);
        entity.setUpdateTime(LocalDateTime.of(2026, 9, 12, 0, 31));
        return entity;
    }
}
