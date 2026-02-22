package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.Vector4f;
import com.jme3.vulkan.mesh.AttributeMappingInfo;

import java.nio.ByteBuffer;

public class Tangent extends AbstractAttribute<Vector4f, Float> {

    public Tangent(ValueMapper<Float> mapper, AttributeMappingInfo info) {
        super(mapper, info);
    }

    @Override
    protected void copyValueTo(Vector4f src, Vector4f dst) {
        dst.set(src);
    }

    @Override
    public void set(long element, Vector4f value) {
        set(element, value.x, value.y, value.z, value.w);
    }

    @Override
    public Vector4f get(long element) {
        return get(element, (Vector4f)null);
    }

    @Override
    public Vector4f get(long element, Vector4f store) {
        ByteBuffer buf = getBuffer(element);
        return Vector4f.storage(store).set(mapper.get(buf), mapper.get(buf), mapper.get(buf), mapper.get(buf));
    }

    public void set(long element, float x, float y, float z, float w) {
        ByteBuffer buf = getBuffer(element);
        mapper.put(buf, x);
        mapper.put(buf, y);
        mapper.put(buf, z);
        mapper.put(buf, w);
    }

    public void set(long startElement, float[] array) {
        for (int i = 0; i < array.length; i += 4) {
            set(startElement++, array[i], array[i + 1], array[i + 2], array[i + 3]);
        }
    }

}
