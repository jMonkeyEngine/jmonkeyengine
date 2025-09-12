package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

import java.nio.*;

public interface VertexWriter {

    int capacity();

    int limit();

    VertexWriter limit(int vertex);

    VertexWriter putByte(int vertex, int component, byte value);

    VertexWriter putShort(int vertex, int component, short value);

    VertexWriter putInt(int vertex, int component, int value);

    VertexWriter putFloat(int vertex, int component, float value);

    VertexWriter putDouble(int vertex, int component, double value);

    VertexWriter putLong(int vertex, int component, long value);

    VertexWriter putVector2(int vertex, int baseComponent, float x, float y);

    VertexWriter putVector3(int vertex, int baseComponent, float x, float y, float z);

    VertexWriter putVector4(int vertex, int baseComponent, float x, float y, float z, float w);

    default VertexWriter putVector2(int vertex, int baseComponent, Vector2f value) {
        return putVector2(vertex, baseComponent, value.x, value.y);
    }

    default VertexWriter putVector3(int vertex, int baseComponent, Vector3f value) {
        return putVector3(vertex, baseComponent, value.x, value.y, value.z);
    }

    default VertexWriter putVector4(int vertex, int baseComponent, Vector4f value) {
        return putVector4(vertex, baseComponent, value.x, value.y, value.z, value.w);
    }

    default VertexWriter putColor(int vertex, int baseComponent, ColorRGBA value) {
        return putVector4(vertex, baseComponent, value.r, value.g, value.b, value.a);
    }

    VertexWriter putBytes(int baseVertex, int baseComponent, byte... values);

    VertexWriter putBytes(int baseVertex, int baseComponent, ByteBuffer values);

    VertexWriter putShorts(int baseVertex, int baseComponent, short... values);

    VertexWriter putShorts(int baseVertex, int baseComponent, ShortBuffer values);

    VertexWriter putInts(int baseVertex, int baseComponent, int... values);

    VertexWriter putInts(int baseVertex, int baseComponent, IntBuffer values);

    VertexWriter putFloats(int baseVertex, int baseComponent, float... values);

    VertexWriter putFloats(int baseVertex, int baseComponent, FloatBuffer values);

    VertexWriter putDoubles(int baseVertex, int baseComponent, double... values);

    VertexWriter putDoubles(int baseVertex, int baseComponent, DoubleBuffer values);

    VertexWriter putLongs(int baseVertex, int baseComponent, long... values);

    VertexWriter putLongs(int baseVertex, int baseComponent, LongBuffer values);
    
}
