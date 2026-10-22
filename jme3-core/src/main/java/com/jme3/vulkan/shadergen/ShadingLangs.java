package com.jme3.vulkan.shadergen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShadingLangs {

    int glsl() default -1;

    int hlsl() default -1;

    int slang() default -1;

    int spirv() default -1;

}
