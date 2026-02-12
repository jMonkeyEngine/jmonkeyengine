package com.jme3.util.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When assigned to a {@link Struct} implementation, this annotation hints that
 * the layout and size of the struct will remain consistent across all instances
 * of that struct forever. This allows for possible optimizations in managing
 * the struct.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Consistent {}
