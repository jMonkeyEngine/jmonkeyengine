package com.jme3.vulkan.shadergen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShadingLangVersion {

    int glsl() default 460;

    int hlsl() default 1;

    int slang() default 1;

}
