package org.namewta.system.oss.upload;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 浏览器直传的部署要求说明；启动时不访问远端 Bucket。
 */
@Component
public class OssUploadDiagnostics {

    public List<String> requirements() {
        return List.of(
            "Bucket CORS 必须只允许明确的前端 Origin 和 PUT 方法",
            "Bucket CORS 必须向浏览器暴露 ETag 响应头",
            "Bucket Lifecycle 应配置 AbortIncompleteMultipartUpload 作为应用清理兜底"
        );
    }

}
