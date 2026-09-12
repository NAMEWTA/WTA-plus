package org.namewta.common.core.validation;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** 通用格式校验门面。空值由 @NotBlank 等必填约束负责。 */
public final class ValidationUtils {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern TELEPHONE = Pattern.compile("^(?:0\\d{2,3}-?\\d{7,8}|\\d{7,8})$");
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final Pattern HK_ID = Pattern.compile("^[A-Z]{1,2}\\d{6}\\([0-9A]\\)$");
    private static final Pattern MO_ID = Pattern.compile("^[157]\\d{6}\\([0-9A]\\)$");
    private static final Pattern TW_ID = Pattern.compile("^[A-Z][12]\\d{8}$");
    private static final Pattern HK_MO_PERMIT = Pattern.compile("^8\\d{7}[0-9X]$");
    private static final Pattern TW_PERMIT = Pattern.compile("^[A-Z0-9]{8,10}$");
    private static final Pattern HK_MO_TRAVEL = Pattern.compile("^[HM]\\d{8}$");
    private static final Pattern TW_TRAVEL = Pattern.compile("^[A-Z0-9]{8}$");
    private static final String CREDIT_CHARS = "0123456789ABCDEFGHJKLMNPQRTUWXY";
    private static final int[] CREDIT_WEIGHTS = {1, 3, 9, 7, 1, 3, 9, 7, 1, 3, 9, 7, 1, 3, 9, 7, 1};

    private ValidationUtils() {
    }

    public static ValidationResult validate(String value, ValidationFormat format) {
        if (format == null) return ValidationResult.invalid("validation.format.invalid");
        if (value == null || value.isBlank()) return ValidationResult.ok();
        String normalized = value.strip();
        return switch (format) {
            case EMAIL -> normalized.length() > 254
                ? ValidationResult.invalid("validation.email.invalid")
                : match(normalized, EMAIL, "validation.email.invalid");
            case MAINLAND_MOBILE -> match(normalized, MOBILE, "validation.phone.mobile.invalid");
            case TELEPHONE -> match(normalized, TELEPHONE, "validation.phone.telephone.invalid");
            case E164_PHONE -> match(normalized, E164, "validation.phone.e164.invalid");
            case MAINLAND_ID_CARD -> mainlandIdCard(normalized);
            case HK_RESIDENT_ID -> match(normalized.toUpperCase(), HK_ID, "validation.idCard.hk.invalid");
            case MO_RESIDENT_ID -> match(normalized.toUpperCase(), MO_ID, "validation.idCard.mo.invalid");
            case TW_RESIDENT_ID -> match(normalized.toUpperCase(), TW_ID, "validation.idCard.tw.invalid");
            case HK_MACAO_RESIDENCE_PERMIT -> match(normalized.toUpperCase(), HK_MO_PERMIT, "validation.permit.hkMacao.invalid");
            case TW_RESIDENCE_PERMIT -> match(normalized.toUpperCase(), TW_PERMIT, "validation.permit.tw.invalid");
            case MAINLAND_TRAVEL_PERMIT_HK_MACAO -> match(normalized.toUpperCase(), HK_MO_TRAVEL, "validation.travelPermit.hkMacao.invalid");
            case MAINLAND_TRAVEL_PERMIT_TW -> match(normalized.toUpperCase(), TW_TRAVEL, "validation.travelPermit.tw.invalid");
            case UNIFIED_SOCIAL_CREDIT_CODE -> unifiedCreditCode(normalized.toUpperCase());
        };
    }

    public static boolean isValid(String value, ValidationFormat format) {
        return validate(value, format).valid();
    }

    public static ValidationResult validatePassword(PasswordPolicyContract policy, String password) {
        return PasswordValidation.validate(policy, password);
    }

    public static void requireValid(String value, ValidationFormat format) {
        ValidationResult result = validate(value, format);
        if (!result.valid()) throw new ValidationException(result.violations());
    }

    public static void requirePasswordValid(PasswordPolicyContract policy, String password) {
        ValidationResult result = validatePassword(policy, password);
        if (!result.valid()) throw new ValidationException(result.violations());
    }

    private static ValidationResult match(String value, Pattern pattern, String code) {
        return pattern.matcher(value).matches() ? ValidationResult.ok() : ValidationResult.invalid(code);
    }

    private static ValidationResult mainlandIdCard(String value) {
        if (!value.matches("^\\d{17}[0-9Xx]$")) return ValidationResult.invalid("validation.idCard.mainland.format");
        try {
            LocalDate.of(Integer.parseInt(value.substring(6, 10)), Integer.parseInt(value.substring(10, 12)), Integer.parseInt(value.substring(12, 14)));
        } catch (DateTimeException | NumberFormatException ex) {
            return ValidationResult.invalid("validation.idCard.mainland.date");
        }
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        String checks = "10X98765432";
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += (value.charAt(i) - '0') * weights[i];
        char expected = checks.charAt(sum % 11);
        return Character.toUpperCase(value.charAt(17)) == expected
            ? ValidationResult.ok() : ValidationResult.invalid("validation.idCard.mainland.checksum");
    }

    private static ValidationResult unifiedCreditCode(String value) {
        if (!value.matches("^[0-9A-Z]{18}$")) return ValidationResult.invalid("validation.creditCode.format");
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int index = CREDIT_CHARS.indexOf(value.charAt(i));
            if (index < 0) return ValidationResult.invalid("validation.creditCode.character");
            sum += index * CREDIT_WEIGHTS[i];
        }
        int check = (31 - sum % 31) % 31;
        return CREDIT_CHARS.charAt(check) == value.charAt(17)
            ? ValidationResult.ok() : ValidationResult.invalid("validation.creditCode.checksum");
    }
}
