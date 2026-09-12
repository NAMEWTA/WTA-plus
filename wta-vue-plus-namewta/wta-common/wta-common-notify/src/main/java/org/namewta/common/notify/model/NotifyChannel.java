package org.namewta.common.notify.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 规范化的通知渠道标识，允许插件声明自定义渠道。
 *
 * @param value 渠道名称
 */
public record NotifyChannel(String value) {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z][a-z0-9_-]{0,31}");

    public static final NotifyChannel MAIL = new NotifyChannel("mail");
    public static final NotifyChannel SMS = new NotifyChannel("sms");

    public NotifyChannel {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("通知渠道不能为空");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("通知渠道必须以小写字母开头，且只能包含小写字母、数字、下划线或连字符，长度不超过 32");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NotifyChannel of(String value) {
        return new NotifyChannel(value);
    }

    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
