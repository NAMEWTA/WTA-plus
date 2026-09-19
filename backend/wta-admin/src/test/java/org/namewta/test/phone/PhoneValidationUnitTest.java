package org.namewta.test.phone;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.namewta.system.api.model.RegisterBody;
import org.namewta.system.domain.bo.SysUserBo;
import org.namewta.system.domain.bo.SysUserProfileBo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class PhoneValidationUnitTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void registrationRequiresPhoneNumber(String phone) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            RegisterBody body = new RegisterBody();
            body.setPhoneNumber(phone);
            assertThat(factory.getValidator().validateProperty(body, "phoneNumber")).isNotEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "12800138000", "13800138000x"})
    void managementRejectsMalformedPhoneAtTransportBoundary(String phone) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            SysUserBo body = new SysUserBo(); body.setPhoneNumber(phone);
            assertThat(factory.getValidator().validateProperty(body, "phoneNumber")).isNotEmpty();
        }
    }

    @Test
    void updateDtosLeaveOmittedPhoneForTheWriteBoundaryToMerge() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(new SysUserBo(), "phoneNumber")).isEmpty();
            assertThat(factory.getValidator().validateProperty(new SysUserProfileBo(), "phoneNumber")).isEmpty();
        }
    }
}
