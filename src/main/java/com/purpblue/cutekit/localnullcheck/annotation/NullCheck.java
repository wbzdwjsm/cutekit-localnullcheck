package com.purpblue.cutekit.localnullcheck.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method to check its local variables that
 * may be null.
 * If you don't want to check some variables, put the Exclude annotation
 * on them.
 * @author Purpblue
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface NullCheck {

}
