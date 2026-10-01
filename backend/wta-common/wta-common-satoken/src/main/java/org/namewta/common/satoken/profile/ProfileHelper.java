package org.namewta.common.satoken.profile;

import org.namewta.common.core.utils.SpringUtils;
import org.namewta.profile.api.domain.ProfileDisclosure;
import org.namewta.profile.api.domain.ProfileDisclosureField;
import org.namewta.profile.api.domain.ProfileSummary;
import org.namewta.profile.api.domain.ProfileType;

import java.util.Set;

/** 档案访问的静态便利入口，每次解析当前容器中的服务，不保存 Bean 或认证结果。 */
public final class ProfileHelper {
    private ProfileHelper() {
    }

    /**
     * 查询当前普通登录账号的有效认证摘要。
     * @return 当前账号摘要
     */
    public static ProfileSummary currentSummary() {
        return access().currentSummary();
    }

    /**
     * 判断当前账号是否有有效个人认证。
     * @return 个人认证是否有效
     */
    public static boolean isPersonVerified() {
        return access().isPersonVerified();
    }

    /**
     * 判断当前账号是否有有效企业负责人认证。
     * @return 企业认证是否有效，不隐含个人认证
     */
    public static boolean isEnterpriseVerified() {
        return access().isEnterpriseVerified();
    }

    /**
     * 要求当前账号同时满足所有指定认证，失败语义与注入 ProfileAccess 相同。
     * @param requiredTypes 非空认证类型，多项按 AND 校验
     * @return 已通过门禁的当前摘要
     */
    public static ProfileSummary requireVerified(ProfileType... requiredTypes) {
        return access().requireVerified(requiredTypes);
    }

    /**
     * 读取调用业务已经核准的当前账号档案字段，不扩展字段授权范围。
     * @param approvedFields 可信业务代码核准的字段集合
     * @return 当前账号的字段投影
     */
    public static ProfileDisclosure currentDisclosure(Set<ProfileDisclosureField> approvedFields) {
        return access().currentDisclosure(approvedFields);
    }

    /** 按次查找服务，使容器切换和认证状态变化不会沿用旧 Bean 或旧结果。 */
    private static ProfileAccess access() {
        return SpringUtils.getBean(ProfileAccess.class);
    }
}
