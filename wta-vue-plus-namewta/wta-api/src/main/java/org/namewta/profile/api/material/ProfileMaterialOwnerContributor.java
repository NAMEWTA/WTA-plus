package org.namewta.profile.api.material;

import org.namewta.profile.api.domain.ProfileType;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerKey;
import org.namewta.profile.api.material.ProfileMaterialPort.MaterialOwnerType;

import java.util.Objects;
import java.util.Optional;

/**
 * 一类档案对共享材料服务贡献的 owner 解析边界。
 *
 * <p>个人与企业模块分别拥有自身 WORKING、SUBMISSION、SOURCE、VERSION 记录及其关系。
 * 共享 {@link ProfileMaterialPort} 实现通过本合同调用对应 owner，不得直接读取其他业务模块的表、
 * Mapper、Entity 或内部实现。</p>
 */
public interface ProfileMaterialOwnerContributor {

    /**
     * 返回该贡献者拥有的档案类型。
     */
    ProfileType profileType();

    /**
     * 在事务内锁定并解析一个材料 owner。
     *
     * <p>调用方必须在覆盖后续材料操作的动态数据源事务中调用本方法。实现必须拒绝与
     * {@link #profileType()} 不同的 owner，并按 {@link MaterialOwnerType} 锁定所属模块的权威记录。
     * WORKING 与 SUBMISSION 应返回申请账户 ID；不可变 SOURCE 与 VERSION 可以不返回账户 ID。</p>
     *
     * @param owner 已完成基础校验的 owner key
     * @return owner 存在时的解析结果；不存在或不再有效时返回空
     */
    Optional<ResolvedMaterialOwner> lockOwner(MaterialOwnerKey owner);

    /**
     * 判断 WORKING owner 当前是否仍允许增删材料。
     *
     * @param owner 必须属于该贡献者且 ownerType 为 WORKING
     * @return 当前业务状态允许编辑时为 {@code true}
     */
    boolean isWorkingEditable(MaterialOwnerKey owner);

    /**
     * 判断源 owner 与目标 owner 是否具有权威快照关系。
     *
     * @param relationship 已校验的同档案类型快照关系
     * @return 所属模块中存在该关系时为 {@code true}
     */
    boolean hasSnapshotRelationship(SnapshotRelationship relationship);

    /**
     * 已解析的材料 owner，不包含档案或证件明文。
     *
     * @param owner           owner key
     * @param applicantUserId WORKING/SUBMISSION 的申请账户 ID；不可变 owner 可以为 {@code null}
     */
    record ResolvedMaterialOwner(MaterialOwnerKey owner, Long applicantUserId) {

        public ResolvedMaterialOwner {
            Objects.requireNonNull(owner, "owner");
            if (applicantUserId != null && applicantUserId <= 0) {
                throw new IllegalArgumentException("applicantUserId must be positive when present");
            }
            if ((owner.ownerType() == MaterialOwnerType.WORKING
                || owner.ownerType() == MaterialOwnerType.SUBMISSION) && applicantUserId == null) {
                throw new IllegalArgumentException("editable and submission owners require applicantUserId");
            }
        }
    }

    /**
     * 一个业务模块拥有的材料快照关系。
     *
     * <p>只允许 WORKING 到 SUBMISSION、SUBMISSION 到 VERSION、SOURCE 到 VERSION。
     * 两端必须属于相同的 {@link ProfileType}。</p>
     *
     * @param source 快照来源
     * @param target 不可变快照目标
     */
    record SnapshotRelationship(MaterialOwnerKey source, MaterialOwnerKey target) {

        public SnapshotRelationship {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(target, "target");
            if (source.profileType() != target.profileType()) {
                throw new IllegalArgumentException("snapshot owners must use the same profileType");
            }
            if (!validTransition(source.ownerType(), target.ownerType())) {
                throw new IllegalArgumentException("unsupported material snapshot transition");
            }
        }

        private static boolean validTransition(MaterialOwnerType source, MaterialOwnerType target) {
            return (source == MaterialOwnerType.WORKING && target == MaterialOwnerType.SUBMISSION)
                || (source == MaterialOwnerType.SUBMISSION && target == MaterialOwnerType.VERSION)
                || (source == MaterialOwnerType.SOURCE && target == MaterialOwnerType.VERSION);
        }
    }
}
