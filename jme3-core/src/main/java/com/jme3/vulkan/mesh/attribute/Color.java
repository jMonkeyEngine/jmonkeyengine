package com.jme3.vulkan.mesh.attribute;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector4f;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.mesh.VertexBinding;

import java.nio.ByteBuffer;

public class Color extends AbstractAttribute<ColorRGBA, Float> {

    public Color(ValueMapper<Float> mapper, VertexBinding binding, MappableBuffer vertices, int size, int offset) {
        super(mapper, binding, vertices, size, offset);
    }

    @Override
    public void set(int element, ColorRGBA value) {
        set(element, value.r, value.g, value.b, value.a);
    }

    @Override
    public ColorRGBA get(int element) {
        return get(element, (ColorRGBA)null);
    }

    @Override
    public ColorRGBA get(int element, ColorRGBA store) {
        store = ColorRGBA.storage(store);
        ByteBuffer buf = getBuffer(element);
        store.r = mapper.get(buf);
        store.g = mapper.get(buf);
        store.b = mapper.get(buf);
        store.a = mapper.get(buf);
        return store;
    }

    public void set(int element, float r, float g, float b, float a) {
        ByteBuffer buf = getBuffer(element);
        mapper.put(buf, r);
        mapper.put(buf, g);
        mapper.put(buf, b);
        mapper.put(buf, a);
    }

    public void set(int startElement, float[] array) {
        for (int i = 0; i < array.length; i += 4, startElement++) {
            set(startElement, array[i], array[i + 1], array[i + 2], array[i + 3]);
        }
    }

    public void set(int element, Vector4f value) {
        set(element, value.x, value.y, value.z, value.w);
    }

    public void set(int element, Quaternion value) {
        set(element, value.getX(), value.getY(), value.getW(), value.getZ());
    }

    public Vector4f get(int element, Vector4f store) {
        store = Vector4f.storage(store);
        ByteBuffer buf = getBuffer(element);
        store.x = mapper.get(buf);
        store.y = mapper.get(buf);
        store.z = mapper.get(buf);
        store.w = mapper.get(buf);
        return store;
    }

    public Quaternion get(int element, Quaternion store) {
        store = Quaternion.storage(store);
        ByteBuffer buf = getBuffer(element);
        store.set(mapper.get(buf), mapper.get(buf), mapper.get(buf), mapper.get(buf));
        return store;
    }

    public static Color float64(VertexBinding binding, MappableBuffer vertices, int size, int offset) {
        return new Color(ValueMapper.Float64, binding, vertices, size, offset);
    }

    public static Color float32(VertexBinding binding, MappableBuffer vertices, int size, int offset) {
        return new Color(ValueMapper.Float32, binding, vertices, size, offset);
    }

    public static Color float16(VertexBinding binding, MappableBuffer vertices, int size, int offset) {
        return new Color(ValueMapper.Float16, binding, vertices, size, offset);
    }

    public static Color float8(VertexBinding binding, MappableBuffer vertices, int size, int offset) {
        return new Color(ValueMapper.Float8, binding, vertices, size, offset);
    }

}
