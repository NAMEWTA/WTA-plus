package org.namewta.sso.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.redis.utils.RedisUtils;
import org.namewta.sso.api.SsoRuntimeSettings;
import org.namewta.sso.dao.SsoServiceConfigDao;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;

/** 数据库决定版本，Redis按版本缓存；结构快照整个进程不变。 */
@Service
@RequiredArgsConstructor
public class SsoConfigurationService {
    private final SsoServiceConfigDao dao;
    private volatile SsoRuntimeSettings structure;

    /** 首次构造运行配置时冻结结构，后续只读取动态参数。 */
    public synchronized SsoRuntimeSettings current() {
        var saved = saved();
        if (structure == null) structure = saved;
        return new SsoRuntimeSettings(
                saved.enabled()
                        && structure.webOrigin().equals(saved.webOrigin())
                        && structure.webBasePath().equals(saved.webBasePath())
                        && structure.cookieName().equals(saved.cookieName())
                        && structure.cookieSecure() == saved.cookieSecure(),
                structure.webOrigin(),
                structure.webBasePath(),
                structure.cookieName(),
                structure.cookieSecure(),
                saved.codeTtlSeconds(),
                saved.sessionTtlSeconds());
    }

    /** 以数据库版本定位缓存，缓存写入失败不改变持久事实。 */
    public SsoRuntimeSettings saved() {
        var row = dao.find();
        if (row == null || row.getSettingsJson() == null) return SsoRuntimeSettings.defaults();
        String key = "sso:configuration:" + row.getVersion();
        String json = RedisUtils.getCacheObject(key);
        if (!java.util.Objects.equals(json, row.getSettingsJson())) {
            json = row.getSettingsJson();
            RedisUtils.setCacheObject(key, json, Duration.ofMinutes(10));
        }
        return JsonUtils.parseObject(json, SsoRuntimeSettings.class);
    }

    /** 校验管理输入；新配置不修改本进程的Cookie和Origin身份。 */
    public void save(SsoRuntimeSettings value) {
        if (value == null
                || value.webOrigin() == null
                || value.codeTtlSeconds() < 30
                || value.codeTtlSeconds() > 3600
                || value.sessionTtlSeconds() < 60
                || value.sessionTtlSeconds() > 2592000
                || value.cookieName() == null
                || !value.cookieName().matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                || value.webBasePath() == null
                || !value.webBasePath().matches("/(?:[A-Za-z0-9][A-Za-z0-9-]*/)?"))
            throw new ServiceException("中央认证配置无效");
        if (value.enabled() || !value.webOrigin().isBlank()) {
            URI u = URI.create(value.webOrigin());
            if (u.getHost() == null
                    || !("https".equals(u.getScheme()) || "http".equals(u.getScheme()))
                    || u.getRawUserInfo() != null
                    || u.getRawQuery() != null
                    || u.getRawFragment() != null
                    || u.getRawPath() != null && !u.getRawPath().isEmpty())
                throw new ServiceException("SSO Origin必须为精确来源");
        }
        var row = dao.lock();
        if (row == null) throw new ServiceException("请先安装认证配置基座");
        var old = JsonUtils.parseObject(row.getSettingsJson(), SsoRuntimeSettings.class);
        boolean change =
                !old.webOrigin().equals(value.webOrigin())
                        || !old.webBasePath().equals(value.webBasePath())
                        || !old.cookieName().equals(value.cookieName())
                        || old.cookieSecure() != value.cookieSecure();
        if (change && (old.enabled() || value.enabled()))
            throw new ServiceException("请先停用中央认证，再保存结构配置并维护重启");
        var running = current();
        if (value.enabled()
                && (!running.webOrigin().equals(value.webOrigin())
                        || !running.webBasePath().equals(value.webBasePath())
                        || !running.cookieName().equals(value.cookieName())
                        || running.cookieSecure() != value.cookieSecure()))
            throw new ServiceException("中央认证结构配置待生效，请维护重启");
        row.setSettingsJson(JsonUtils.toJsonString(value));
        if (dao.update(row) != 1) throw new ServiceException("配置已变更，请刷新");
    }
}
