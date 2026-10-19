package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.material.shader.ShaderStage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EntryPoint {

    ShaderStage value();

}
