package org.namewta.oidc.service;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.json.utils.JsonUtils;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.oidc.config.OidcProperties;
import org.namewta.oidc.dao.OidcApplicationDao;
import org.namewta.oidc.domain.OidcApplication;
import org.namewta.oidc.domain.bo.OidcApplicationBo;
import org.namewta.oidc.domain.vo.*;
import org.namewta.oidc.support.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

/** 应用登记规则与凭据生命周期，不调用第一方 Client 准入。 */
@Service
@RequiredArgsConstructor
public class OidcApplicationService {
    private final OidcApplicationDao dao;
    private final OidcProperties properties;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /** 在调用方事务内锁定应用当前策略，防止撤销后重新插入旧授权。 */
    public OidcApplication lockClient(String clientId) {
        return dao.lockClient(clientId);
    }

    /** 读取标识对应的当前持久记录，不存在时返回空。 */
    public OidcApplication find(Long id) {
        return id == null ? null : dao.find(id);
    }

    /** 按管理主键读取应用，缺失或已逻辑删除时报告应用不存在。 */
    public OidcApplication require(Long id) {
        var app = dao.find(id);
        if (app == null) throw new ServiceException("OIDC应用不存在");
        return app;
    }

    /** 读取独立 OIDC 客户端登记，不进行第一方 Client 准入。 */
    public OidcApplication client(String id) {
        return id == null ? null : dao.client(id);
    }

    /** 解析持久配置中的字符串列表，缺失值视为空列表。 */
    public List<String> values(String json) {
        return json == null ? List.of() : JsonUtils.parseArray(json, String.class);
    }

    /** 构造不包含密钥和摘要的管理响应。 */
    public OidcApplicationVo vo(OidcApplication app) {
        var allow = values(app.getAllowedFieldsJson());
        return new OidcApplicationVo(
                app.getApplicationId().toString(),
                app.getName(),
                app.getClientId(),
                values(app.getRedirectUrisJson()),
                values(app.getPostLogoutRedirectUrisJson()),
                allow,
                List.copyOf(org.namewta.oidc.support.OidcFieldCatalog.scopes(allow)),
                app.getClientAuthenticationMethod(),
                Boolean.TRUE.equals(app.getPkceRequired()),
                Boolean.TRUE.equals(app.getEnabled()),
                app.getVersion(),
                app.getCreateTime());
    }

    /** 按确定顺序分页查询，限制单页记录数量。 */
    public PageResult<OidcApplicationVo> page(String name, int page, int size) {
        var data = dao.page(name, page, size);
        return PageResult.build(data.getRows().stream().map(this::vo).toList(), data.getTotal());
    }

    /** 创建独立应用及随机客户端凭据，持久化只保存 BCrypt 摘要，明文仅本次返回。 */
    public OidcApplicationSecretVo create(OidcApplicationBo bo) {
        var app = new OidcApplication();
        apply(app, bo);
        app.setApplicationId(IdGeneratorUtil.nextLongId());
        app.setClientId("wta_" + OidcSecrets.random());
        app.setEnabled(true);
        app.setVersion(0);
        app.setDelFlag("0");
        var secret = OidcSecrets.random();
        app.setClientSecretHash("{bcrypt}" + encoder.encode(secret));
        changed(dao.insert(app));
        return new OidcApplicationSecretVo(vo(app), secret);
    }

    /** 校验输入和乐观版本后更新允许编辑的字段。 */
    public OidcApplicationVo update(OidcApplicationBo bo) {
        var app = require(bo.getApplicationId());
        version(app, bo.getVersion());
        apply(app, bo);
        changed(dao.update(app));
        return vo(require(app.getApplicationId()));
    }

    /** 按乐观版本保存启用状态；关联授权撤销由外层用例在同一事务编排。 */
    public OidcApplicationVo status(Long id, Integer version, boolean enabled) {
        var app = require(id);
        version(app, version);
        app.setEnabled(enabled);
        changed(dao.update(app));
        return vo(require(id));
    }

    /** 替换客户端密钥摘要并仅返回一次新明文；外层用例负责同步撤销授权。 */
    public OidcApplicationSecretVo rotate(Long id, Integer version) {
        var app = require(id);
        version(app, version);
        String secret = OidcSecrets.random();
        app.setClientSecretHash("{bcrypt}" + encoder.encode(secret));
        changed(dao.update(app));
        return new OidcApplicationSecretVo(vo(require(id)), secret);
    }

    /** 按乐观版本逻辑删除应用；外层用例在同一事务终止关联授权。 */
    public void delete(Long id, Integer version) {
        var app = require(id);
        version(app, version);
        changed(dao.delete(app));
    }

    /** 集中校验回调、认证方式和字段白名单，再写入允许编辑的应用属性。 */
    private void apply(OidcApplication app, OidcApplicationBo bo) {
        if (bo.getName() == null || bo.getName().isBlank() || bo.getName().length() > 128)
            throw new ServiceException("请输入有效应用名称");
        if (!Set.of("client_secret_basic", "client_secret_post")
                .contains(bo.getClientAuthenticationMethod()))
            throw new ServiceException("不支持的客户端认证方式");
        var allow =
                bo.getAllowedFields() == null
                        ? org.namewta.oidc.support.OidcFieldCatalog.defaults()
                        : new LinkedHashSet<>(bo.getAllowedFields());
        var known =
                org.namewta.oidc.support.OidcFieldCatalog.fields().stream()
                        .map(OidcFieldVo::key)
                        .toList();
        if (!known.containsAll(allow)) throw new ServiceException("存在未知身份字段");
        app.setName(bo.getName().strip());
        app.setClientAuthenticationMethod(bo.getClientAuthenticationMethod());
        app.setPkceRequired(!Boolean.FALSE.equals(bo.getPkceRequired()));
        app.setRedirectUrisJson(
                JsonUtils.toJsonString(
                        OidcUriPolicy.addresses(
                                bo.getRedirectUris(), true, properties.isAllowHttp())));
        app.setPostLogoutRedirectUrisJson(
                JsonUtils.toJsonString(
                        OidcUriPolicy.addresses(
                                bo.getPostLogoutRedirectUris() == null
                                        ? List.of()
                                        : bo.getPostLogoutRedirectUris(),
                                false,
                                properties.isAllowHttp())));
        app.setAllowedFieldsJson(JsonUtils.toJsonString(allow));
    }

    /** 拒绝缺少版本或基于旧页面的更新，防止覆盖其他管理员调整。 */
    private void version(OidcApplication app, Integer version) {
        if (version == null || !version.equals(app.getVersion()))
            throw new ServiceException("记录已变更，请刷新后重试");
    }

    /** 持久化未影响唯一预期行时按并发冲突失败，不宣称修改成功。 */
    private void changed(int n) {
        if (n != 1) throw new ServiceException("记录已变更，请刷新后重试");
    }
}
