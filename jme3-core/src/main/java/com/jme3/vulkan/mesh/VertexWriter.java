package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

import java.nio.*;

public interface VertexWriter extends VertexReader {

    VertexWriter limit(int vertex);

    VertexWriter putByte(int vertex, int component, byte value);

    VertexWriter putShort(int vertex, int component, short value);

    VertexWriter putInt(int vertex, int component, int value);

    VertexWriter putFloat(int vertex, int component, float value);

    VertexWriter putDouble(int vertex, int component, double value);

    VertexWriter putLong(int vertex, int component, long value);

    VertexWriter putNumber(int vertex, int component, Number number);

    default VertexWriter putVector2(int vertex, int baseComponent, float x, float y) {
        putFloat(vertex, baseComponent++, x);
        putFloat(vertex, baseComponent  , y);
        return this;
    }

    default VertexWriter putVector3(int vertex, int baseComponent, float x, float y, float z) {
        putFloat(vertex, baseComponent++, x);
        putFloat(vertex, baseComponent++, y);
        putFloat(vertex, baseComponent  , z);
        return this;
    }

    default VertexWriter putVector4(int vertex, int baseComponent, float x, float y, float z, float w) {
        putFloat(vertex, baseComponent++, x);
        putFloat(vertex, baseComponent++, y);
        putFloat(vertex, baseComponent++, z);
        putFloat(vertex, baseComponent  , w);
        return this;
    }

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
    
    default VertexWriter putBytes(int baseVertex, int baseComponent, byte... values) {
        for (byte v : values) {
            putByte(baseVertex, baseComponent, v);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putBytes(int baseVertex, int baseComponent, ByteBuffer values) {
        for (int i = 0; i < values.limit(); i++) {
            putByte(baseVertex, baseComponent, values.get(i));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putShorts(int baseVertex, int baseComponent, short... values) {
        for (short v : values) {
            putShort(baseVertex, baseComponent, v);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putShorts(int baseVertex, int baseComponent, ShortBuffer values) {
        for (int i = 0; i < values.limit(); i++) {
            putShort(baseVertex, baseComponent, values.get(i));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putInts(int baseVertex, int baseComponent, int... values) {
        for (int v : values) {
            putInt(baseVertex, baseComponent, v);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putInts(int baseVertex, int baseComponent, IntBuffer values) {
        for (int i = 0; i < values.limit(); i++) {
            putInt(baseVertex, baseComponent, values.get(i));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putFloats(int baseVertex, int baseComponent, float... values) {
        for (float v : values) {
            putFloat(baseVertex, baseComponent, v);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putFloats(int baseVertex, int baseComponent, FloatBuffer values) {
        for (int i = 0; i < values.limit(); i++) {
            putFloat(baseVertex, baseComponent, values.get(i));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putDoubles(int baseVertex, int baseComponent, double... values) {
        for (double v : values) {
            putDouble(baseVertex, baseComponent, v);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putDoubles(int baseVertex, int baseComponent, DoubleBuffer values) {
        for (int i = 0; i < values.limit(); i++) {
            putDouble(baseVertex, baseComponent, values.get(i));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putLongs(int baseVertex, int baseComponent, long... values) {
        for (long v : values) {
            putLong(baseVertex, baseComponent, v);
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }

    default VertexWriter putLongs(int baseVertex, int baseComponent, LongBuffer values) {
        for (int i = 0; i < values.limit(); i++) {
            putLong(baseVertex, baseComponent, values.get(i));
            if (++baseComponent >= components()) {
                baseComponent = 0;
                baseVertex++;
            }
        }
        return this;
    }
    
}
