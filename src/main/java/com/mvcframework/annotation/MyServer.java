package com.mvcframework.annotation;
import java.lang.annotation.*;

@Target(value= {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface MyServer {
    String value() default  "";
}
