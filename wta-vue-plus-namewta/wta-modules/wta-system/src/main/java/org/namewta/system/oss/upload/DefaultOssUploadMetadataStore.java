package org.namewta.system.oss.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import lombok.RequiredArgsConstructor;
import org.namewta.system.domain.SysOss;
import org.namewta.system.domain.SysOssExt;
import org.namewta.system.mapper.SysOssMapper;
import org.namewta.system.oss.config.OssLifecycleProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

/**
 * 以 objectKey + service 保证 Complete 重试只登记一个 ossId。
 */
@Service
@RequiredArgsConstructor
public class DefaultOssUploadMetadataStore implements OssUploadMetadataStore {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SysOssMapper mapper;
    private final OssLifecycleProperties lifecycleProperties;

    @Override
    public Long findByObject(String service, String objectKey) {
        SysOss existing = mapper.selectOne(new LambdaQueryWrapper<SysOss>()
            .eq(SysOss::getService, service)
            .eq(SysOss::getFileName, objectKey)
            .last("limit 1"));
        return existing == null ? null : existing.getOssId();
    }

    @Override
    @DSTransactional
    public Long registerTemporary(OssUploadTicket ticket) {
        Long existing = findByObject(ticket.service(), ticket.objectKey());
        if (existing != null) {
            return existing;
        }
        SysOssExt ext = new SysOssExt();
        ext.setFileSize(ticket.fileSize());
        ext.setContentType(ticket.contentType());
        ext.setSource("directUpload");
        ext.setIsTemp(true);
        ext.setUploaderClientPk(ticket.clientPk());
        SysOss oss = new SysOss();
        oss.setFileName(ticket.objectKey());
        oss.setOriginalName(ticket.originalName());
        oss.setFileSuffix(ticket.fileSuffix());
        oss.setUrl("");
        oss.setExt1(JSON.writeValueAsString(ext));
        oss.setService(ticket.service());
        oss.setIsTemp("Y");
        oss.setExpireTime(LocalDateTime.now().plus(lifecycleProperties.getTempRetention()));
        mapper.insert(oss);
        return oss.getOssId();
    }
}
