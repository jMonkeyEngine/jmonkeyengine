package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.Vector3f;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.Mappable;
import com.jme3.vulkan.mesh.VertexBinding;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Position extends BufferAttribute<Vector3f, FloatBuffer> {

    public Position(VertexBinding binding, GpuBuffer vertices, int size, int offset) {
        super(binding, vertices, size, offset);
    }

    @Override
    public void set(int element, Vector3f value) {
        set(element, value.x, value.y, value.z);
    }

    @Override
    public Vector3f get(int element) {
        return get(element, (Vector3f)null);
    }

    @Override
    public Vector3f get(int element, Vector3f store) {
        store = Vector3f.storage(store);
        ByteBuffer buf = getBuffer(element);
        return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat());
    }

    public void set(int element, float x, float y, float z) {
        getBuffer(element).putFloat(x).putFloat(y).putFloat(z);
    }

    public void set(int baseElement, float[] values) {
        ByteBuffer buf = getBuffer(baseElement);
        for (float v : values) {
            buf.putFloat(v);
        }
    }

    public float[] get(int baseElement, float[] store) {
        ByteBuffer buf = getBuffer(baseElement);
        for (int i = 0; i < store.length; i++) {
            store[i] = buf.getFloat();
        }
        return store;
    }

}
