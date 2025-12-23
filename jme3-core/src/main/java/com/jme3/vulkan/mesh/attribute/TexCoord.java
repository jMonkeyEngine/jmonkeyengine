package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.Vector2f;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.mesh.VertexBinding;

import java.nio.ByteBuffer;

public class TexCoord extends AbstractAttribute<Vector2f> {

    public TexCoord(VertexBinding binding, GpuBuffer vertices, int size, int offset) {
        super(binding, vertices, size, offset);
    }

    @Override
    public void set(int element, Vector2f value) {
        set(element, value.x, value.y);
    }

    @Override
    public Vector2f get(int element) {
        return get(element, (Vector2f)null);
    }

    @Override
    public Vector2f get(int element, Vector2f store) {
        store = Vector2f.storage(store);
        ByteBuffer buf = getBuffer(element);
        store.x = buf.getFloat();
        store.y = buf.getFloat();
        return store;
    }

    public void set(int element, float x, float y) {
        getBuffer(element).putFloat(x).putFloat(y);
    }

}
