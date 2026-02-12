package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.Vector3f;
import com.jme3.vulkan.mesh.AttributeMappingInfo;

import java.nio.ByteBuffer;

public class Position extends AbstractAttribute<Vector3f, Float> {

    public Position(ValueMapper<Float> mapper, AttributeMappingInfo info) {
        super(mapper, info);
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
        return store.set(mapper.get(buf), mapper.get(buf), mapper.get(buf));
    }

    public void set(int element, float x, float y, float z) {
        ByteBuffer buf = getBuffer(element);
        mapper.put(buf, x);
        mapper.put(buf, y);
        mapper.put(buf, z);
    }

    public void set(int startElement, float[] array) {
        for (int i = 0; i < array.length; i += 3) {
            set(startElement++, array[i], array[i + 1], array[i + 2]);
        }
    }

}
