package org.namewta.profile.enterprise.controller.anonymous;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.namewta.profile.enterprise.domain.verification.EnterpriseProviderCallbackEnvelope;
import org.namewta.profile.enterprise.domain.verification.EnterpriseVerificationCallbackOutcome;
import org.namewta.profile.enterprise.support.EnterpriseVerificationTimeSource;
import org.namewta.profile.enterprise.usecase.EnterpriseVerificationUseCase;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EnterpriseVerificationAnonymousController HTTP 接口，负责参数校验和响应包装。
 */
@Validated
@RestController
@RequestMapping("/profile/enterprise/verification/providers")
public class EnterpriseVerificationAnonymousController {

    private final EnterpriseVerificationUseCase useCase;
    private final EnterpriseVerificationTimeSource timeSource;

    /**
     * 处理 EnterpriseVerificationAnonymousController HTTP 请求。
     */
    public EnterpriseVerificationAnonymousController(EnterpriseVerificationUseCase useCase,
                                                     EnterpriseVerificationTimeSource timeSource) {
        this.useCase = useCase;
        this.timeSource = timeSource;
    }

    /**
     * 处理 callback HTTP 请求。
     */
    @SaIgnore
    @Log(title = "企业认证供应商回调", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{providerCode}/callback")
    public R<EnterpriseVerificationCallbackOutcome> callback(
        @PathVariable String providerCode,
        @Valid @RequestBody CallbackRequest request) {
        EnterpriseVerificationCallbackOutcome outcome = useCase.callback(
            providerCode,
            new EnterpriseProviderCallbackEnvelope(
                request.providerRequestId(),
                request.timestampEpochSecond(),
                request.payload(),
                request.signature()),
            timeSource.now());
        return R.ok(outcome);
    }

    /**
     * CallbackRequest HTTP 接口，负责参数校验和响应包装。
     */
    public record CallbackRequest(
        @NotBlank @Size(max = 128) String providerRequestId,
        @Positive long timestampEpochSecond,
        @NotBlank @Size(max = 1048576) String payload,
        @NotBlank @Size(max = 1024) String signature
    ) {
    }
}
