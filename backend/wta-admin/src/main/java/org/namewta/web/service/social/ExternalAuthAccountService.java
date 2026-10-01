package org.namewta.web.service.social;

import cn.hutool.crypto.digest.DigestUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.social.oidc.OidcIdentity;
import org.namewta.system.domain.policy.UserPhonePolicy;
import org.namewta.system.domain.vo.SysClientVo;
import org.namewta.system.domain.vo.SysUserVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 外部身份账号门面；跨节点注册冲突锁覆盖完整数据库事务提交或回滚。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalAuthAccountService {
    private final ExternalAuthAccountTransactionService transactions;
    private final RedissonClient redis;

    /** 所有权元数据投影，不含任何第三方令牌。 */
    public record Binding(
            Long id,
            String providerKey,
            String name,
            String userName,
            String nickName,
            java.time.LocalDateTime createTime) {}

    /** 精确外部身份查找，不通过邮箱或手机号合并账户。 */
    public Long findUser(OidcIdentity identity, String protocol) {
        return transactions.findUser(identity, protocol);
    }

    /** 复核本地用户当前启用状态。 */
    public SysUserVo requireUser(Long userId) {
        return transactions.requireUser(userId);
    }

    /** 用户分布式锁覆盖事务提交，与登录签发、解绑使用相同的串行边界。 */
    public void bind(Long userId, String providerKey, String protocol, OidcIdentity identity) {
        withUserLock(
                userId,
                () -> {
                    transactions.bind(userId, providerKey, protocol, identity);
                    return null;
                });
    }

    /** 在签发业务令牌前重查精确身份所有权，锁保持到签发结束。 不创建数据库事务：绑定/解绑的代理事务已在释放同一把锁之前提交。 */
    public <T> T withBoundIdentity(
            Long userId, OidcIdentity identity, String protocol, Supplier<T> issueToken) {
        return withUserLock(
                userId,
                () -> {
                    if (!userId.equals(transactions.findUser(identity, protocol))) {
                        throw new ServiceException("第三方身份绑定已变更，请重新发起登录");
                    }
                    return issueToken.get();
                });
    }

    /**
     * 手机号冲突按摘要键串行；邮箱采用统一检查锁匹配数据库现有字符排序规则。 锁先于事务读取获取，避免等待者沿用等待前的可重复读快照；代理返回时数据库已提交。 此范围只协调外部注册，不改变存量
     * System 普通用户写入策略。
     */
    public Long register(
            SysClientVo client,
            String providerKey,
            String protocol,
            OidcIdentity identity,
            String phone) {
        String normalizedPhone = UserPhonePolicy.requirePhone(phone);
        var keys = new TreeSet<String>();
        keys.add("external-auth:account:phone:" + DigestUtil.sha256Hex(normalizedPhone));
        // MySQL 既有邮箱比较可能忽略大小写和重音；统一检查锁无需假设 Java 能复现该排序规则。
        if (identity.email() != null && !identity.email().isBlank()) {
            keys.add("external-auth:account:email-conflicts");
        }
        var held = new ArrayList<RLock>();
        try {
            for (String key : keys) {
                var lock = redis.getLock(key);
                if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                    throw new ServiceException("账号正在注册，请稍后重新发起登录");
                }
                held.add(lock);
            }
            return transactions.register(client, providerKey, protocol, identity, normalizedPhone);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceException("注册已中断，请重新发起登录");
        } finally {
            for (int i = held.size() - 1; i >= 0; i--) {
                try {
                    held.get(i).unlock();
                } catch (RuntimeException exception) {
                    log.warn("外部注册冲突锁释放失败，等待 Redis watchdog 清理");
                }
            }
        }
    }

    /** 只列出当前用户绑定的公开元数据。 */
    public List<Binding> bindings(Long userId) {
        return transactions.bindings(userId);
    }

    /** 与绑定和登录签发共用用户锁，不允许操作其他用户的关联。 */
    public void unbind(Long userId, Long id) {
        withUserLock(
                userId,
                () -> {
                    transactions.unbind(userId, id);
                    return null;
                });
    }

    private <T> T withUserLock(Long userId, Supplier<T> operation) {
        if (userId == null) throw new ServiceException("请先登录");
        RLock lock = redis.getLock("external-auth:account:user:" + userId);
        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) throw new ServiceException("账号身份正在更新，请重试");
            try {
                return operation.get();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceException("账号身份操作已中断，请重试");
        }
    }
}
