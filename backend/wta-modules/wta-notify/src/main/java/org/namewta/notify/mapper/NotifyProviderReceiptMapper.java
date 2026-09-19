package org.namewta.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.namewta.common.mybatis.core.mapper.BaseMapperPlus;
import org.namewta.notify.domain.entity.NotifyProviderReceipt;

/** 回执幂等凭据映射器，仅供 DAO 使用。 */
@Mapper
public interface NotifyProviderReceiptMapper extends BaseMapperPlus<NotifyProviderReceipt, NotifyProviderReceipt> {
}
