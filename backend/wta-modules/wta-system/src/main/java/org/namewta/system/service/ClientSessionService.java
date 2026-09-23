package org.namewta.system.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.core.utils.StringUtils;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 按登录域或客户端清理在线会话。
 */
@Slf4j
@Service
public class ClientSessionService {

    private final TokenOperations tokenOperations;
    private final Cache<InvalidationTarget, Set<String>> pendingTokens = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    /**
     * 创建使用 Sa-Token 生产边界的会话服务。
     */
    public ClientSessionService() {
        this(new SaTokenOperations());
    }

    /**
     * 创建使用指定 Token 边界的会话服务。
     *
     * @param tokenOperations Token 操作边界
     */
    public ClientSessionService(TokenOperations tokenOperations) {
        this.tokenOperations = Objects.requireNonNull(tokenOperations, "tokenOperations");
    }

    /**
     * 踢出指定用户在指定登录域下的全部 Token。userId 为空时踢出该登录域的全部 Token。
     *
     * @param userId       用户ID，可空
     * @param userTypeCode 登录域编码
     * @return 失效结果
     */
    public InvalidationResult kickoutUserType(Long userId, String userTypeCode) {
        if (StringUtils.isBlank(userTypeCode)) {
            return InvalidationResult.empty("userType", userId, null);
        }
        return invalidate("userType", userId, null, userTypeCode,
            loginUser -> userTypeCode.equals(loginUser.getUserType())
            && (userId == null || userId.equals(loginUser.getUserId())));
    }

    /**
     * 踢出指定客户端（主键）下的全部 Token。
     *
     * @param clientId 客户端主键
     * @return 失效结果
     */
    public InvalidationResult kickoutClient(Long clientId) {
        if (clientId == null) {
            throw new ServiceException("客户端主键不能为空");
        }
        return invalidate("client", null, clientId, null,
            loginUser -> clientId.equals(loginUser.getClientPk()));
    }

    /**
     * 踢出指定用户在指定客户端下的 Token。
     *
     * @param userId   用户ID
     * @param clientId 客户端主键
     * @return 失效结果
     */
    public InvalidationResult kickoutUserClient(Long userId, Long clientId) {
        if (userId == null) {
            throw new ServiceException("用户主键不能为空");
        }
        if (clientId == null) {
            throw new ServiceException("客户端主键不能为空");
        }
        return invalidate("userClient", userId, clientId, null,
            loginUser -> userId.equals(loginUser.getUserId())
            && clientId.equals(loginUser.getClientPk()));
    }

    private InvalidationResult invalidate(String targetType, Long userId, Long clientId, String scope,
                                          Predicate<LoginUser> matcher) {
        int matched = 0;
        int invalidated = 0;
        InvalidationTarget target = new InvalidationTarget(targetType, userId, clientId, scope);
        Set<String> pending = pendingTokens.get(target, ignored -> ConcurrentHashMap.newKeySet());
        try {
            Set<String> candidates = new LinkedHashSet<>(pending);
            candidates.addAll(tokenOperations.activeTokenValues());
            for (String token : candidates) {
                boolean retryingUnconfirmedToken = pending.contains(token);
                LoginUser loginUser = tokenOperations.getLoginUser(token);
                if (!retryingUnconfirmedToken && (loginUser == null || !matcher.test(loginUser))) {
                    continue;
                }
                matched++;
                try {
                    tokenOperations.logout(token);
                } catch (NotLoginException ignored) {
                    if (retryingUnconfirmedToken) {
                        throw new IllegalStateException("Unconfirmed cluster invalidation could not be retried");
                    }
                    // 会话已由并发请求清除，目标状态已经收敛。
                } catch (RuntimeException ex) {
                    pending.add(token);
                    throw ex;
                }
                pending.remove(token);
                invalidated++;
            }
        } catch (RuntimeException ex) {
            log.error("授权会话失效失败 targetType={} userId={} clientPk={} matched={} invalidated={} errorType={}",
                targetType, userId, clientId, matched, invalidated, ex.getClass().getSimpleName());
            throw new ServiceException("授权会话失效失败，请重试")
                .setDetailMessage(ex.getClass().getName());
        }
        if (pending.isEmpty()) {
            pendingTokens.invalidate(target);
        }
        log.info("授权会话失效完成 targetType={} userId={} clientPk={} matched={} invalidated={} clusterAcknowledged=true",
            targetType, userId, clientId, matched, invalidated);
        return new InvalidationResult(targetType, userId, clientId, matched, invalidated, true);
    }

    /**
     * 会话失效结果。clusterAcknowledged=true 表示 Sa-Token 删除及 T-01 集群确认均已完成。
     */
    public record InvalidationResult(String targetType, Long userId, Long clientPk,
                                     int matchedTokenCount, int invalidatedTokenCount,
                                     boolean clusterAcknowledged) {

        private static InvalidationResult empty(String targetType, Long userId, Long clientPk) {
            return new InvalidationResult(targetType, userId, clientPk, 0, 0, true);
        }
    }

    /**
     * Token 存储和注销边界，用于验证目标隔离和故障传播。
     */
    public interface TokenOperations {

        List<String> activeTokenValues();

        LoginUser getLoginUser(String tokenValue);

        void logout(String tokenValue);
    }

    private record InvalidationTarget(String targetType, Long userId, Long clientPk, String scope) {
    }

    private static final class SaTokenOperations implements TokenOperations {

        @Override
        public List<String> activeTokenValues() {
            List<String> keys = StpUtil.searchTokenValue("", 0, -1, false);
            if (CollUtil.isEmpty(keys)) {
                return List.of();
            }
            List<String> tokens = new ArrayList<>(keys.size());
            for (String key : keys) {
                String token = StringUtils.substringAfterLast(key, StringUtils.COLON);
                if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(token) >= -1) {
                    tokens.add(token);
                }
            }
            return tokens;
        }

        @Override
        public LoginUser getLoginUser(String tokenValue) {
            return LoginHelper.getLoginUser(tokenValue);
        }

        @Override
        public void logout(String tokenValue) {
            StpUtil.logoutByTokenValue(tokenValue);
        }
    }
}
