package org.namewta.system.oss.upload;

/**
 * 上传调用身份接缝，便于 HTTP、测试和后台入口使用同一状态机。
 */
public interface OssUploadIdentityResolver {

    Identity resolve();

    boolean hasPermission(String permission);

    record Identity(Long userId, Long clientPk) {
    }
}
