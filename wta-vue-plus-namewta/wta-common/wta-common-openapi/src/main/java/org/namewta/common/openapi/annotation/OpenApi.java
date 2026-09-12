package org.namewta.common.openapi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly exposes one Spring MVC handler to NAMEWTA OpenAPI calls.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenApi {

    /**
     * Stable human-readable operation summary used by the generated catalog.
     */
    String value();

}
