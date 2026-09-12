package org.namewta.profile.api.domain;

/**
 * 账户当前有效个人与企业档案的非敏感摘要。
 *
 * @param userId     账户 ID
 * @param person     当前有效个人档案绑定，不存在时为 {@code null}
 * @param enterprise 当前有效企业档案绑定，不存在时为 {@code null}
 */
public record ProfileSummary(Long userId, ProfileBindingSummary person, ProfileBindingSummary enterprise) {

    public ProfileSummary {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        requireType(person, ProfileType.PERSON);
        requireType(enterprise, ProfileType.ENTERPRISE);
    }

    public static ProfileSummary unverified(Long userId) {
        return new ProfileSummary(userId, null, null);
    }

    public boolean personVerified() {
        return person != null;
    }

    public boolean enterpriseVerified() {
        return enterprise != null;
    }

    private static void requireType(ProfileBindingSummary binding, ProfileType expected) {
        if (binding != null && binding.profileType() != expected) {
            throw new IllegalArgumentException(expected + " binding has mismatched profile type");
        }
    }
}
