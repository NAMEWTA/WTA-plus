package org.namewta.web.service.social;

import cn.hutool.crypto.digest.BCrypt;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import org.namewta.common.core.constant.SystemConstants;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.common.social.oidc.OidcProtocolClient;
import org.namewta.system.domain.SysSocial;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.constant.UserTypeGrantSource;
import org.namewta.system.domain.policy.UserPhonePolicy;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.namewta.system.mapper.SysSocialMapper;
import org.namewta.system.mapper.SysUserMapper;
import org.namewta.system.service.ISysUserService;
import org.namewta.system.service.ISysUserTypeRelService;
import org.namewta.system.service.ISysUserTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/** 外部身份账号的数据库事务边界；由门面在分布式冲突锁内调用真实 Spring 代理。 */
@Service
@RequiredArgsConstructor
public class ExternalAuthAccountTransactionService {
    private final SysSocialMapper socialMapper;
    private final SysUserMapper userMapper;
    private final ISysUserService userService;
    private final ISysUserTypeRelService userTypes;
    private final ISysUserTypeService typeService;

    /** 查找稳定身份；JustAuth 历史关系仅按原来的平台身份精确兼容。 */
    public Long findUser(OidcIdentity identity, String protocol) {
        SysSocial relation =
                socialMapper.selectOne(
                        new LambdaQueryWrapper<SysSocial>()
                                .eq(
                                        SysSocial::getIdentityKey,
                                        OidcProtocolClient.identityKey(
                                                identity.issuer(), identity.subject())));
        if (relation != null) return relation.getUserId();
        if (!"OIDC".equals(protocol)) {
            List<SysSocial> legacy =
                    socialMapper.selectList(
                            new LambdaQueryWrapper<SysSocial>()
                                    .eq(SysSocial::getAuthId, protocol + identity.subject()));
            if (legacy.size() == 1) return legacy.getFirst().getUserId();
            if (legacy.size() > 1) throw new ServiceException("历史第三方绑定存在冲突，请联系管理员处理");
        }
        return null;
    }

    /** 每次登录都检查本地账号当前状态，身份提供方成功不等于业务准入成功。 */
    public SysUserVo requireUser(Long userId) {
        SysUserVo user = userMapper.selectVoById(userId);
        if (user == null || !SystemConstants.NORMAL.equals(user.getStatus()))
            throw new ServiceException("本地账号不存在或已停用");
        return user;
    }

    /** 明确绑定到当前本地用户，同一身份重复绑定幂等，不覆盖其他身份或其他用户。 */
    @DSTransactional
    public void bind(Long userId, String providerKey, String protocol, OidcIdentity identity) {
        if (socialMapper.lockUser(userId) == null) throw new ServiceException("本地账号不存在");
        SysUserVo user = requireUser(userId);
        Long existing = findUser(identity, protocol);
        if (existing != null) {
            if (!existing.equals(userId)) throw new ServiceException("该第三方身份已绑定其他账号");
            return;
        }
        // OIDC 的不同 App 可以获得同一人的不同 pairwise sub，均需当前用户明确授权绑定。
        // 精确 issuer+sub 的唯一所有权仍由前置查询、用户锁和 identity_key 唯一键共同保证。
        if (!"OIDC".equals(protocol)) {
            Long existingIssuer =
                    socialMapper.selectCount(
                            new LambdaQueryWrapper<SysSocial>()
                                    .eq(SysSocial::getUserId, userId)
                                    .eq(SysSocial::getIssuer, identity.issuer()));
            if (existingIssuer > 0) throw new ServiceException("请先解绑该身份源的原账号，再绑定新账号");
        }
        insertBinding(userId, user.getUserName(), providerKey, identity);
    }

    /** 原子创建普通用户、当前 Client 登录域和绑定；identity_key 唯一键处理并发重复身份。 */
    @DSTransactional
    public Long register(
            SysClientVo client,
            String providerKey,
            String protocol,
            OidcIdentity identity,
            String phone) {
        Long existing = findUser(identity, protocol);
        if (existing != null) return existing;
        if (!Boolean.TRUE.equals(client.getRegisterEnabled()) || client.getUserTypeId() == null)
            throw new ServiceException("当前应用未开放注册");
        var userType = typeService.queryById(client.getUserTypeId());
        if (userType == null || !SystemConstants.NORMAL.equals(userType.getStatus()))
            throw new ServiceException("当前应用登录域不可用");
        SysUserBo user = new SysUserBo();
        user.setUserId(IdGeneratorUtil.nextLongId());
        user.setUserName("oidc_" + user.getUserId());
        user.setNickName(displayName(identity.name(), user.getUserName()));
        user.setPhoneNumber(UserPhonePolicy.requirePhone(phone));
        if (identity.email() != null
                && identity.email().length() <= 50
                && identity.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            user.setEmail(identity.email());
        if (!userService.checkPhoneUnique(user)
                || user.getEmail() != null && !userService.checkEmailUnique(user)) return null;
        // 外部密码从未进入本站。随机不可知密码仅满足已有存储合同，用户可走既有重置流程。
        user.setPassword(BCrypt.hashpw(OidcProtocolClient.randomToken()));
        user.setStatus(SystemConstants.NORMAL);
        if (!userService.registerUser(user)) throw new ServiceException("创建本地账号失败");
        userTypes.grantUserType(
                user.getUserId(), client.getUserTypeId(), UserTypeGrantSource.SELF_REGISTER);
        insertBinding(user.getUserId(), user.getUserName(), providerKey, identity);
        return user.getUserId();
    }

    public List<ExternalAuthAccountService.Binding> bindings(Long userId) {
        return socialMapper
                .selectList(new LambdaQueryWrapper<SysSocial>().eq(SysSocial::getUserId, userId))
                .stream()
                .map(
                        row ->
                                new ExternalAuthAccountService.Binding(
                                        row.getId(),
                                        row.getSource(),
                                        row.getSource(),
                                        row.getUserName(),
                                        row.getNickName(),
                                        row.getCreateTime()))
                .toList();
    }

    @DSTransactional
    public void unbind(Long userId, Long id) {
        if (socialMapper.lockUser(userId) == null) throw new ServiceException("本地账号不存在");
        if (socialMapper.delete(
                        new LambdaQueryWrapper<SysSocial>()
                                .eq(SysSocial::getId, id)
                                .eq(SysSocial::getUserId, userId))
                != 1) throw new ServiceException("绑定不存在或不属于当前用户");
    }

    private void insertBinding(
            Long userId, String userName, String providerKey, OidcIdentity identity) {
        SysSocial relation = new SysSocial();
        String key = OidcProtocolClient.identityKey(identity.issuer(), identity.subject());
        relation.setUserId(userId);
        relation.setSource(providerKey);
        relation.setAuthId("external:" + key);
        relation.setIssuer(identity.issuer());
        relation.setSubject(identity.subject());
        relation.setIdentityKey(key);
        relation.setOpenId(identity.subject());
        relation.setUserName(userName);
        relation.setNickName(displayName(identity.name(), userName));
        relation.setAccessToken("");
        socialMapper.insert(relation);
    }

    private static String displayName(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String plain = value.replaceAll("[<>\\p{Cntrl}]", "").strip();
        return plain.isEmpty() ? fallback : plain.substring(0, Math.min(30, plain.length()));
    }
}
