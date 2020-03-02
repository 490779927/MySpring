package com.mvcframework.annotation;

import java.lang.annotation.*;

@Target(value= {ElementType.TYPE,ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestMapping {
    String value() default "";
}
