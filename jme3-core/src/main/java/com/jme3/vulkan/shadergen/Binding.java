package com.jme3.vulkan.shadergen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Binding {

    int set() default 0;

    /**
     * Binding index to reserve, or -1 to auto-assign the next free index within
     * {@link #set()}, the same way unannotated resources are assigned a binding in
     * slang shaders.
     */
    int binding() default -1;

}
