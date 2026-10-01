package org.namewta.common.satoken.profile;

import cn.dev33.satoken.stp.StpUtil;
import org.namewta.common.core.exception.ServiceException;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.profile.api.ProfileDisclosureService;
import org.namewta.profile.api.ProfileService;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;
import org.namewta.system.api.model.LoginUser;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 当前普通登录账号的档案访问与认证门禁，共用已有 Profile 公共查询合同。
 * <p>每次读取当前有效绑定，不缓存认证结论；入口检查不替代业务写入时的事务内归属复核。</p>
 */
public final class ProfileAccess {
    private final ProfileService profiles;
    private final ProfileDisclosureService disclosures;

    /**
     * 创建登录态适配器，不依赖个人或企业模块的实现类型。
     * @param profiles 当前有效绑定的批量摘要服务
     * @param disclosures 已授权字段的档案披露服务
     */
    public ProfileAccess(ProfileService profiles, ProfileDisclosureService disclosures) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.disclosures = Objects.requireNonNull(disclosures, "disclosures");
    }

    /**
     * 查询当前普通账号的有效认证摘要，账号编号仅取自已核对的登录态。
     * @return 主体匹配的当前摘要，无有效绑定时对应认证标志为 false
     * @throws ServiceException 当前登录上下文缺失、不一致或属于机器身份，业务码为 401
     * @throws IllegalStateException 查询服务返回空值或另一主体
     */
    public ProfileSummary currentSummary() {
        return summary(currentUserId());
    }

    /**
     * 判断当前账号是否有有效个人档案绑定；查询失败原样传播。
     * @return 当前个人认证是否有效
     */
    public boolean isPersonVerified() {
        return currentSummary().personVerified();
    }

    /**
     * 判断当前账号是否为有效企业档案的绑定负责人，不隐含个人认证。
     * @return 当前企业负责人认证是否有效
     */
    public boolean isEnterpriseVerified() {
        return currentSummary().enterpriseVerified();
    }

    /**
     * 要求当前账号同时满足所有指定认证，重复类型按一次处理。
     * @param requiredTypes 非空认证类型；多项按 AND 校验
     * @return 已通过门禁的当前摘要，可供调用方复用，避免再次查询
     * @throws IllegalArgumentException 类型数组为空或包含 null
     * @throws ServiceException 登录上下文无效时业务码为 401；认证不足时为 403，data 给出稳定原因和缺失类型
     */
    public ProfileSummary requireVerified(ProfileType... requiredTypes) {
        long userId = currentUserId();
        if (requiredTypes == null || requiredTypes.length == 0) {
            throw new IllegalArgumentException("至少指定一种档案认证类型");
        }
        EnumSet<ProfileType> required = EnumSet.noneOf(ProfileType.class);
        for (ProfileType type : requiredTypes) {
            if (type == null) throw new IllegalArgumentException("档案认证类型不能为 null");
            required.add(type);
        }
        ProfileSummary summary = summary(userId);
        List<ProfileType> missing = required.stream().filter(type -> switch (type) {
            case PERSON -> !summary.personVerified();
            case ENTERPRISE -> !summary.enterpriseVerified();
        }).toList();
        if (!missing.isEmpty()) {
            String reason = missing.size() > 1 ? "PROFILE_VERIFICATION_REQUIRED"
                : missing.getFirst() == ProfileType.PERSON ? "PERSON_VERIFICATION_REQUIRED"
                : "ENTERPRISE_VERIFICATION_REQUIRED";
            String message = missing.size() > 1 ? "请先完成实名认证和企业认证"
                : missing.getFirst() == ProfileType.PERSON ? "请先完成实名认证" : "请先完成企业认证";
            throw new ServiceException(message, 403).setData(Map.of(
                "reason", reason,
                "requiredTypes", required.stream().map(Enum::name).toList(),
                "missingTypes", missing.stream().map(Enum::name).toList()));
        }
        return summary;
    }

    /**
     * 读取当前账号已核准的档案字段；门禁通过不等于取得所有字段的披露权限。
     * @param approvedFields 调用业务已授权的封闭字段集合，不得直接透传浏览器字段参数
     * @return 当前主体投影；未选择的子域为 null，未选择的字段保持为空
     * @throws ServiceException 当前登录上下文无效，业务码为 401
     * @throws IllegalArgumentException 字段集合为 null 或含 null
     * @throws IllegalStateException 查询服务返回空值或另一主体
     */
    public ProfileDisclosure currentDisclosure(Set<ProfileDisclosureField> approvedFields) {
        long userId = currentUserId();
        if (approvedFields == null || approvedFields.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("必须提供有效的档案字段白名单");
        }
        Set<ProfileDisclosureField> fields = Set.copyOf(approvedFields);
        ProfileDisclosure disclosure = disclosures.findByUserId(userId, fields);
        if (disclosure == null || !Long.valueOf(userId).equals(disclosure.userId())) {
            throw new IllegalStateException("档案披露服务返回的主体无效");
        }
        return new ProfileDisclosure(userId,
            disclosure.person() != null && fields.stream().anyMatch(field -> field.profileType() == ProfileType.PERSON)
                ? disclosure.person().filtered(fields) : null,
            disclosure.enterprise() != null && fields.stream().anyMatch(field -> field.profileType() == ProfileType.ENTERPRISE)
                ? disclosure.enterprise().filtered(fields) : null);
    }

    /** 校验只读服务返回的主体，不把异常或空结果降级成未认证。 */
    private ProfileSummary summary(long userId) {
        ProfileSummary summary = profiles.findByUserId(userId);
        if (summary == null || !Long.valueOf(userId).equals(summary.userId())) {
            throw new IllegalStateException("档案摘要服务返回的主体无效");
        }
        return summary;
    }

    /**
     * 先验证登录，再核对普通用户上下文及令牌主体，禁止缺 Client 时回退全局账号。
     * clientid 是 OAuth 字符串，不能与 LoginUser.clientKey 的 App 键混为一谈。
     */
    private long currentUserId() {
        StpUtil.checkLogin();
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null || user.getUserId() == null || user.getUserId() <= 0
            || user.getClientPk() == null || user.getClientPk() <= 0
            || user.getClientKey() == null || user.getClientKey().isBlank()
            || user.getUserType() == null || user.getUserType().isBlank()
            || "openapi".equalsIgnoreCase(user.getUserType().strip())
            || !user.getLoginId().equals(StpUtil.getLoginIdAsString())
            || !sameId(StpUtil.getExtra(LoginHelper.USER_KEY), user.getUserId())
            || !sameId(StpUtil.getExtra(LoginHelper.CLIENT_PK_KEY), user.getClientPk())
            || !user.getUserType().equals(StpUtil.getExtra(LoginHelper.USER_TYPE_KEY))
            || !(StpUtil.getExtra(LoginHelper.CLIENT_KEY) instanceof String clientId) || clientId.isBlank()) {
            throw new ServiceException("当前登录上下文不支持档案认证访问", 401)
                .setData(Map.of("reason", "PROFILE_LOGIN_CONTEXT_INVALID"));
        }
        return user.getUserId();
    }

    /** 兼容 JWT 的整数或十进制字符串表示，不把小数或不合法标识强转成账号编号。 */
    private static boolean sameId(Object actual, Long expected) {
        return (actual instanceof Number || actual instanceof String) && expected.toString().equals(actual.toString());
    }
}
