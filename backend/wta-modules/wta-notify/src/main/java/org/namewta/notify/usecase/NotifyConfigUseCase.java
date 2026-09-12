package org.namewta.notify.usecase;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.PageResult;
import org.namewta.notify.domain.bo.NotifyChannelAccountBo;
import org.namewta.notify.domain.bo.NotifySceneBindingBo;
import org.namewta.notify.domain.vo.NotifyChannelAccountVo;
import org.namewta.notify.domain.vo.NotifySceneBindingVo;
import org.namewta.notify.service.NotifyConfigService;
import org.namewta.notify.service.NotifyTestSendService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知渠道配置用例。
 */
@Service
@RequiredArgsConstructor
public class NotifyConfigUseCase {
    private final NotifyConfigService configService;
    private final NotifyTestSendService testSendService;

    /**
     * 分页查询账号。
     *
     * @param channel  渠道
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 分页
     */
    public PageResult<NotifyChannelAccountVo> pageAccounts(String channel, Integer pageNum, Integer pageSize) {
        return configService.pageAccounts(channel, pageNum, pageSize);
    }

    /**
     * 账号详情。
     *
     * @param accountId 主键
     * @return 账号
     */
    public NotifyChannelAccountVo getAccount(Long accountId) {
        return configService.getAccount(accountId);
    }

    /**
     * 新增账号。
     *
     * @param bo 参数
     * @return 行数
     */
    @DSTransactional
    public int addAccount(NotifyChannelAccountBo bo) {
        return configService.addAccount(bo);
    }

    /**
     * 更新账号。
     *
     * @param bo 参数
     * @return 行数
     */
    @DSTransactional
    public int updateAccount(NotifyChannelAccountBo bo) {
        return configService.updateAccount(bo);
    }

    /**
     * 启停账号。
     *
     * @param accountId 主键
     * @param enabled   状态
     * @return 行数
     */
    @DSTransactional
    public int changeStatus(Long accountId, String enabled) {
        return configService.changeStatus(accountId, enabled);
    }

    /**
     * 删除账号。
     *
     * @param accountId 主键
     * @return 行数
     */
    @DSTransactional
    public int removeAccount(Long accountId) {
        return configService.removeAccount(accountId);
    }

    /**
     * 场景列表。
     *
     * @param channel 渠道
     * @return 场景绑定
     */
    public List<NotifySceneBindingVo> listScenes(String channel) {
        return configService.listScenes(channel);
    }

    /**
     * 保存绑定。
     *
     * @param bo 参数
     * @return 行数
     */
    @DSTransactional
    public int saveBinding(NotifySceneBindingBo bo) {
        return configService.saveBinding(bo);
    }

    /**
     * 账号级测试发送。
     *
     * @param accountId 账号
     * @param sceneCode 场景
     * @param target    收件人
     * @return 结果说明
     */
    @DSTransactional
    public String testAccount(Long accountId, String sceneCode, String target) {
        return testSendService.sendAccount(accountId, sceneCode, target);
    }

    /**
     * 模板级测试发送。
     *
     * @param sceneCode 场景
     * @param channel   渠道
     * @param target    收件人
     * @return 结果说明
     */
    @DSTransactional
    public String testTemplate(String sceneCode, String channel, String target) {
        return testSendService.sendTemplate(sceneCode, channel, target);
    }
}
