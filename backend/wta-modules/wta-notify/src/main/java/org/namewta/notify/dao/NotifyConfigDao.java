package org.namewta.notify.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.mapper.NotifyChannelAccountMapper;
import org.namewta.notify.mapper.NotifySceneBindingMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知渠道配置持久化边界。
 */
@Repository
@RequiredArgsConstructor
public class NotifyConfigDao {
    private final NotifyChannelAccountMapper accountMapper;
    private final NotifySceneBindingMapper bindingMapper;

    /**
     * 分页查询渠道账号。
     *
     * @param channel  渠道
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 账号分页
     */
    public PageResult<NotifyChannelAccount> pageAccounts(String channel, Integer pageNum, Integer pageSize) {
        var wrapper = new LambdaQueryWrapper<NotifyChannelAccount>()
            .eq(channel != null && !channel.isBlank(), NotifyChannelAccount::getChannel, channel)
            .orderByDesc(NotifyChannelAccount::getUpdateTime)
            .orderByDesc(NotifyChannelAccount::getAccountId);
        var page = accountMapper.selectPage(new PageQuery(pageSize, pageNum).build(), wrapper);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    public NotifyChannelAccount findAccount(Long accountId) {
        return accountId == null ? null : accountMapper.selectById(accountId);
    }

    /** 配置标识永久保留，包含逻辑删除的账号。 */
    public boolean existsNamespace(String channel, String configKey) {
        return accountMapper.existsNamespace(channel, configKey);
    }

    public NotifyChannelAccount findAccount(String channel, String configKey) {
        return accountMapper.selectOne(new LambdaQueryWrapper<NotifyChannelAccount>()
            .eq(NotifyChannelAccount::getChannel, channel)
            .eq(NotifyChannelAccount::getConfigKey, configKey)
            .last("limit 1"));
    }

    public int insert(NotifyChannelAccount account) {
        return accountMapper.insert(account);
    }

    public int update(NotifyChannelAccount account) {
        return accountMapper.updateById(account);
    }

    public int deleteAccount(Long accountId) {
        return accountMapper.deleteById(accountId);
    }

    public long countBindings(Long accountId) {
        return bindingMapper.selectCount(new LambdaQueryWrapper<NotifySceneBinding>()
            .eq(NotifySceneBinding::getAccountId, accountId));
    }

    public NotifySceneBinding findBinding(String sceneCode, String channel) {
        return bindingMapper.selectOne(new LambdaQueryWrapper<NotifySceneBinding>()
            .eq(NotifySceneBinding::getSceneCode, sceneCode)
            .eq(NotifySceneBinding::getChannel, channel)
            .last("limit 1"));
    }

    /**
     * 列出指定渠道账号。
     *
     * @param channel 渠道
     * @return 账号
     */
    public List<NotifyChannelAccount> listAccounts(String channel) {
        return accountMapper.selectList(new LambdaQueryWrapper<NotifyChannelAccount>()
            .eq(channel != null && !channel.isBlank(), NotifyChannelAccount::getChannel, channel)
            .orderByAsc(NotifyChannelAccount::getConfigKey));
    }

    /**
     * 列出绑定到指定账号的场景。
     *
     * @param accountId 账号
     * @return 绑定
     */
    public List<NotifySceneBinding> listBindingsByAccount(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        return bindingMapper.selectList(new LambdaQueryWrapper<NotifySceneBinding>()
            .eq(NotifySceneBinding::getAccountId, accountId)
            .orderByAsc(NotifySceneBinding::getSceneCode));
    }

    /**
     * 列出全部场景绑定。
     *
     * @return 绑定
     */
    public List<NotifySceneBinding> listBindings() {
        return bindingMapper.selectList(new LambdaQueryWrapper<NotifySceneBinding>()
            .orderByAsc(NotifySceneBinding::getSceneCode)
            .orderByAsc(NotifySceneBinding::getChannel));
    }

    public int insert(NotifySceneBinding binding) {
        return bindingMapper.insert(binding);
    }

    public int update(NotifySceneBinding binding) {
        return bindingMapper.updateById(binding);
    }
}
