package com.hypercube.util.javafx.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Use this annotation on your controller to be sure it is not shared between view instances
 * <p>By default, Spring components are singletons which is not what we want for controllers !</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@Scope("prototype")
public @interface JavaFXSpringController {
    String value() default "";
}
