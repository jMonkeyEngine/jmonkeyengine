package com.jme3.util.struct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field within a {@link Struct} implementation as being a member of
 * that struct.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Member {

    /**
     * Position of the annotated struct member within the struct.
     *
     * @return struct member position
     */
    int value();

}
