package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Var;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ArraySpec {

    Class<? extends Var> type();

    int[] sizes();

}
