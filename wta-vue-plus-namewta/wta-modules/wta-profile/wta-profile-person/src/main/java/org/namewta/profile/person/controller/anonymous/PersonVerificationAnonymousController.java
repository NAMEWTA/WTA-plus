package org.namewta.profile.person.controller.anonymous;

import org.namewta.profile.person.domain.verification.PersonProviderCallbackEnvelope;
import org.namewta.profile.person.domain.verification.PersonVerificationCallbackOutcome;
import org.namewta.profile.person.support.PersonVerificationTimeSource;
import org.namewta.profile.person.usecase.PersonVerificationUseCase;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.namewta.common.core.domain.R;
import org.namewta.common.log.annotation.Log;
import org.namewta.common.log.enums.BusinessType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PersonVerificationAnonymousController HTTP 接口，负责参数校验和响应包装。
 */
@Validated
@RestController
@RequestMapping("/profile/person/verification/providers")
public class PersonVerificationAnonymousController {

    private final PersonVerificationUseCase useCase;
    private final PersonVerificationTimeSource timeSource;

    /**
     * 处理 PersonVerificationAnonymousController HTTP 请求。
     */
    public PersonVerificationAnonymousController(PersonVerificationUseCase useCase,
                                                  PersonVerificationTimeSource timeSource) {
        this.useCase = useCase;
        this.timeSource = timeSource;
    }

    /**
     * 处理 callback HTTP 请求。
     */
    @SaIgnore
    @Log(title = "个人认证供应商回调", businessType = BusinessType.OTHER,
        isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{providerCode}/callback")
    public R<PersonVerificationCallbackOutcome> callback(
        @PathVariable String providerCode,
        @Valid @RequestBody CallbackRequest request) {
        PersonVerificationCallbackOutcome outcome = useCase.callback(
            providerCode,
            new PersonProviderCallbackEnvelope(
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
