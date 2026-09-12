package org.namewta.demo.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 富文本保存请求。 */
@Data
public class TestRichTextBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "标题不能为空")
    private String title;
    @NotNull(message = "内容不能为空")
    private String html;
    private Long version;
}
