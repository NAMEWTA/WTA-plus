package org.namewta.system.oss.upload;

/**
 * 上传完成后的 sys_oss 原子登记接缝。
 */
public interface OssUploadMetadataStore {

    Long findByObject(String service, String objectKey);

    Long registerTemporary(OssUploadTicket ticket);
}
