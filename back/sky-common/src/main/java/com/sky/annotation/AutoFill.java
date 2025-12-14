package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * self defined annotation to find the method to be auto-filled
 */
@Target(ElementType.METHOD) //indicate that the annotation is applicable to methods
@Retention(RetentionPolicy.RUNTIME) // indicate that the annotation is available at runtime
public @interface AutoFill {
    // set the operation type
    OperationType value();
}
