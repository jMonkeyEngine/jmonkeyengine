package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.vulkan.Format;

import java.nio.*;

public class AttributeModifier implements AutoCloseable {
    
    private final VertexBuffer vertex;
    private final VertexAttribute attribute;
    private ByteBuffer buffer;

    public AttributeModifier(VertexBuffer vertex, VertexAttribute attribute) {
        this.vertex = vertex;
        this.attribute = attribute;
    }

    protected AttributeModifier map() {
        buffer = vertex.mapBytes();
        return this;
    }

    @Override
    public void close() {
        vertex.unmap();
    }
    
    public int transformPosition(int vertex) {
        return attribute.getBinding().getStride() * vertex + attribute.getOffset();
    }

    public AttributeModifier putByte(int vertex, int component, byte value) {
        attribute.getFormat().getComponent(component).putByte(buffer, transformPosition(vertex), value);
        return this;
    }

    public AttributeModifier putShort(int vertex, int component, short value) {
        attribute.getFormat().getComponent(component).putShort(buffer, transformPosition(vertex), value);
        return this;
    }

    public AttributeModifier putInt(int vertex, int component, int value) {
        attribute.getFormat().getComponent(component).putInt(buffer, transformPosition(vertex), value);
        return this;
    }

    public AttributeModifier putFloat(int vertex, int component, float value) {
        attribute.getFormat().getComponent(component).putFloat(buffer, transformPosition(vertex), value);
        return this;
    }

    public AttributeModifier putDouble(int vertex, int component, double value) {
        attribute.getFormat().getComponent(component).putDouble(buffer, transformPosition(vertex), value);
        return this;
    }

    public AttributeModifier putLong(int vertex, int component, long value) {
        attribute.getFormat().getComponent(component).putLong(buffer, transformPosition(vertex), value);
        return this;
    }

    public AttributeModifier putVector2(int vertex, int baseComponent, Vector2f value) {
        return putVector2(vertex, baseComponent, value.x, value.y);
    }

    public AttributeModifier putVector2(int vertex, int baseComponent, float x, float y) {
        vertex = transformPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, x);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, y);
        return this;
    }

    public AttributeModifier putVector3(int vertex, int baseComponent, Vector3f value) {
        return putVector3(vertex, baseComponent, value.x, value.y, value.z);
    }

    public AttributeModifier putVector3(int vertex, int baseComponent, float x, float y, float z) {
        vertex = transformPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, x);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, y);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, z);
        return this;
    }

    public AttributeModifier putVector4(int vertex, int baseComponent, Vector4f value) {
        return putVector4(vertex, baseComponent, value.x, value.y, value.z, value.w);
    }

    public AttributeModifier putVector4(int vertex, int baseComponent, float x, float y, float z, float w) {
        vertex = transformPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, x);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, y);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, z);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, w);
        return this;
    }

    public AttributeModifier putColor(int vertex, int baseComponent, ColorRGBA value) {
        return putVector4(vertex, baseComponent, value.r, value.g, value.b, value.a);
    }

    public AttributeModifier putBytes(int baseVertex, int baseComponent, byte[] values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        for (byte v : values) {
            f.getComponent(baseComponent).putByte(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        return this;
    }

    public AttributeModifier putBytes(int baseVertex, int baseComponent, ByteBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putByte(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    public AttributeModifier putShorts(int baseVertex, int baseComponent, short[] values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        for (short v : values) {
            f.getComponent(baseComponent).putShort(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        return this;
    }

    public AttributeModifier putShorts(int baseVertex, int baseComponent, ShortBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putShort(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    public AttributeModifier putInts(int baseVertex, int baseComponent, int[] values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        for (int v : values) {
            f.getComponent(baseComponent).putInt(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        return this;
    }

    public AttributeModifier putInts(int baseVertex, int baseComponent, IntBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putInt(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    public AttributeModifier putFloats(int baseVertex, int baseComponent, float[] values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        for (float v : values) {
            f.getComponent(baseComponent).putFloat(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        return this;
    }

    public AttributeModifier putFloats(int baseVertex, int baseComponent, FloatBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putFloat(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    public AttributeModifier putDoubles(int baseVertex, int baseComponent, double[] values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        for (double v : values) {
            f.getComponent(baseComponent).putDouble(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        return this;
    }

    public AttributeModifier putDoubles(int baseVertex, int baseComponent, DoubleBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putDouble(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    public AttributeModifier putLongs(int baseVertex, int baseComponent, long[] values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        for (long v : values) {
            f.getComponent(baseComponent).putLong(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        return this;
    }

    public AttributeModifier putLongs(int baseVertex, int baseComponent, LongBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = transformPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putLong(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = transformPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    public byte getByte(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getByte(buffer, transformPosition(vertex));
    }

    public short getShort(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getShort(buffer, transformPosition(vertex));
    }

    public int getInt(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getInt(buffer, transformPosition(vertex));
    }

    public float getFloat(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getFloat(buffer, transformPosition(vertex));
    }

    public double getDouble(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getDouble(buffer, transformPosition(vertex));
    }

    public long getLong(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getLong(buffer, transformPosition(vertex));
    }

    public Vector2f getVector2(int vertex, int baseComponent, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    public Vector3f getVector3(int vertex, int baseComponent, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    public Vector4f getVector4(int vertex, int baseComponent, Vector4f store) {
        if (store == null) {
            store = new Vector4f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    public ColorRGBA getColor(int vertex, int baseComponent, ColorRGBA store) {
        if (store == null) {
            store = new ColorRGBA();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

}
