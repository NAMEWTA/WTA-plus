package org.namewta.demo.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** 富文本资源授权访问结果。 */
@Data
@Builder
public class TestRichTextAssetVo {
    private String ossId;
    private String status;
    private String url;
    private Instant expiresAt;
    private String fileName;
    private String contentType;
}
