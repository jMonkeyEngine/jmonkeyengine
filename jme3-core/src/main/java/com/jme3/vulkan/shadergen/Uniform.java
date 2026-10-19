package com.jme3.vulkan.shadergen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an initialized {@link com.jme3.vulkan.shadergen.vars.StructVar} instance field
 * as a uniform buffer, declared and assigned to a descriptor set the same way
 * {@link Storage} declares a shader storage buffer. Combine with {@link Binding} to
 * reserve a specific set/binding and {@link Layout} to pick a layout other than
 * std140; both are optional, as is {@link Binding} itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Uniform {
}
