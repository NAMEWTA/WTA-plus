package org.namewta.profile.api.person;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 个人有效身份的跨模块精确匹配服务。
 *
 * <p>该合同仅暴露账户 ID 与个人档案 ID，不返回姓名、完整证件号码或其他身份明文。
 * 调用方必须通过自身拥有的公开服务完成账户状态、手机号等非个人档案条件校验。</p>
 */
public interface PersonIdentityLookupService {

    /**
     * 按姓名和证件号码后四位精确查询当前有效的个人档案绑定。
     *
     * <p>实现只查询未删除、状态为有效的个人档案与绑定。返回多个结果时，调用方不得自行猜测目标，
     * 应按业务规则将本次匹配视为不可用。</p>
     *
     * @param query 已校验并规范化的精确匹配条件
     * @return 只包含账户 ID 与个人档案 ID 的匹配结果；无结果时返回空列表
     */
    List<ActiveIdentityMatch> findActiveExactMatches(ActiveIdentityQuery query);

    /**
     * 在事务内重新锁定并确认一个精确匹配目标仍为当前有效绑定。
     *
     * <p>调用方必须在覆盖后续业务状态变更的动态数据源事务中调用本方法。实现必须使用数据库行锁，
     * 并同时核对候选 ID、姓名与证件号码后四位；不得以先前查询结果替代锁内复核。</p>
     *
     * @param query 已校验并规范化的锁定条件
     * @return 仍满足条件的非敏感匹配；目标不存在或已变化时返回空
     */
    Optional<ActiveIdentityMatch> lockActiveExactMatch(ActiveIdentityLock query);

    /**
     * 个人有效身份精确查询条件。
     *
     * @param fullName         与个人档案一致的完整姓名
     * @param documentLastFour 证件号码后四位；统一转换为大写
     */
    record ActiveIdentityQuery(String fullName, String documentLastFour) {

        public ActiveIdentityQuery {
            fullName = requireText(fullName, "fullName");
            documentLastFour = requireText(documentLastFour, "documentLastFour").toUpperCase(Locale.ROOT);
            if (documentLastFour.length() != 4) {
                throw new IllegalArgumentException("documentLastFour must contain exactly four characters");
            }
        }
    }

    /**
     * 个人有效身份事务内锁定条件。
     *
     * @param userId           待复核的账户 ID
     * @param personProfileId  待复核的个人档案 ID
     * @param fullName         与个人档案一致的完整姓名
     * @param documentLastFour 证件号码后四位；统一转换为大写
     */
    record ActiveIdentityLock(Long userId, Long personProfileId, String fullName, String documentLastFour) {

        public ActiveIdentityLock {
            requirePositive(userId, "userId");
            requirePositive(personProfileId, "personProfileId");
            ActiveIdentityQuery identity = new ActiveIdentityQuery(fullName, documentLastFour);
            fullName = identity.fullName();
            documentLastFour = identity.documentLastFour();
        }
    }

    /**
     * 个人有效身份的非敏感匹配结果。
     *
     * @param userId          当前有效绑定的账户 ID
     * @param personProfileId 当前有效的个人档案 ID
     */
    record ActiveIdentityMatch(Long userId, Long personProfileId) {

        public ActiveIdentityMatch {
            requirePositive(userId, "userId");
            requirePositive(personProfileId, "personProfileId");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
