package org.namewta.notify.service;

import lombok.RequiredArgsConstructor;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.support.NotifySceneCatalog;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账号级和模板级测试发送，走与生产相同的提交路径。
 */
@Service
@RequiredArgsConstructor
public class NotifyTestSendService {
    private final NotifyConfigDao dao;
    private final NotificationApplicationService notifications;

    /**
     * 按账号试发。账号必须启用且已绑定场景。
     *
     * @param accountId 账号
     * @param sceneCode 可选场景，缺省取该账号第一个绑定
     * @param target    收件人
     * @return 提交状态
     */
    public String sendAccount(Long accountId, String sceneCode, String target) {
        NotifyChannelAccount account = dao.findAccount(accountId);
        if (account == null) {
            throw new ServiceException("渠道账号不存在");
        }
        if (!"Y".equals(account.getEnabled())) {
            throw new ServiceException("渠道账号未启用");
        }
        requireTarget(target);
        List<NotifySceneBinding> bindings = dao.listBindingsByAccount(accountId);
        NotifySceneBinding binding = sceneCode == null || sceneCode.isBlank()
            ? bindings.stream().findFirst().orElse(null)
            : bindings.stream().filter(item -> sceneCode.equals(item.getSceneCode())).findFirst().orElse(null);
        if (binding == null) {
            throw new ServiceException("账号未绑定逻辑场景");
        }
        return submit(binding.getSceneCode(), account.getChannel(), target);
    }

    /**
     * 按场景模板试发。
     *
     * @param sceneCode 场景
     * @param channel   渠道
     * @param target    收件人
     * @return 提交状态
     */
    public String sendTemplate(String sceneCode, String channel, String target) {
        requireTarget(target);
        NotifySceneBinding binding = dao.findBinding(sceneCode, channel);
        if (binding == null || binding.getAccountId() == null) {
            throw new ServiceException("场景未绑定渠道账号");
        }
        NotifyChannelAccount account = dao.findAccount(binding.getAccountId());
        if (account == null || !"Y".equals(account.getEnabled())) {
            throw new ServiceException("渠道账号未启用或不存在");
        }
        return submit(sceneCode, channel, target);
    }

    private String submit(String sceneCode, String channel, String target) {
        String recipientType = "MAIL".equals(channel) ? "EMAIL" : "PHONE";
        NotificationChannel notifyChannel = "MAIL".equals(channel) ? NotificationChannel.MAIL : NotificationChannel.SMS;
        Map<String, Object> params = new LinkedHashMap<>();
        NotifySceneCatalog.examples(sceneCode).forEach(params::put);
        NotificationReceipt receipt = notifications.submit(new NotificationCommand(
            "notify", sceneCode, "NOTIFY_CONFIG_TEST", target, recipientType, List.of(target),
            sceneCode, params, List.of(notifyChannel), NotificationStrategy.ALL, NotificationMode.SYNC,
            20, null, null,
            "notify-config-test:" + sceneCode + ":" + channel + ":" + System.currentTimeMillis(),
            Map.of("audit", "TEST")));
        return receipt == null || receipt.status() == null ? "UNKNOWN" : receipt.status().name();
    }

    private void requireTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new ServiceException("测试收件人不能为空");
        }
    }
}
