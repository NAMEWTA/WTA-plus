package org.namewta.demo.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.namewta.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/** 富文本演示对象 test_rich_text。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_rich_text")
public class TestRichText extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("rich_text_id")
    private Long richTextId;
    private Long clientPk;
    private String title;
    private String contentHtml;
    @Version
    private Long version;
    @TableLogic
    private Long delFlag;
}
