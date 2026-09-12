package org.namewta.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.namewta.common.core.domain.R;
import org.namewta.system.oss.upload.OssUploadContracts.*;
import org.namewta.system.oss.upload.OssUploadException;
import org.namewta.system.oss.upload.OssUploadService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * OSS 浏览器直传 JSON 控制面。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/resource/oss/uploads")
public class SysOssUploadController {

    private final OssUploadService uploadService;

    @SaCheckPermission("system:oss:upload")
    @PostMapping
    public R<InitResponse> init(@Valid @RequestBody InitRequest request) {
        return R.ok(uploadService.init(request));
    }

    @SaCheckPermission("system:oss:upload")
    @PostMapping("/{uploadToken}/parts/sign")
    public R<SignPartsResponse> signParts(@PathVariable String uploadToken,
                                         @Valid @RequestBody SignPartsRequest request) {
        return R.ok(uploadService.signParts(uploadToken, request));
    }

    @SaCheckPermission("system:oss:upload")
    @GetMapping("/{uploadToken}/parts")
    public R<ResumeResponse> parts(@PathVariable String uploadToken,
                                   @NotBlank @RequestParam String fingerprint) {
        return R.ok(uploadService.resume(uploadToken, fingerprint));
    }

    @SaCheckPermission("system:oss:upload")
    @PostMapping("/{uploadToken}/complete")
    public R<String> complete(@PathVariable String uploadToken,
                              @Valid @RequestBody(required = false) CompleteRequest request) {
        return R.data(uploadService.complete(uploadToken, request));
    }

    @SaCheckPermission("system:oss:upload")
    @DeleteMapping("/{uploadToken}")
    public R<Void> abort(@PathVariable String uploadToken) {
        uploadService.abort(uploadToken);
        return R.ok();
    }

    @ExceptionHandler(OssUploadException.class)
    public R<ErrorResponse> handleUploadException(OssUploadException exception) {
        return R.fail(exception.getMessage(), new ErrorResponse(exception.error().name()));
    }
}
