package org.namewta.demo.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 富文本乐观锁版本请求。 */
@Data
public class TestRichTextVersionBo {
    @NotNull(message = "版本不能为空")
    private Long version;
}
