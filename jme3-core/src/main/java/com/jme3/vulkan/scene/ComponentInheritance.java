package com.jme3.vulkan.scene;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables or disables inheritance for an annotated {@link SceneComponent} type.
 * Types that are not annotated default to {@link TypeInheritHint#PullOnNull}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ComponentInheritance {

    /**
     * Allows inheritance for the annotated component type.
     *
     * @return true to enable inheritance
     */
    boolean allow() default true;

    /**
     * Inheritance mode to be used for null components.
     *
     * @return mode on null
     */
    InheritMode onNull() default InheritMode.Pull;

}
