package org.namewta.common.core.validation;

import java.util.Map;

/** 单项校验错误，code 可直接映射到 i18n 资源键。 */
public record ValidationIssue(String code, Map<String, Object> args, String field) {
    public ValidationIssue {
        args = args == null ? Map.of() : Map.copyOf(args);
    }

    public ValidationIssue(String code) {
        this(code, Map.of(), null);
    }
}
