package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.Vector3f;
import com.jme3.vulkan.mesh.AttributeMappingInfo;

import java.nio.ByteBuffer;

public class Position extends AbstractAttribute<Vector3f, Float> {

    public Position(ValueMapper<Float> mapper, AttributeMappingInfo info) {
        super(mapper, info);
    }

    @Override
    protected void copyValueTo(Vector3f src, Vector3f dst) {
        dst.set(src);
    }

    @Override
    public Vector3f createStorageObject(Vector3f store) {
        return Vector3f.storage(store);
    }

    @Override
    public void set(long element, Vector3f value) {
        set(element, value.x, value.y, value.z);
    }

    @Override
    public Vector3f get(long element) {
        return get(element, (Vector3f)null);
    }

    @Override
    public Vector3f get(long element, Vector3f store) {
        store = createStorageObject(store);
        ByteBuffer buf = getBuffer(element);
        return store.set(mapper.get(buf), mapper.get(buf), mapper.get(buf));
    }

    public void set(long element, float x, float y, float z) {
        ByteBuffer buf = getBuffer(element);
        mapper.put(buf, x);
        mapper.put(buf, y);
        mapper.put(buf, z);
    }

    public void set(long startElement, float[] array) {
        for (int i = 0; i < array.length; i += 3) {
            set(startElement++, array[i], array[i + 1], array[i + 2]);
        }
    }

    public static Position float64(AttributeMappingInfo info) {
        return new Position(ValueMapper.Float64, info);
    }

    public static Position float32(AttributeMappingInfo info) {
        return new Position(ValueMapper.Float32, info);
    }

    public static Position float16(AttributeMappingInfo info) {
        return new Position(ValueMapper.Float16, info);
    }

    public static Position float8(AttributeMappingInfo info) {
        return new Position(ValueMapper.Float8, info);
    }

}
