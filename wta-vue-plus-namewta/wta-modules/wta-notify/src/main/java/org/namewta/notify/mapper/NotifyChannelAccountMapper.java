package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.vo.NotifyChannelAccountVo;

/**
 * 渠道账号数据访问。
 */
@Mapper
public interface NotifyChannelAccountMapper extends BaseMapperPlus<NotifyChannelAccount, NotifyChannelAccountVo> {
}
