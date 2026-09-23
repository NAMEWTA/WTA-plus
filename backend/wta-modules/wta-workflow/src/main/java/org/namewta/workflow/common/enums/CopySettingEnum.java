package org.namewta.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 抄送设置枚举
 */
@Getter
@AllArgsConstructor
public enum CopySettingEnum implements NodeExtEnum {
    ;

    /**
     * 展示名称。
     */
    private final String label;

    /**
     * 配置值。
     */
    private final String value;

    /**
     * 是否默认选中。
     */
    private final boolean selected;

}

