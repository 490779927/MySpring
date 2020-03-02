package com.mvcframework.annotation;

import java.lang.annotation.*;

@Target(value= {ElementType.TYPE,ElementType.METHOD,ElementType.PARAMETER})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParm {
    String value() default "";
}
