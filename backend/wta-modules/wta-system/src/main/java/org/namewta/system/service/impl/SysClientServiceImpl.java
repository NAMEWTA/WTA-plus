package org.namewta.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.constant.CacheNames;
import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.domain.PageResult;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.MapstructUtils;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.mybatis.core.page.PageQuery;
import org.namewta.common.mybatis.core.query.QueryBuilder;
import org.namewta.common.openapi.session.OpenApiMachineSessionInvalidator;
import org.namewta.system.domain.SysClient;
import org.namewta.system.domain.SysRole;
import org.namewta.system.domain.SysUserTypeRel;
import org.namewta.system.domain.bo.SysClientBo;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserTypeVo;
import org.namewta.system.mapper.SysClientMapper;
import org.namewta.system.mapper.SysRoleMapper;
import org.namewta.system.mapper.SysUserTypeRelMapper;
import org.namewta.system.service.ClientSessionService;
import org.namewta.system.service.ISysClientService;
import org.namewta.system.service.ISysUserTypeService;
import org.namewta.system.sso.SsoClientFieldsSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * 客户端管理Service业务层处理
 *
 * @author Michelle.Chung
 * @date 2023-06-18
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysClientServiceImpl implements ISysClientService {

    private static final String CLIENT_RULE_SEPARATOR_REGEX = "[,;\\r\\n]+";

    private final SysClientMapper clientMapper;
    private final SysRoleMapper roleMapper;
    private final ISysUserTypeService userTypeService;
    private final ClientSessionService clientSessionService;
    private final SysUserTypeRelMapper userTypeRelMapper;
    private OpenApiMachineSessionInvalidator openApiSessionInvalidator = ignored -> 0;

    @Autowired(required = false)
    void setOpenApiSessionInvalidator(OpenApiMachineSessionInvalidator openApiSessionInvalidator) {
        this.openApiSessionInvalidator = Objects.requireNonNull(openApiSessionInvalidator);
    }

    /**
     * 查询客户端管理
     *
     * @param id 主键
     * @return 客户端详情
     */
    @Override
    public SysClientVo queryById(Long id) {
        SysClientVo vo = clientMapper.selectVoById(id);
        fillClientRuleFields(vo);
        return vo;
    }

    /**
     * 查询客户端管理
     *
     * @param clientId 客户端标识
     * @return 客户端详情
     */
    @Cacheable(cacheNames = CacheNames.SYS_CLIENT, key = "#clientId")
    @Override
    public SysClientVo queryByClientId(String clientId) {
        SysClientVo vo = clientMapper.lambda().eq(SysClient::getClientId, clientId).voOne();
        fillClientRuleFields(vo);
        return vo;
    }

    /**
     * 查询客户端管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 客户端分页列表
     */
    @Override
    public PageResult<SysClientVo> queryPageList(SysClientBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysClient> lqw = buildQueryWrapper(bo);
        Page<SysClientVo> result = clientMapper.selectVoPage(pageQuery.build(), lqw);
        result.getRecords().forEach(this::fillClientRuleFields);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询客户端管理列表
     *
     * @param bo 查询条件
     * @return 客户端列表
     */
    @Override
    public List<SysClientVo> queryList(SysClientBo bo) {
        LambdaQueryWrapper<SysClient> lqw = buildQueryWrapper(bo);
        List<SysClientVo> list = clientMapper.selectVoList(lqw);
        list.forEach(this::fillClientRuleFields);
        return list;
    }

    /**
     * 构造客户端列表查询条件。
     *
     * @param bo 客户端筛选条件
     * @return 包含 clientId、clientKey、状态等条件的查询包装器
     */
    private LambdaQueryWrapper<SysClient> buildQueryWrapper(SysClientBo bo) {
        return QueryBuilder.lambda(SysClient.class)
            .eqIfText(SysClient::getClientId, bo.getClientId())
            .eqIfText(SysClient::getClientKey, bo.getClientKey())
            .eqIfText(SysClient::getClientSecret, bo.getClientSecret())
            .eqIfPresent(SysClient::getUserTypeId, bo.getUserTypeId())
            .eqIfText(SysClient::getStatus, bo.getStatus())
            .orderByAsc(SysClient::getId)
            .build();
    }

    /**
     * 新增客户端管理
     *
     * @param bo 客户端业务对象
     * @return 新增成功返回 {@code true}
     */
    @Override
    @DSTransactional
    public Boolean insertByBo(SysClientBo bo) {
        SsoClientFieldsSupport.validate(bo);
        SysClient add = MapstructUtils.convert(bo, SysClient.class);
        validClientPolicy(add, true);
        if (ObjectUtil.isNull(add.getRegisterEnabled())) {
            add.setRegisterEnabled(Boolean.FALSE);
        }
        add.setGrantType(CollUtil.join(bo.getGrantTypeList(), StringUtils.SEPARATOR));
        add.setAccessPath(resolveRuleValue(bo.getAccessPath(), bo.getAccessPathList(), this::normalizeAccessPath));
        add.setIpWhitelist(resolveRuleValue(bo.getIpWhitelist(), bo.getIpWhitelistList(), UnaryOperator.identity()));
        String ssoSecretOnce = SsoClientFieldsSupport.apply(bo, add, null);
        // 生成clientid
        String clientKey = bo.getClientKey();
        String clientSecret = bo.getClientSecret();
        add.setClientId(SecureUtil.md5(clientKey + clientSecret));
        boolean flag = clientMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            bo.setSsoSecretOnce(ssoSecretOnce);
            invalidateUsersForUserTypes(List.of(add.getUserTypeId()));
        }
        return flag;
    }

    /**
     * 修改客户端管理
     *
     * @param bo 客户端业务对象
     * @return 修改成功返回 {@code true}
     */
    @CacheEvict(cacheNames = CacheNames.SYS_CLIENT, key = "#bo.clientId")
    @Override
    @DSTransactional
    public Boolean updateByBo(SysClientBo bo) {
        SsoClientFieldsSupport.validate(bo);
        SysClient db = clientMapper.selectById(bo.getId());
        SysClient update = MapstructUtils.convert(bo, SysClient.class);
        validClientPolicy(update, false);
        if (ObjectUtil.isNull(update.getRegisterEnabled())) {
            update.setRegisterEnabled(Boolean.FALSE);
        }
        update.setGrantType(StringUtils.joinComma(bo.getGrantTypeList()));
        update.setAccessPath(resolveRuleValue(bo.getAccessPath(), bo.getAccessPathList(), this::normalizeAccessPath));
        update.setIpWhitelist(resolveRuleValue(bo.getIpWhitelist(), bo.getIpWhitelistList(), UnaryOperator.identity()));
        String existingHash = ObjectUtil.isNull(db) ? null : db.getSsoSecretHash();
        String ssoSecretOnce = SsoClientFieldsSupport.apply(bo, update, existingHash);
        boolean flag = clientMapper.updateById(update) > 0;
        if (flag) {
            bo.setSsoSecretOnce(ssoSecretOnce);
        }
        if (flag && shouldKickClientSessions(db, update)) {
            clientSessionService.kickoutClient(db.getId());
        }
        if (flag && shouldInvalidateOpenApiSessions(db, update)) {
            invalidateUsersForUserTypes(List.of(db.getUserTypeId(), update.getUserTypeId()));
        }
        return flag;
    }

    /**
     * 修改状态
     *
     * @param clientId 客户端标识
     * @param status   状态值
     * @return 更新条数
     */
    @CacheEvict(cacheNames = CacheNames.SYS_CLIENT, key = "#clientId")
    @Override
    @DSTransactional
    public int updateClientStatus(String clientId, String status) {
        SysClientVo client = clientMapper.lambda().eq(SysClient::getClientId, clientId).voOne();
        int rows = clientMapper.lambda()
            .set(SysClient::getStatus, status)
            .eq(SysClient::getClientId, clientId)
            .updateCount();
        if (rows > 0 && SystemConstants.DISABLE.equals(status) && ObjectUtil.isNotNull(client)) {
            clientSessionService.kickoutClient(client.getId());
        }
        if (rows > 0 && ObjectUtil.isNotNull(client) && !ObjectUtil.equal(client.getStatus(), status)) {
            invalidateUsersForUserTypes(List.of(client.getUserTypeId()));
        }
        return rows;
    }

    /**
     * 批量删除客户端管理
     *
     * @param ids     主键集合
     * @param isValid 是否执行业务校验
     * @return 删除成功返回 {@code true}
     */
    @CacheEvict(cacheNames = CacheNames.SYS_CLIENT, allEntries = true)
    @Override
    @DSTransactional
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<SysClient> clients = clientMapper.selectByIds(ids);
        boolean deleted = clientMapper.deleteByIds(ids) > 0;
        if (deleted) {
            invalidateUsersForUserTypes(clients.stream().map(SysClient::getUserTypeId).toList());
        }
        return deleted;
    }

    /**
     * 轮换 SSO 密钥，明文只返回一次。
     *
     * @param id 客户端主键
     * @return 含一次性明文的视图
     */
    @Override
    @DSTransactional
    public SysClientVo rotateSsoSecret(Long id) {
        SysClient db = clientMapper.selectById(id);
        if (ObjectUtil.isNull(db)) {
            throw new ServiceException("客户端不存在");
        }
        String issued = SsoClientFieldsSupport.rotate(db);
        clientMapper.updateById(db);
        SysClientVo vo = queryById(id);
        vo.setSsoSecretOnce(issued);
        return vo;
    }

    /**
     * 校验客户端key是否唯一
     *
     * @param client 客户端信息
     * @return 结果
     */
    @Override
    public boolean checkClickKeyUnique(SysClientBo client) {
        boolean exist = clientMapper.lambda()
            .eq(SysClient::getClientKey, client.getClientKey())
            .neIfPresent(SysClient::getId, client.getId())
            .exists();
        return !exist;
    }

    /**
     * 回填客户端扩展规则字段，便于前端直接展示和编辑。
     *
     * @param vo 客户端视图对象
     */
    private void fillClientRuleFields(SysClientVo vo) {
        if (ObjectUtil.isNull(vo)) {
            return;
        }
        vo.setGrantTypeList(StringUtils.splitList(vo.getGrantType()));
        vo.setAccessPathList(parseRuleList(vo.getAccessPath(), this::normalizeAccessPath));
        vo.setIpWhitelistList(parseRuleList(vo.getIpWhitelist(), UnaryOperator.identity()));
        boolean hashed = StringUtils.isNotBlank(vo.getSsoSecretHash());
        SsoClientFieldsSupport.fillView(vo, hashed);
        fillUserTypeAndDefaultRole(vo);
    }

    /**
     * 统一处理白名单与路径规则的入库格式。
     *
     * @param rawValue   原始字符串
     * @param listValue  列表值
     * @param normalizer 单条规则归一化器
     * @return 逗号拼接后的规则串
     */
    private String resolveRuleValue(String rawValue, List<String> listValue, UnaryOperator<String> normalizer) {
        List<String> rules = rawValue != null
            ? StringUtils.str2List(rawValue, CLIENT_RULE_SEPARATOR_REGEX, true, true)
            : listValue;
        if (CollUtil.isEmpty(rules)) {
            return listValue != null || rawValue != null ? "" : null;
        }
        return CollUtil.join(rules.stream()
            .map(normalizer)
            .filter(StringUtils::isNotBlank)
            .toList(), StringUtils.SEPARATOR);
    }

    /**
     * 将规则串转换为列表。
     *
     * @param value      规则串
     * @param normalizer 单条规则归一化器
     * @return 规则列表
     */
    private List<String> parseRuleList(String value, UnaryOperator<String> normalizer) {
        return StringUtils.str2List(value, CLIENT_RULE_SEPARATOR_REGEX, true, true).stream()
            .map(normalizer)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    /**
     * 统一补齐路径前导斜杠，避免配置成 app/** 时无法命中。
     *
     * @param path 路径规则
     * @return 规范化后的路径规则
     */
    private String normalizeAccessPath(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String accessPath = StringUtils.trim(path);
        if (StringUtils.isBlank(accessPath)) {
            return null;
        }
        if (StringUtils.equals(accessPath, "*") || StringUtils.equals(accessPath, "/**")) {
            return "/**";
        }
        return accessPath.startsWith(StringUtils.SLASH) ? accessPath : StringUtils.SLASH + accessPath;
    }

    /**
     * 回填登录域与默认角色展示字段。
     *
     * @param vo 客户端视图对象
     */
    private void fillUserTypeAndDefaultRole(SysClientVo vo) {
        if (ObjectUtil.isNotNull(vo.getUserTypeId())) {
            SysUserTypeVo userType = userTypeService.queryById(vo.getUserTypeId());
            if (ObjectUtil.isNotNull(userType)) {
                vo.setUserTypeCode(userType.getUserTypeCode());
                vo.setUserTypeName(userType.getUserTypeName());
            }
        }
        if (ObjectUtil.isNotNull(vo.getDefaultRoleId())) {
            SysRole role = roleMapper.selectById(vo.getDefaultRoleId());
            if (ObjectUtil.isNotNull(role)) {
                vo.setDefaultRoleName(role.getRoleName());
            }
        }
    }

    /**
     * 校验登录域与默认角色策略。
     *
     * @param client 客户端实体
     * @param isAdd  是否新增
     */
    private void validClientPolicy(SysClient client, boolean isAdd) {
        if (ObjectUtil.isNull(client.getUserTypeId())) {
            throw new ServiceException("登录域不能为空");
        }
        SysUserTypeVo userType = userTypeService.queryById(client.getUserTypeId());
        if (ObjectUtil.isNull(userType)) {
            throw new ServiceException("登录域不存在");
        }
        if (!SystemConstants.NORMAL.equals(userType.getStatus())) {
            throw new ServiceException("登录域已停用");
        }
        if (ObjectUtil.isNull(client.getDefaultRoleId())) {
            return;
        }
        SysRole role = roleMapper.selectById(client.getDefaultRoleId());
        if (ObjectUtil.isNull(role)) {
            throw new ServiceException("默认角色不存在");
        }
        if (!SystemConstants.NORMAL.equals(role.getStatus())) {
            throw new ServiceException("默认角色已停用");
        }
        if (isAdd || ObjectUtil.isNull(role.getClientId()) || !role.getClientId().equals(client.getId())) {
            throw new ServiceException("默认角色必须属于当前客户端");
        }
    }

    /**
     * 停用客户端或变更登录域时清理会话；改注册开关不清 Token。
     *
     * @param db     变更前客户端
     * @param update 变更后客户端
     * @return 是否需要踢出该客户端会话
     */
    private boolean shouldKickClientSessions(SysClient db, SysClient update) {
        if (ObjectUtil.isNull(db)) {
            return false;
        }
        if (!ObjectUtil.equal(db.getUserTypeId(), update.getUserTypeId())) {
            return true;
        }
        return SystemConstants.DISABLE.equals(update.getStatus())
            && !SystemConstants.DISABLE.equals(db.getStatus());
    }

    private boolean shouldInvalidateOpenApiSessions(SysClient db, SysClient update) {
        if (ObjectUtil.isNull(db)) {
            throw new ServiceException("客户端变更前状态不可用");
        }
        return !ObjectUtil.equal(db.getUserTypeId(), update.getUserTypeId())
            || !ObjectUtil.equal(db.getDefaultRoleId(), update.getDefaultRoleId())
            || !ObjectUtil.equal(db.getStatus(), update.getStatus());
    }

    private void invalidateUsersForUserTypes(Collection<Long> userTypeIds) {
        Set<Long> ids = new LinkedHashSet<>();
        userTypeIds.stream().filter(Objects::nonNull).forEach(ids::add);
        if (ids.isEmpty()) {
            return;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        userTypeRelMapper.lambda()
            .in(SysUserTypeRel::getUserTypeId, ids)
            .eq(SysUserTypeRel::getStatus, SystemConstants.NORMAL)
            .list()
            .stream()
            .map(SysUserTypeRel::getUserId)
            .filter(Objects::nonNull)
            .forEach(userIds::add);
        userIds.forEach(openApiSessionInvalidator::invalidateByUserId);
    }

}
