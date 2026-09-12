package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifyMessage;

/**
 * 通知中心站内消息数据访问。
 */
@Mapper
public interface NotifyMessageMapper extends BaseMapperPlus<NotifyMessage, NotifyMessage> {
}
