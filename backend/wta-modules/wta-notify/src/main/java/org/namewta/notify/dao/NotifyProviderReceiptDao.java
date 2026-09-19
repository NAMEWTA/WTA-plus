package org.namewta.notify.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.notify.domain.entity.NotifyProviderReceipt;
import org.namewta.notify.mapper.NotifyProviderReceiptMapper;
import org.springframework.stereotype.Repository;

/** 回执持久化边界，唯一键保留在业务事务内，不依赖进程缓存。 */
@Repository
@RequiredArgsConstructor
public class NotifyProviderReceiptDao {
    private final NotifyProviderReceiptMapper mapper;

    /** 查询已提交且不可变的回执；不存在时返回 null。 */
    public NotifyProviderReceipt find(String eventKey) {
        return mapper.selectOne(new LambdaQueryWrapper<NotifyProviderReceipt>()
            .eq(NotifyProviderReceipt::getEventKey, eventKey));
    }

    /** 唯一键竞争后使用当前读，避免事务早先快照漏掉刚提交的赢家。 */
    public NotifyProviderReceipt lock(String eventKey) {
        return mapper.selectOne(new LambdaQueryWrapper<NotifyProviderReceipt>()
            .eq(NotifyProviderReceipt::getEventKey, eventKey).last("for update"));
    }

    /** 插入回执，唯一键冲突由 Service 按幂等业务规则处理。 */
    public int insert(NotifyProviderReceipt receipt) {
        return mapper.insert(receipt);
    }
}
