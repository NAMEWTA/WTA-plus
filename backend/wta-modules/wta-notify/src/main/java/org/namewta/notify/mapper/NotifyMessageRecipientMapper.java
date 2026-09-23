package org.namewta.notify.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifyMessageRecipient;

/**
 * 通知中心收件关系数据访问。
 */
@Mapper
public interface NotifyMessageRecipientMapper extends BaseMapperPlus<NotifyMessageRecipient, NotifyMessageRecipient>,
    MPJBaseMapper<NotifyMessageRecipient> {
}
