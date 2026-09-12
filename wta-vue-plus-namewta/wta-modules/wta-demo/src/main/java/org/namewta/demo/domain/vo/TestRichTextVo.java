package org.namewta.demo.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import io.github.linpeilie.annotations.ReverseAutoMapping;
import lombok.Data;
import org.namewta.demo.domain.TestRichText;

import java.time.LocalDateTime;

/** 富文本详情视图。 */
@Data
@AutoMapper(target = TestRichText.class)
public class TestRichTextVo {
    private Long richTextId;
    private String title;
    @AutoMapping(target = "contentHtml")
    @ReverseAutoMapping(source = "contentHtml", target = "html")
    private String html;
    private Long version;
    private LocalDateTime updateTime;
}
