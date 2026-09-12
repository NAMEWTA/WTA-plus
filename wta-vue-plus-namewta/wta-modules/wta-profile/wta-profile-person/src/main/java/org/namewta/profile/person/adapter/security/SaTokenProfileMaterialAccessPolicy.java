package org.namewta.profile.person.adapter.security;
import org.namewta.profile.person.port.security.ProfileMaterialAccessPolicy;
import org.namewta.profile.person.domain.exception.ProfileMaterialException;
import org.namewta.profile.person.domain.material.MaterialOwner;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;
import cn.dev33.satoken.stp.StpUtil;
import org.namewta.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Component;
import java.util.Objects;
/** 基于 Sa-Token 的材料访问策略，实现目录和所有者权限校验。 */
@Component
public class SaTokenProfileMaterialAccessPolicy implements ProfileMaterialAccessPolicy {
    /** 校验材料目录管理权限。 */
    @Override
    public void requireCatalogManage() {
        require(StpUtil.hasPermission("profile:material-tag:manage"));
    }
    /** 校验材料目录读取权限。 */
    @Override
    public void requireCatalogRead() {
        require(StpUtil.hasPermission("profile:material-tag:query")
            || StpUtil.hasPermission("profile:material-tag:manage"));
    }
    /** 校验材料关联权限。 */
    @Override
    public Long requireAttach(MaterialOwner owner) {
        String prefix = prefix(owner);
        Long currentUserId = LoginHelper.getUserId();
        boolean applicant = owner.key().ownerType() == MaterialOwnerType.WORKING
            && StpUtil.hasPermission(prefix + ":material")
            && owner.applicantUserId() != null
            && Objects.equals(owner.applicantUserId(), currentUserId);
        boolean administrator = owner.key().ownerType() == MaterialOwnerType.SOURCE
            && StpUtil.hasPermission(prefix + ":override");
        require(applicant || administrator);
        return currentUserId;
    }
    /** 校验并获取写入权限。 */
    @Override
    public void requireWrite(MaterialOwner owner) {
        String prefix = prefix(owner);
        require(StpUtil.hasPermission(prefix + ":material")
            && owner.applicantUserId() != null
            && Objects.equals(owner.applicantUserId(), LoginHelper.getUserId()));
    }
    /** 校验并获取读取权限。 */
    @Override
    public void requireRead(MaterialOwner owner) {
        String prefix = prefix(owner);
        boolean management = StpUtil.hasPermission(prefix + ":query")
            || StpUtil.hasPermission(prefix + ":review")
            || StpUtil.hasPermission(prefix + ":manage")
            || StpUtil.hasPermission(prefix + ":override");
        boolean applicant = StpUtil.hasPermission(prefix + ":material")
            && owner.applicantUserId() != null
            && Objects.equals(owner.applicantUserId(), LoginHelper.getUserId());
        require(management || applicant);
    }
    /** 生成材料访问权限前缀。 */
    private String prefix(MaterialOwner owner) {
        return "profile:" + owner.key().profileType().name().toLowerCase(java.util.Locale.ROOT);
    }
    /** 校验并获取当前材料所有者。 */
    private void require(boolean allowed) {
        if (!allowed) {
            throw new ProfileMaterialException("MATERIAL_ACCESS_DENIED");
        }
    }
}
