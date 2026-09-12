package org.namewta.common.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** 通用格式校验注解。 */
@Documented
@Constraint(validatedBy = ValidFormatValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValidFormat {
    ValidationFormat type() default ValidationFormat.EMAIL;

    String message() default "{validation.format.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
