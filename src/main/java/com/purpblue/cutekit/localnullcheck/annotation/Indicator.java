package com.purpblue.cutekit.localnullcheck.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a method is annotated by NullCheck, you can use this annotation
 * to annotate a new declared local variable, indicating the process strategy
 * and new class(when ProcType is NEW).
 * @author Purpblue
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.LOCAL_VARIABLE)
public @interface Indicator {

    ProcType procType() default ProcType.THROW_EXCEPTION;

    /**
     * Exception info, the parameter s of new NullPointerException(String s).
     * At compile time, when procType == ProcType.THROW_EXCEPTION and exInfo.length==0,
     * the apt will use "xxx:yyy cannot be null" as the NPE parameter.
     * Here "xxx" is the declared type of the local variable, and "yyy" is the name.
     */
    String exInfo() default "";

    /**
     * If procType == ProcType.NEW, you must indicate the new class type,
     * here Class.class is only a placeholder.
     */
    Class<?> newClass() default Class.class;

}
