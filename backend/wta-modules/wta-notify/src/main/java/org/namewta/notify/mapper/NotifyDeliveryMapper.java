package org.namewta.notify.mapper;

import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.namewta.notify.domain.entity.NotifyDelivery;
import java.time.LocalDateTime;
import java.util.List;

/** 通知投递 Mapper。 */
@Mapper
public interface NotifyDeliveryMapper extends BaseMapperPlus<NotifyDelivery, NotifyDelivery> {
    /** 只选启用的原生短信账号；实际领取仍须DAO执行CAS。 */
    NotifyDelivery selectSmsReceiptCandidate(@Param("now") LocalDateTime now);

    /** 当前会话内的多值插入；调用方保证非空且已填充审计字段。 */
    int insertBatch(@Param("rows") List<NotifyDelivery> rows);
}
