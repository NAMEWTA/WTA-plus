package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.vo.NotifyChannelAccountVo;

/**
 * 渠道账号数据访问。
 */
@Mapper
public interface NotifyChannelAccountMapper extends BaseMapperPlus<NotifyChannelAccount, NotifyChannelAccountVo> {
    /** 包含已逻辑删除账号，防止旧回执命名空间被新账号复用。 */
    boolean existsNamespace(@Param("channel") String channel, @Param("configKey") String configKey);
}
