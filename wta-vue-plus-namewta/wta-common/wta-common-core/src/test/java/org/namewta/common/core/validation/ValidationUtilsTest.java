package org.namewta.common.core.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilsTest {
    @Test
    void validatesContactFormats() {
        assertTrue(ValidationUtils.isValid("user@example.com", ValidationFormat.EMAIL));
        assertTrue(ValidationUtils.isValid("13800138000", ValidationFormat.MAINLAND_MOBILE));
        assertFalse(ValidationUtils.isValid("123", ValidationFormat.MAINLAND_MOBILE));
    }

    @Test
    void validatesMainlandIdCardChecksumAndDate() {
        assertTrue(ValidationUtils.isValid("11010519491231002X", ValidationFormat.MAINLAND_ID_CARD));
        assertFalse(ValidationUtils.isValid("110105194912310020", ValidationFormat.MAINLAND_ID_CARD));
        assertFalse(ValidationUtils.isValid("11010519990230002X", ValidationFormat.MAINLAND_ID_CARD));
    }

    @Test
    void validatesUnifiedSocialCreditCodeChecksum() {
        assertTrue(ValidationUtils.isValid("911100001234567897", ValidationFormat.UNIFIED_SOCIAL_CREDIT_CODE));
        assertFalse(ValidationUtils.isValid("91110000123456789X", ValidationFormat.UNIFIED_SOCIAL_CREDIT_CODE));
    }
}
