package org.namewta.profile.api;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.namewta.profile.api.domain.*;

/** 组合各子域字段投影，校验主体、重复贡献与最终字段上限，避免越权外发。 */
public final class CompositeProfileDisclosureService implements ProfileDisclosureService {
    private final List<ProfileDisclosureContributor> contributors;

    /** 创建只读组合器，防止外部集合在请求间发生变化。 */
    public CompositeProfileDisclosureService(List<ProfileDisclosureContributor> contributors) {
        this.contributors = List.copyOf(contributors);
    }

    @Override
    public ProfileDisclosure findByUserId(Long userId, Set<ProfileDisclosureField> fields) {
        if (userId == null || userId <= 0 || fields == null || fields.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Valid subject and disclosure fields are required");
        }
        Set<ProfileDisclosureField> allowed = Set.copyOf(fields);
        PersonDisclosure person = null;
        EnterpriseDisclosure enterprise = null;
        Set<ProfileType> seen = EnumSet.noneOf(ProfileType.class);
        for (ProfileDisclosureContributor contributor : contributors) {
            ProfileType type = contributor.profileType();
            Set<ProfileDisclosureField> selected = allowed.stream().filter(f -> f.profileType() == type)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (selected.isEmpty()) continue;
            if (!seen.add(type)) throw new IllegalStateException("Duplicate profile disclosure contributor");
            ProfileDisclosure value = contributor.findByUserId(userId, selected);
            if (value == null || !userId.equals(value.userId())
                || type == ProfileType.PERSON && value.enterprise() != null
                || type == ProfileType.ENTERPRISE && value.person() != null) {
                throw new IllegalStateException("Profile disclosure subject or owner mismatch");
            }
            if (type == ProfileType.PERSON && value.person() != null) person = value.person().filtered(selected);
            if (type == ProfileType.ENTERPRISE && value.enterprise() != null) enterprise = value.enterprise().filtered(selected);
        }
        return new ProfileDisclosure(userId, person, enterprise);
    }
}

