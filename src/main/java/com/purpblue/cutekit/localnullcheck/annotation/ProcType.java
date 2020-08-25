package com.purpblue.cutekit.localnullcheck.annotation;

/**
 * Process strategy
 * @author Purpblue
 */
public enum ProcType {
    /** throw NullPointerException */
    THROW_EXCEPTION,
    /**
     * Invoke a public no arg constructor, must be indicated explicitly in Indicator.newClass
     */
    NEW
}
