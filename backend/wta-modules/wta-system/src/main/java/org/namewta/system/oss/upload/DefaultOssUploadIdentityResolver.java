package org.namewta.system.oss.upload;

import cn.dev33.satoken.stp.StpUtil;
import org.namewta.common.satoken.utils.LoginHelper;
import org.namewta.system.api.model.LoginUser;
import org.springframework.stereotype.Component;

/**
 * 从当前 Sa-Token 会话快照上传调用身份。
 */
@Component
public class DefaultOssUploadIdentityResolver implements OssUploadIdentityResolver {

    @Override
    public Identity resolve() {
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null || user.getUserId() == null) {
            throw new OssUploadException(OssUploadError.ACCESS_DENIED, "上传需要登录用户");
        }
        return new Identity(user.getUserId(), user.getClientPk());
    }

    @Override
    public boolean hasPermission(String permission) {
        return permission == null || permission.isBlank() || StpUtil.hasPermission(permission);
    }
}
