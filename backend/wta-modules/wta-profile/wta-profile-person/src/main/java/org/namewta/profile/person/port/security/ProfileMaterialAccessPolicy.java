package org.namewta.profile.person.port.security;

import org.namewta.profile.person.domain.material.MaterialOwner;
/** 档案材料访问策略接口，集中执行目录和材料权限校验。 */
public interface ProfileMaterialAccessPolicy {

    /** 校验材料目录管理权限。 */
    void requireCatalogManage();

    /** 校验材料目录读取权限。 */
    void requireCatalogRead();

    /** 校验材料关联权限。 */
    Long requireAttach(MaterialOwner owner);

    /** 校验并获取写入权限。 */
    void requireWrite(MaterialOwner owner);

    /** 校验并获取读取权限。 */
    void requireRead(MaterialOwner owner);
}
