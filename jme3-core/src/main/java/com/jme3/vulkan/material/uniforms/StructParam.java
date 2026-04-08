package com.jme3.vulkan.material.uniforms;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.buffers.MappableBuffer;

public class StructParam <T extends MappableBuffer, S extends Struct> extends ShaderParam<T> {

    private final S struct;

    public StructParam(S struct) {
        this.struct = struct;
    }

    public StructMapping<S> map() {
        return get().mapAllStructs(struct);
    }

    public S getStruct() {
        return struct;
    }

}
