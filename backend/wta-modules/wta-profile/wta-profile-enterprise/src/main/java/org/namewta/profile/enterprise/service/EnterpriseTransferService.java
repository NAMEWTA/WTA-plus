package org.namewta.profile.enterprise.service;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore;
import org.namewta.profile.enterprise.port.security.EnterpriseTransferCodePort;
import org.namewta.profile.enterprise.dao.EnterpriseTransferDao;
import org.namewta.profile.enterprise.domain.model.read.EnterpriseTransferOwnerRow;
import org.namewta.profile.enterprise.domain.exception.EnterpriseTransferException;
import org.namewta.profile.enterprise.domain.transfer.EnterpriseTransferChallenge;
import org.namewta.common.mybatis.utils.IdGeneratorUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.namewta.notify.api.NotificationApplicationService;
import org.namewta.notify.api.NotificationChannel;
import org.namewta.notify.api.NotificationCommand;
import org.namewta.notify.api.NotificationMode;
import org.namewta.notify.api.NotificationReceipt;
import org.namewta.notify.api.NotificationStatus;
import org.namewta.notify.api.NotificationStrategy;
import org.namewta.profile.api.person.PersonIdentityLookupService;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityLock;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityMatch;
import org.namewta.profile.api.person.PersonIdentityLookupService.ActiveIdentityQuery;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.StageResult;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.Verification;
import org.namewta.profile.enterprise.port.store.EnterpriseTransferChallengeStore.VerificationStatus;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferConfirmBo;
import org.namewta.profile.enterprise.domain.bo.EnterpriseTransferSendBo;
import org.namewta.profile.enterprise.domain.vo.EnterpriseTransferVo;
import org.namewta.system.api.UserService;
import org.namewta.system.api.domain.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * 创建企业转移业务服务。
 */
@Service
public class EnterpriseTransferService {
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private final EnterpriseTransferDao dao;
    private final EnterpriseTransferChallengeStore challenges;
    private final EnterpriseTransferCodePort codes;
    private final PersonIdentityLookupService personIdentities;
    private final UserService users;
    private final NotificationApplicationService notify;
    private final Clock clock;
    /** 创建企业转移业务服务。 */
    @Autowired
    public EnterpriseTransferService(EnterpriseTransferDao dao,
                                     EnterpriseTransferChallengeStore challenges,
                                     EnterpriseTransferCodePort codes,
                                     PersonIdentityLookupService personIdentities,
                                     UserService users,
                                     NotificationApplicationService notify) {
        this(dao, challenges, codes, personIdentities, users, notify, Clock.systemUTC());
    }
    /** 创建可注入时钟的企业转移业务服务，测试场景据此固定过期时间。 */
    public EnterpriseTransferService(EnterpriseTransferDao dao,
                              EnterpriseTransferChallengeStore challenges,
                              EnterpriseTransferCodePort codes,
                              PersonIdentityLookupService personIdentities,
                              UserService users,
                              NotificationApplicationService notify,
                              Clock clock) {
        this.dao = dao;
        this.challenges = challenges;
        this.codes = codes;
        this.personIdentities = personIdentities;
        this.users = users;
        this.notify = notify;
        this.clock = clock;
    }
    /** 兼容存量测试适配器使用的具体验证码生成器构造方法。 */
    @Deprecated
    public EnterpriseTransferService(EnterpriseTransferDao dao,
                              EnterpriseTransferChallengeStore challenges,
                              org.namewta.profile.enterprise.adapter.security.EnterpriseTransferCodeGenerator codes,
                              PersonIdentityLookupService personIdentities, UserService users,
                              NotificationApplicationService notify, Clock clock) {
        this(dao, challenges, (EnterpriseTransferCodePort) codes, personIdentities, users, notify, clock);
    }
    /**
     * 发起企业档案转移
     */

