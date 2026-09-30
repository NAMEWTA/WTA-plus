package org.namewta.workflow.service.impl;

import java.util.function.Supplier;

/** 仅由公开 Java 系统办理入口创建的调用域，浏览器参数不能设置该状态。 */
final class WorkflowTrustedExecution {
    private static final ThreadLocal<Boolean> TRUSTED = new ThreadLocal<>();
    private WorkflowTrustedExecution() { }

    static boolean active() { return Boolean.TRUE.equals(TRUSTED.get()); }

    static <T> T run(Supplier<T> operation) {
        Boolean previous = TRUSTED.get();
        TRUSTED.set(true);
        try {
            return operation.get();
        } finally {
            if (previous == null) TRUSTED.remove();
            else TRUSTED.set(previous);
        }
    }
}
