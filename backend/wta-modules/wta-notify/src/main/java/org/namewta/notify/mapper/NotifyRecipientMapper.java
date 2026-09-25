package org.namewta.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.namewta.notify.domain.entity.NotifyRecipient;

import java.util.List;

/** 通知接收者 Mapper。 */
@Mapper
public interface NotifyRecipientMapper extends BaseMapper<NotifyRecipient> {
    /** 当前会话内的多值插入；调用方保证非空且已填充审计字段。 */
    int insertBatch(@Param("rows") List<NotifyRecipient> rows);
}