    public EnterpriseTransferVo send(long sourceUserId, EnterpriseTransferSendBo command) {
        OwnerBinding owner = currentActiveOwner(sourceUserId).orElse(null);
        String fullName = command.fullName().trim();
        String documentLastFour = command.documentLastFour().trim().toUpperCase(Locale.ROOT);
        String phone = command.phone().trim();
        List<TargetIdentity> matches = exactTargets(fullName, documentLastFour, phone);
        if (owner == null || matches.size() != 1 || matches.getFirst().userId() == sourceUserId) {
            return EnterpriseTransferVo.status("NOT_AVAILABLE");
        }
        TargetIdentity target = matches.getFirst();
        if (hasEffectiveEnterpriseBinding(target.userId())) {
            return EnterpriseTransferVo.status("NOT_AVAILABLE");
        }
        String challengeId = UUID.randomUUID().toString();
        String code = codes.generate();
        Instant expiresAt = clock.instant().plus(CHALLENGE_TTL);
        EnterpriseTransferChallenge challenge = new EnterpriseTransferChallenge(challengeId, sourceUserId,
            target.userId(), owner.profileId(), owner.bindingId(), owner.version(), target.personProfileId(),
            fullName, documentLastFour, target.phone(),
            BCrypt.hashpw(code), EnterpriseTransferChallenge.State.PENDING_DELIVERY, 0, expiresAt.toEpochMilli());
        if (challenges.stage(challenge) != StageResult.STAGED) {
            throw failure("ENTERPRISE_TRANSFER_RATE_LIMITED");
        }
        NotificationReceipt result;
        try {
            result = notify.submit(new NotificationCommand("profile", "enterprise-transfer", "ENTERPRISE_TRANSFER",
                challengeId, "PHONE", List.of(target.phone()), "enterprise-transfer",
                Map.of("code", code),
                List.of(NotificationChannel.SMS), NotificationStrategy.ALL, NotificationMode.SYNC, 80, null, expiresAt,
                "profile:enterprise:transfer:" + challengeId, Map.of("audit", "REDACT_SENSITIVE")));
        } catch (RuntimeException exception) {
            challenges.revoke(challengeId);
            throw new EnterpriseTransferException("ENTERPRISE_TRANSFER_DELIVERY_FAILED", exception);
        }
        if (!delivered(result)) {
            challenges.revoke(challengeId);
            throw failure("ENTERPRISE_TRANSFER_DELIVERY_FAILED");
        }
        try {
            recordChallenge(challenge, clock.instant());
            if (!challenges.activate(challengeId)) {
                throw failure("ENTERPRISE_TRANSFER_CHALLENGE_STATE_FAILURE");
            }
        } catch (RuntimeException exception) {
            challenges.revoke(challengeId);
            throw exception;
        }
        return EnterpriseTransferVo.sent(challengeId);
    }
    /**
     * 确认当前业务操作
     */

