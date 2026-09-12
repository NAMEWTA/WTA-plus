package org.namewta.notify.service;

import cn.hutool.extra.mail.MailAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mail.notify.MailAccountResolver;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.notify.dao.NotifyConfigDao;
import org.namewta.notify.domain.bo.NotifyChannelAccountBo;
import org.namewta.notify.domain.bo.NotifySceneBindingBo;
import org.namewta.notify.domain.entity.NotifyChannelAccount;
import org.namewta.notify.domain.entity.NotifySceneBinding;
import org.namewta.notify.domain.vo.NotifyChannelAccountVo;
import org.namewta.notify.domain.vo.NotifySceneBindingVo;
import org.namewta.notify.domain.vo.NotifySceneVariableVo;
import org.namewta.notify.port.SmsBlendRegistryPort;
import org.namewta.notify.support.NotifySceneCatalog;
import org.namewta.notify.support.NotifyTemplateRenderer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知渠道配置规则：账号、绑定、变量契约与 SMTP 解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyConfigService implements MailAccountResolver {

    private final NotifyConfigDao dao;
    private final SmsBlendRegistryPort smsBlendRegistry;

    /**
     * 启动时把已启用短信账号注册进 SMS4J。
     *
     * <p>已有库尚未建表时查询会失败。缺表不得阻止进程启动；发送路径仍按无绑定失败关闭。</p>
     */
    @PostConstruct
    public void loadEnabledSmsAccounts() {
        List<NotifyChannelAccount> accounts;
        try {
            accounts = dao.listAccounts("SMS");
        } catch (RuntimeException exception) {
            log.warn("加载短信渠道账号失败，进程继续启动；发送将失败关闭。原因={}", exception.getMessage());
            return;
        }
        for (NotifyChannelAccount account : accounts) {
            registerSms(account);
        }
    }

    /**
     * 分页查询账号。
     *
     * @param channel  渠道
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 账号分页
     */
    public PageResult<NotifyChannelAccountVo> pageAccounts(String channel, Integer pageNum, Integer pageSize) {
        var page = dao.pageAccounts(channel, pageNum, pageSize);
        return PageResult.build(page.getRows().stream().map(this::toAccountVo).toList(), page.getTotal());
    }

    /**
     * 查询账号详情。
     *
     * @param accountId 主键
     * @return 账号
     */
    public NotifyChannelAccountVo getAccount(Long accountId) {
        NotifyChannelAccount account = dao.findAccount(accountId);
        if (account == null) {
            throw new ServiceException("渠道账号不存在");
        }
        return toAccountVo(account);
    }

    /**
     * 新增账号。
     *
     * @param bo 写入参数
     * @return 影响行数
     */
    public int addAccount(NotifyChannelAccountBo bo) {
        validateChannel(bo.getChannel());
        if (dao.findAccount(bo.getChannel(), bo.getConfigKey()) != null) {
            throw new ServiceException("配置标识已存在");
        }
        NotifyChannelAccount entity = toAccountEntity(bo);
        entity.setAccountId(IdGeneratorUtil.nextLongId());
        if (isBlank(entity.getEnabled())) {
            entity.setEnabled("N");
        }
        if (entity.getMinuteMax() == null || entity.getMinuteMax() < 1) {
            throw new ServiceException("账号每分钟上限必须大于 0");
        }
        int rows = dao.insert(entity);
        registerSms(entity);
        return rows;
    }

    /**
     * 更新账号。
     *
     * @param bo 写入参数
     * @return 影响行数
     */
    public int updateAccount(NotifyChannelAccountBo bo) {
        NotifyChannelAccount current = requireAccount(bo.getAccountId());
        validateChannel(bo.getChannel());
        if (!current.getChannel().equals(bo.getChannel()) || !current.getConfigKey().equals(bo.getConfigKey())) {
            NotifyChannelAccount clash = dao.findAccount(bo.getChannel(), bo.getConfigKey());
            if (clash != null && !clash.getAccountId().equals(current.getAccountId())) {
                throw new ServiceException("配置标识已存在");
            }
        }
        NotifyChannelAccount entity = toAccountEntity(bo);
        entity.setAccountId(current.getAccountId());
        if (isBlank(entity.getMailPass())) {
            entity.setMailPass(current.getMailPass());
        }
        if (isBlank(entity.getAccessKeySecret())) {
            entity.setAccessKeySecret(current.getAccessKeySecret());
        }
        if (entity.getMinuteMax() == null || entity.getMinuteMax() < 1) {
            throw new ServiceException("账号每分钟上限必须大于 0");
        }
        int rows = dao.update(entity);
        registerSms(dao.findAccount(entity.getAccountId()));
        return rows;
    }

    /**
     * 启停账号。
     *
     * @param accountId 主键
     * @param enabled   Y/N
     * @return 影响行数
     */
    public int changeStatus(Long accountId, String enabled) {
        if (!"Y".equals(enabled) && !"N".equals(enabled)) {
            throw new ServiceException("启用状态无效");
        }
        NotifyChannelAccount current = requireAccount(accountId);
        current.setEnabled(enabled);
        int rows = dao.update(current);
        registerSms(current);
        return rows;
    }

    /**
     * 删除账号。
     *
     * @param accountId 主键
     * @return 影响行数
     */
    public int removeAccount(Long accountId) {
        NotifyChannelAccount current = requireAccount(accountId);
        if (dao.countBindings(accountId) > 0) {
            throw new ServiceException("账号仍被场景绑定，不能删除");
        }
        int rows = dao.deleteAccount(accountId);
        if ("SMS".equals(current.getChannel())) {
            smsBlendRegistry.remove(current.getConfigKey());
        }
        return rows;
    }

    /**
     * 列出播种场景及绑定。
     *
     * @param channel 渠道
     * @return 场景绑定
     */
    public List<NotifySceneBindingVo> listScenes(String channel) {
        validateChannel(channel);
        List<NotifySceneBindingVo> rows = new ArrayList<>();
        for (String sceneCode : NotifySceneCatalog.sceneCodes()) {
            NotifySceneBinding binding = dao.findBinding(sceneCode, channel);
            rows.add(toSceneVo(sceneCode, channel, binding));
        }
        return rows;
    }

    /**
     * 保存场景绑定。
     *
     * @param bo 绑定参数
     * @return 影响行数
     */
    public int saveBinding(NotifySceneBindingBo bo) {
        if (!NotifySceneCatalog.sceneCodes().contains(bo.getSceneCode())) {
            throw new ServiceException("未知逻辑场景");
        }
        validateChannel(bo.getChannel());
        NotifyChannelAccount account = null;
        int templateMax = bo.getTemplateMinuteMax() == null ? 60 : bo.getTemplateMinuteMax();
        if (bo.getAccountId() != null) {
            account = requireAccount(bo.getAccountId());
            if (!bo.getChannel().equals(account.getChannel())) {
                throw new ServiceException("账号渠道与绑定渠道不一致");
            }
            if (!"Y".equals(account.getEnabled())) {
                throw new ServiceException("只能绑定已启用的渠道账号");
            }
            templateMax = bo.getTemplateMinuteMax() == null ? account.getMinuteMax() : bo.getTemplateMinuteMax();
            if (templateMax > account.getMinuteMax()) {
                throw new ServiceException("模板每分钟上限不能超过账号上限");
            }
        }
        List<String> allowed = NotifySceneCatalog.variables(bo.getSceneCode()).stream()
            .map(NotifySceneCatalog.Variable::name).toList();
        List<String> required = NotifySceneCatalog.requiredNames(bo.getSceneCode());
        if ("MAIL".equals(bo.getChannel())) {
            boolean boundOrFilled = account != null
                || !isBlank(bo.getMailSubject())
                || !isBlank(bo.getMailBody());
            if (boundOrFilled) {
                NotifyTemplateRenderer.validate(nvl(bo.getMailSubject()) + nvl(bo.getMailBody()), allowed, required);
            }
        } else {
            Map<String, String> mapping = bo.getSmsParamMapping() == null ? Map.of() : bo.getSmsParamMapping();
            if (isBlank(bo.getSmsTemplateCode()) && bo.getAccountId() != null) {
                throw new ServiceException("短信供应商模板码不能为空");
            }
            for (String name : required) {
                if (bo.getAccountId() != null && (!mapping.containsKey(name) || isBlank(mapping.get(name)))) {
                    throw new ServiceException("缺少短信变量映射 " + name);
                }
            }
            for (String name : mapping.keySet()) {
                if (!allowed.contains(name)) {
                    throw new ServiceException("不允许映射未声明变量 " + name);
                }
            }
        }
        NotifySceneBinding current = dao.findBinding(bo.getSceneCode(), bo.getChannel());
        NotifySceneBinding entity = new NotifySceneBinding();
        entity.setSceneCode(bo.getSceneCode());
        entity.setChannel(bo.getChannel());
        entity.setAccountId(bo.getAccountId());
        entity.setMailSubject(nvl(bo.getMailSubject()));
        entity.setMailBody(nvl(bo.getMailBody()));
        entity.setSmsTemplateCode(nvl(bo.getSmsTemplateCode()));
        entity.setSmsParamMappingJson(JsonUtils.toJsonString(
            bo.getSmsParamMapping() == null ? Map.of() : bo.getSmsParamMapping()));
        entity.setTemplateMinuteMax(templateMax);
        entity.setRestricted(isBlank(bo.getRestricted()) ? "N" : bo.getRestricted());
        entity.setRecipientMinuteMax(bo.getRecipientMinuteMax() == null ? 0 : bo.getRecipientMinuteMax());
        entity.setRecipientDayMax(bo.getRecipientDayMax() == null ? 0 : bo.getRecipientDayMax());
        if (current == null) {
            entity.setBindingId(IdGeneratorUtil.nextLongId());
            return dao.insert(entity);
        }
        entity.setBindingId(current.getBindingId());
        return dao.update(entity);
    }

    /**
     * 按 configKey 解析 SMTP 账户。
     *
     * @param providerKey 账号标识
     * @return SMTP 账户
     */
    @Override
    public MailAccount resolve(String providerKey) {
        if (isBlank(providerKey)) {
            return null;
        }
        NotifyChannelAccount account = dao.findAccount("MAIL", providerKey);
        if (account == null || !"Y".equals(account.getEnabled())) {
            return null;
        }
        MailAccount mailAccount = new MailAccount();
        mailAccount.setHost(account.getHost());
        mailAccount.setPort(account.getPort());
        mailAccount.setFrom(account.getMailFrom());
        mailAccount.setUser(account.getMailUser());
        mailAccount.setPass(account.getMailPass());
        mailAccount.setAuth(true);
        mailAccount.setSslEnable("Y".equals(account.getSslEnable()));
        mailAccount.setStarttlsEnable("Y".equals(account.getStarttlsEnable()));
        return mailAccount;
    }

    private void registerSms(NotifyChannelAccount account) {
        if (account == null || !"SMS".equals(account.getChannel()) || isBlank(account.getConfigKey())) {
            return;
        }
        if ("Y".equals(account.getEnabled())) {
            smsBlendRegistry.upsert(account);
        } else {
            smsBlendRegistry.remove(account.getConfigKey());
        }
    }

    private NotifyChannelAccount requireAccount(Long accountId) {
        NotifyChannelAccount account = dao.findAccount(accountId);
        if (account == null) {
            throw new ServiceException("渠道账号不存在");
        }
        return account;
    }

    private void validateChannel(String channel) {
        if (!"MAIL".equals(channel) && !"SMS".equals(channel)) {
            throw new ServiceException("渠道仅支持 MAIL 或 SMS");
        }
    }

    private NotifyChannelAccount toAccountEntity(NotifyChannelAccountBo bo) {
        NotifyChannelAccount entity = new NotifyChannelAccount();
        entity.setChannel(bo.getChannel());
        entity.setConfigKey(bo.getConfigKey());
        entity.setEnabled(bo.getEnabled());
        entity.setSupplier(bo.getSupplier());
        entity.setHost(bo.getHost());
        entity.setPort(bo.getPort());
        entity.setMailFrom(bo.getMailFrom());
        entity.setMailUser(bo.getMailUser());
        entity.setMailPass(bo.getMailPass());
        entity.setSslEnable(bo.getSslEnable());
        entity.setStarttlsEnable(bo.getStarttlsEnable());
        entity.setAccessKeyId(bo.getAccessKeyId());
        entity.setAccessKeySecret(bo.getAccessKeySecret());
        entity.setSignature(bo.getSignature());
        entity.setSdkAppId(bo.getSdkAppId());
        entity.setMinuteMax(bo.getMinuteMax());
        entity.setRemark(bo.getRemark());
        return entity;
    }

    private NotifyChannelAccountVo toAccountVo(NotifyChannelAccount entity) {
        NotifyChannelAccountVo vo = new NotifyChannelAccountVo();
        vo.setAccountId(entity.getAccountId());
        vo.setChannel(entity.getChannel());
        vo.setConfigKey(entity.getConfigKey());
        vo.setEnabled(entity.getEnabled());
        vo.setSupplier(entity.getSupplier());
        vo.setHost(entity.getHost());
        vo.setPort(entity.getPort());
        vo.setMailFrom(entity.getMailFrom());
        vo.setMailUser(entity.getMailUser());
        vo.setMailPassSet(!isBlank(entity.getMailPass()));
        vo.setSslEnable(entity.getSslEnable());
        vo.setStarttlsEnable(entity.getStarttlsEnable());
        vo.setAccessKeyId(entity.getAccessKeyId());
        vo.setAccessKeySecretSet(!isBlank(entity.getAccessKeySecret()));
        vo.setSignature(entity.getSignature());
        vo.setSdkAppId(entity.getSdkAppId());
        vo.setMinuteMax(entity.getMinuteMax());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private NotifySceneBindingVo toSceneVo(String sceneCode, String channel, NotifySceneBinding binding) {
        NotifySceneBindingVo vo = new NotifySceneBindingVo();
        vo.setSceneCode(sceneCode);
        vo.setTitle(NotifySceneCatalog.title(sceneCode));
        vo.setVariables(NotifySceneCatalog.variables(sceneCode).stream().map(variable -> {
            NotifySceneVariableVo item = new NotifySceneVariableVo();
            item.setName(variable.name());
            item.setRequired(variable.required());
            item.setExample(variable.example());
            item.setDescription(variable.description());
            return item;
        }).toList());
        vo.setChannel(channel);
        if (binding == null) {
            return vo;
        }
        vo.setBindingId(binding.getBindingId());
        vo.setAccountId(binding.getAccountId());
        if (binding.getAccountId() != null) {
            NotifyChannelAccount account = dao.findAccount(binding.getAccountId());
            if (account != null) {
                vo.setAccountConfigKey(account.getConfigKey());
                vo.setAccountEnabled(account.getEnabled());
            }
        }
        vo.setMailSubject(binding.getMailSubject());
        vo.setMailBody(binding.getMailBody());
        vo.setSmsTemplateCode(binding.getSmsTemplateCode());
        var mapping = JsonUtils.parseMap(binding.getSmsParamMappingJson());
        vo.setSmsParamMapping(mapping == null ? Map.of() : mapping.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, item -> String.valueOf(item.getValue()),
                (left, right) -> left, LinkedHashMap::new)));
        vo.setTemplateMinuteMax(binding.getTemplateMinuteMax());
        vo.setRestricted(binding.getRestricted());
        vo.setRecipientMinuteMax(binding.getRecipientMinuteMax());
        vo.setRecipientDayMax(binding.getRecipientDayMax());
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
