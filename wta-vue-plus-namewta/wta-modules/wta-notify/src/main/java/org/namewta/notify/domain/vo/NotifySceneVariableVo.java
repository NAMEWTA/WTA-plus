package org.namewta.notify.domain.vo;

import lombok.Data;

/**
 * 场景变量契约，只读展示。
 */
@Data
public class NotifySceneVariableVo {
    private String name;
    private boolean required;
    private String example;
    private String description;
}
