package com.jme3.vulkan.material.uniforms;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.util.IntEnum;

public class StructUniform <T extends MappableBuffer, S extends Struct> extends BufferUniform<T> {

    private final S struct;

    public StructUniform(IntEnum<Descriptor> descriptor, StructLayout layout, S struct, T buffer) {
        super(descriptor, layout, buffer);
        struct.bind(layout);
        this.struct = struct;
    }

    public StructUniform(IntEnum<Descriptor> descriptor, S struct, T buffer) {
        super(descriptor, struct.getLayout(), buffer);
        this.struct = struct;
    }

    public StructMapping<S> map() {
        return get().mapAllStructs(struct);
    }

}
