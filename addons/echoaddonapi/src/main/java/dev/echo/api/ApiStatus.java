package dev.echo.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.PACKAGE,
        ElementType.ANNOTATION_TYPE
})
public @interface ApiStatus {
    Stability value();

    String since() default "";

    String owner() default "";

    String note() default "";

    enum Stability {
        STABLE,
        BETA,
        INTERNAL,
        TEST_ONLY,
        DEPRECATED
    }
}
