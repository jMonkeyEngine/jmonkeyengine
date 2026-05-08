package com.jme3.vulkan.spvc;

public interface ShaderResource {

    String getName();

    SpvcCompiler.BaseType getBaseType();

    SpvcCompiler.ResourceType getType();

    int getDecoration(SpvcCompiler.Decoration type);

}
