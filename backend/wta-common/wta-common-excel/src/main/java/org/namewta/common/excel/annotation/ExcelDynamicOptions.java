package org.namewta.common.excel.annotation;

import org.namewta.common.excel.core.ExcelOptionsProvider;

import java.lang.annotation.*;

/**
 * Excel动态下拉选项注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ExcelDynamicOptions {

    /**
     * 提供者类全限定名
     * <p>
     * {@link org.namewta.common.excel.core.ExcelOptionsProvider} 接口实现类 class
     */
    Class<? extends ExcelOptionsProvider> providerClass();
}