    public EnterpriseTransferVo confirm(long sourceUserId, EnterpriseTransferConfirmBo command) {
        Verification verification = challenges.verify(command.challengeId(), sourceUserId, command.code());
        if (verification.status() != VerificationStatus.VERIFIED || verification.verified() == null) {
            throw failure("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        }
        EnterpriseTransferChallenge challenge = verification.verified().challenge();
        ActiveIdentityLock lock = new ActiveIdentityLock(challenge.targetUserId(), challenge.personProfileId(),
            challenge.fullName(), challenge.documentLastFour());
        ActiveIdentityMatch identity = personIdentities.lockActiveExactMatch(lock).orElse(null);
        if (identity == null || !eligibleLockedAccount(identity, challenge.phone())
            || hasEffectiveEnterpriseBinding(identity.userId())) {
            throw failure("ENTERPRISE_TRANSFER_TARGET_INELIGIBLE");
        }
        transferBindings(challenge, clock.instant());
        if (!challenges.consume(verification.verified())) {
            throw failure("ENTERPRISE_TRANSFER_CHALLENGE_INVALID");
        }
        return EnterpriseTransferVo.status("TRANSFERRED");
    }
    /**
     * 解除档案绑定
     */

    public EnterpriseTransferVo unbind(long userId) {
        unbindBinding(userId, clock.instant());
        return EnterpriseTransferVo.status("UNBOUND");
    }
    /**
     * 查询当前生效的企业所有者
     */
    public Optional<OwnerBinding> currentActiveOwner(long userId) {
        return Optional.ofNullable(dao.selectActiveOwner(userId)).map(this::owner);
    }
    /**
     * 判断用户是否存在生效企业绑定
     */
    public boolean hasEffectiveEnterpriseBinding(long userId) {
        return dao.countEffectiveBinding(userId) > 0;
    }
    /**
     * 记录转移挑战信息
     */
    public void recordChallenge(EnterpriseTransferChallenge challenge, Instant occurredTime) {
        try {
            changed(dao.insertTransferRecord(IdGeneratorUtil.nextLongId(), challenge.enterpriseProfileId(),
                challenge.sourceBindingId(), challenge.sourceUserId(), challenge.targetUserId(),
                challenge.challengeId(), challenge.sourceBindingVersion(),
                Instant.ofEpochMilli(challenge.expiresAtEpochMilli()), occurredTime),
                "ENTERPRISE_TRANSFER_RECORD_CONFLICT");
        } catch (DuplicateKeyException exception) {
            throw new EnterpriseTransferException("ENTERPRISE_TRANSFER_RECORD_CONFLICT", exception);
        }
    }
    /**
     * 转移档案绑定关系
     */
    public void transferBindings(EnterpriseTransferChallenge challenge, Instant occurredTime) {
        EnterpriseTransferOwnerRow source = dao.lockActiveOwner(challenge.sourceUserId());
        if (source == null || source.getBindingId() != challenge.sourceBindingId()
            || source.getProfileId() != challenge.enterpriseProfileId()
            || source.getBindingVersion() != challenge.sourceBindingVersion()) {
            throw failure("ENTERPRISE_TRANSFER_SOURCE_CHANGED");
        }
        if (dao.lockEffectiveBindingId(challenge.targetUserId()) != null) {
            throw failure("ENTERPRISE_TRANSFER_TARGET_INELIGIBLE");
        }
        try {
            changed(dao.unbindSource(source.getBindingId(), source.getProfileId(), source.getUserId(),
                source.getBindingVersion(), occurredTime, challenge.sourceUserId()),
                "ENTERPRISE_TRANSFER_SOURCE_CHANGED");
            event(source.getBindingId(), source.getProfileId(), source.getUserId(), "UNBOUND",
                source.getBindingVersion() + 1, "SELF_TRANSFER", source.getBindingId(),
                "TRANSFER_CHALLENGE:" + challenge.challengeId(), occurredTime, challenge.sourceUserId());
            long newBindingId = IdGeneratorUtil.nextLongId();
            changed(dao.insertBinding(newBindingId, source.getProfileId(), challenge.targetUserId(),
                "SELF_TRANSFER", source.getBindingId(), occurredTime, challenge.sourceUserId()),
                "ENTERPRISE_TRANSFER_BINDING_CONFLICT");
            event(newBindingId, source.getProfileId(), challenge.targetUserId(), "ACTIVE", 1,
                "SELF_TRANSFER", source.getBindingId(), "TRANSFER_CHALLENGE:" + challenge.challengeId(),
                occurredTime, challenge.sourceUserId());
            changed(dao.confirmTransferRecord(challenge.challengeId(), source.getProfileId(),
                source.getBindingId(), source.getUserId(), challenge.targetUserId(), source.getBindingVersion(),
                occurredTime, challenge.sourceUserId()), "ENTERPRISE_TRANSFER_RECORD_CHANGED");
        } catch (DuplicateKeyException exception) {
            throw new EnterpriseTransferException("ENTERPRISE_TRANSFER_BINDING_CONFLICT", exception);
        }
    }
    /**
     * 解除指定绑定关系
     */
    public long unbindBinding(long userId, Instant occurredTime) {
        EnterpriseTransferOwnerRow source = dao.lockActiveOwner(userId);
        if (source == null) {
            throw failure("ENTERPRISE_TRANSFER_SOURCE_NOT_ACTIVE");
        }
        changed(dao.unbindSource(source.getBindingId(), source.getProfileId(), source.getUserId(),
            source.getBindingVersion(), occurredTime, userId), "ENTERPRISE_TRANSFER_SOURCE_CHANGED");
        event(source.getBindingId(), source.getProfileId(), source.getUserId(), "UNBOUND",
            source.getBindingVersion() + 1, "SELF_UNBIND", source.getBindingId(),
            "RESPONSIBLE_SELF_UNBIND", occurredTime, userId);
        return source.getProfileId();
    }
    /**
     * 按身份条件查询唯一目标
     */
    private List<TargetIdentity> exactTargets(String fullName, String documentLastFour, String phone) {
        List<ActiveIdentityMatch> identities = personIdentities.findActiveExactMatches(
            new ActiveIdentityQuery(fullName, documentLastFour));
        if (identities == null || identities.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = identities.stream().map(ActiveIdentityMatch::userId).collect(Collectors.toSet());
        Map<Long, UserDTO> accounts = safeUsers(users.selectListByIds(userIds)).stream()
            .filter(user -> user != null && user.getUserId() != null)
            .collect(Collectors.toMap(UserDTO::getUserId, Function.identity(), (left, right) -> left));
        return identities.stream()
            .filter(identity -> activeWithPhone(accounts.get(identity.userId()), phone))
            .map(identity -> new TargetIdentity(identity.userId(), identity.personProfileId(), phone))
            .toList();
    }
    /**
     * 查询符合条件且已锁定的账户
     */
    private boolean eligibleLockedAccount(ActiveIdentityMatch identity, String phone) {
        UserDTO user = users.lockActiveById(identity.userId());
        return user != null && Objects.equals(identity.userId(), user.getUserId())
            && activeWithPhone(user, phone);
    }
    /**
     * 安全查询用户信息
     */
    private Collection<UserDTO> safeUsers(List<UserDTO> accounts) {
        return accounts == null ? List.of() : accounts;
    }
    /**
     * 按手机号查询生效身份
     */
    private boolean activeWithPhone(UserDTO user, String phone) {
        return user != null && "0".equals(user.getStatus()) && phone.equals(user.getPhoneNumber());
    }
    /**
     * 判断通知是否已送达
     */
    private boolean delivered(NotificationReceipt result) {
        return result != null && (result.status() == NotificationStatus.ACCEPTED
            || result.status() == NotificationStatus.DELIVERED);
    }
    /**
     * 构造流程事件数据
     */
    private void event(long bindingId, long profileId, long userId, String eventType, int bindingVersion,
                       String sourceType, long sourceId, String reason, Instant occurredTime, long operatorId) {
        changed(dao.insertEvent(IdGeneratorUtil.nextLongId(), bindingId, profileId, userId, eventType, bindingVersion,
            sourceType, sourceId, reason, occurredTime, operatorId), "ENTERPRISE_TRANSFER_EVENT_CONFLICT");
    }
    /**
     * 解析材料所有者
     */
    private OwnerBinding owner(EnterpriseTransferOwnerRow row) {
        return new OwnerBinding(row.getBindingId(), row.getProfileId(), row.getUserId(), row.getBindingVersion());
    }
    /**
     * 处理changed。
     */
    private void changed(int count, String category) {
        if (count != 1) {
            throw failure(category);
        }
    }
    /**
     * 构造业务失败异常
     */
    private EnterpriseTransferException failure(String category) {
        return new EnterpriseTransferException(category);
    }
    /**
     * 承载TargetIdentity业务规则的领域服务。
     */
    private record TargetIdentity(long userId, long personProfileId, String phone) {
    }
    /**
     * 承载OwnerBinding业务规则的领域服务。
     */
    record OwnerBinding(long bindingId, long profileId, long userId, int version) {
    }
}
