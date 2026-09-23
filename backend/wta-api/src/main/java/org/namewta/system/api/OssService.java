package org.namewta.system.api;

import org.namewta.system.api.domain.OssDTO;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 通用 OSS服务
 */
public interface OssService {

    /**
     * 通过 ossId 查询对应的 URL。
     *
     * <p>兼容接口，不建议新代码优先使用。该返回值会丢失 PUBLIC/PRIVATE 访问类型和签名到期时间；
     * 新接口应在完成业务授权后逐个调用 {@link #resolveAccessUrl(Long)}。</p>
     *
     * @param ossIds ossId 串，逗号分隔
     * @return URL 串，逗号分隔
     * @deprecated 仅为旧调用方和 OSS 翻译器保留；新代码使用 {@link #resolveAccessUrl(Long)}
     */
    @Deprecated(since = "6.0.0", forRemoval = false)
    String selectUrlByIds(String ossIds);

    /**
     * 通过 ossId 查询包含访问 URL 的兼容 DTO 列表。
     *
     * <p>兼容接口，不建议新代码优先使用。DTO 中的私有 URL 可能短时失效，且该合同不返回到期时间；
     * 新接口应从业务 owner 获取已授权对象，再调用 {@link #resolveAccessUrl(Long)}。</p>
     *
     * @param ossIds ossId 串，逗号分隔
     * @return 列表
     * @deprecated 仅为旧调用方和 OSS 翻译器保留；新代码使用 {@link #resolveAccessUrl(Long)}
     */
    @Deprecated(since = "6.0.0", forRemoval = false)
    List<OssDTO> selectByIds(String ossIds);

    /**
     * 将一条业务数据保存前后的 OSS 集合协调为真实引用。
     *
     * <p>调用方必须先完成业务授权，并在保存业务数据的同一个动态数据源事务中调用。
     * null 集合按空集合处理；集合中的 ID 必须是正数。</p>
     *
     * @param refType       真实物理表名
     * @param refId         真实业务主键
     * @param previousOssIds 保存前的 OSS ID 集合
     * @param currentOssIds  保存后的 OSS ID 集合
     */
    void reconcileReferences(String refType, String refId,
                             Collection<Long> previousOssIds, Collection<Long> currentOssIds);

    /**
     * 查询对象当前生命周期及反向定位信息。
     */
    OssLifecycleSnapshot snapshot(Long ossId);

    /**
     * 查询对象上传完成后由 OSS 校验并持久化的权威元数据。
     *
     * <p>该方法不返回访问 URL，也不替代业务 owner 的访问授权。旧的第三方实现可以继续加载，
     * 但消费元数据前必须提供覆盖实现。</p>
     */
    default OssObjectMetadata objectMetadata(Long ossId) {
        throw new UnsupportedOperationException("OSS object metadata is not supported by this implementation");
    }

    /**
     * 在调用方完成业务权限校验后解析对象访问地址。
     */
    OssAccessUrl resolveAccessUrl(Long ossId);

    /**
     * 在调用方完成业务权限校验后为私有对象生成默认短时下载授权。
     */
    OssDownloadUrl presignDownload(Long ossId);

    /**
     * 在调用方完成业务权限校验后按服务端命名策略为私有对象生成下载授权。
     */
    OssDownloadUrl presignDownload(Long ossId, String policyName);

    record OssReferenceState(Long ossId, boolean temporary, LocalDateTime expireTime, long referenceCount) {
    }

    record OssReference(String refType, String refId) {
    }

    record OssLifecycleSnapshot(Long ossId, boolean temporary, LocalDateTime expireTime,
                                List<OssReference> references) {
        public OssLifecycleSnapshot {
            references = references == null ? List.of() : List.copyOf(references);
        }
    }

    record OssObjectMetadata(Long ossId, String objectKey, String fileName, String fileSuffix,
                             long fileSize, String contentType, Long uploaderUserId, Long uploaderClientPk) {
        /** 兼容仅提供用户归属的历史测试实现。 */
        public OssObjectMetadata(Long ossId, String objectKey, String fileName, String fileSuffix,
                                 long fileSize, String contentType, Long uploaderUserId) {
            this(ossId, objectKey, fileName, fileSuffix, fileSize, contentType, uploaderUserId, null);
        }
    }

    record OssDownloadUrl(String url, Instant expiresAt, String fileName) {
    }

    record OssAccessUrl(String accessType, String url, @Nullable Instant expiresAt, String fileName) {
    }
}
