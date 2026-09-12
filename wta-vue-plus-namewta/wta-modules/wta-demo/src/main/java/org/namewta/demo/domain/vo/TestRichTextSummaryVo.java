package org.namewta.demo.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.namewta.demo.domain.TestRichText;

import java.time.LocalDateTime;

/** 富文本列表摘要。 */
@Data
@AutoMapper(target = TestRichText.class)
public class TestRichTextSummaryVo {
    private Long richTextId;
    private String title;
    private Long version;
    private LocalDateTime updateTime;
}
