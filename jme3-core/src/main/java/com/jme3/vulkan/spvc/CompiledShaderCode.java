package com.jme3.vulkan.spvc;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface CompiledShaderCode {

    ByteBuffer getCompiledCode();

    Collection<ShaderResource> getResourcesForType(SpvcCompiler.ResourceType type);

    Collection<EntryPoint> getEntryPoints();

    default EntryPoint getEntryPoint(String name) {
        for (EntryPoint p : getEntryPoints()) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

}
