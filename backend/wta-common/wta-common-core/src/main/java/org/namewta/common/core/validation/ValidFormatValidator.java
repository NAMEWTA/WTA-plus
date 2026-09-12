package org.namewta.common.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidFormat 实现。 */
public final class ValidFormatValidator implements ConstraintValidator<ValidFormat, CharSequence> {
    private ValidationFormat format;

    @Override
    public void initialize(ValidFormat annotation) {
        format = annotation.type();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || ValidationUtils.isValid(value.toString(), format);
    }
}
