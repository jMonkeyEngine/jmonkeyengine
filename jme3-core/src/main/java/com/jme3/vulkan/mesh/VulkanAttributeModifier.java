package com.jme3.vulkan.mesh;

import com.jme3.vulkan.Format;

import java.nio.*;

public class VulkanAttributeModifier implements AttributeModifier {

    private final VertexBuffer vertex;
    private final VertexAttribute attribute;
    private final ByteBuffer buffer;

    public VulkanAttributeModifier(VertexBuffer vertex, VertexAttribute attribute) {
        this.vertex = vertex;
        this.attribute = attribute;
        buffer = vertex.mapBytes();
    }

    @Override
    public void close() {
        vertex.unmap();
    }

    public int vertexToPosition(int vertex) {
        return attribute.getBinding().getStride() * vertex + attribute.getOffset();
    }

    public int positionToVertex(int position) {
        return (position - attribute.getOffset()) / attribute.getBinding().getStride();
    }

    @Override
    public int capacity() {
        return buffer.capacity() / attribute.getBinding().getStride();
    }

    @Override
    public int limit() {
        return positionToVertex(buffer.limit());
    }

    @Override
    public int components() {
        return attribute.getFormat().getNumComponents();
    }

    @Override
    public AttributeModifier limit(int vertex) {
        buffer.limit(vertexToPosition(vertex));
        return this;
    }

    @Override
    public AttributeModifier putByte(int vertex, int component, byte value) {
        attribute.getFormat().getComponent(component).putByte(buffer, vertexToPosition(vertex), value);
        return this;
    }

    @Override
    public AttributeModifier putShort(int vertex, int component, short value) {
        attribute.getFormat().getComponent(component).putShort(buffer, vertexToPosition(vertex), value);
        return this;
    }

    @Override
    public AttributeModifier putInt(int vertex, int component, int value) {
        attribute.getFormat().getComponent(component).putInt(buffer, vertexToPosition(vertex), value);
        return this;
    }

    @Override
    public AttributeModifier putFloat(int vertex, int component, float value) {
        attribute.getFormat().getComponent(component).putFloat(buffer, vertexToPosition(vertex), value);
        return this;
    }

    @Override
    public AttributeModifier putDouble(int vertex, int component, double value) {
        attribute.getFormat().getComponent(component).putDouble(buffer, vertexToPosition(vertex), value);
        return this;
    }

    @Override
    public AttributeModifier putLong(int vertex, int component, long value) {
        attribute.getFormat().getComponent(component).putLong(buffer, vertexToPosition(vertex), value);
        return this;
    }

    @Override
    public AttributeModifier putNumber(int vertex, int component, Number value) {
        if (value instanceof Byte) {
            return putByte(vertex, component, value.byteValue());
        } else if (value instanceof Short) {
            return putShort(vertex, component, value.shortValue());
        } else if (value instanceof Integer) {
            return putInt(vertex, component, value.intValue());
        } else if (value instanceof Float) {
            return putFloat(vertex, component, value.floatValue());
        } else if (value instanceof Double) {
            return putDouble(vertex, component, value.doubleValue());
        } else if (value instanceof Long) {
            return putLong(vertex, component, value.longValue());
        }
        return this;
    }

    @Override
    public AttributeModifier putVector2(int vertex, int baseComponent, float x, float y) {
        vertex = vertexToPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, x);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, y);
        return this;
    }

    @Override
    public AttributeModifier putVector3(int vertex, int baseComponent, float x, float y, float z) {
        vertex = vertexToPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, x);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, y);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, z);
        return this;
    }

    @Override
    public AttributeModifier putVector4(int vertex, int baseComponent, float x, float y, float z, float w) {
        vertex = vertexToPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, x);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, y);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, z);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, w);
        return this;
    }

    @Override
    public AttributeModifier putBytes(int baseVertex, int baseComponent, byte... values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        for (byte v : values) {
            f.getComponent(baseComponent).putByte(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        return this;
    }

    @Override
    public AttributeModifier putBytes(int baseVertex, int baseComponent, ByteBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putByte(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    @Override
    public AttributeModifier putShorts(int baseVertex, int baseComponent, short... values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        for (short v : values) {
            f.getComponent(baseComponent).putShort(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        return this;
    }

    @Override
    public AttributeModifier putShorts(int baseVertex, int baseComponent, ShortBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putShort(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    @Override
    public AttributeModifier putInts(int baseVertex, int baseComponent, int... values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        for (int v : values) {
            f.getComponent(baseComponent).putInt(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        return this;
    }

    @Override
    public AttributeModifier putInts(int baseVertex, int baseComponent, IntBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putInt(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    @Override
    public AttributeModifier putFloats(int baseVertex, int baseComponent, float... values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        for (float v : values) {
            f.getComponent(baseComponent).putFloat(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        return this;
    }

    @Override
    public AttributeModifier putFloats(int baseVertex, int baseComponent, FloatBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putFloat(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    @Override
    public AttributeModifier putDoubles(int baseVertex, int baseComponent, double... values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        for (double v : values) {
            f.getComponent(baseComponent).putDouble(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        return this;
    }

    @Override
    public AttributeModifier putDoubles(int baseVertex, int baseComponent, DoubleBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putDouble(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    @Override
    public AttributeModifier putLongs(int baseVertex, int baseComponent, long... values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        for (long v : values) {
            f.getComponent(baseComponent).putLong(buffer, vertPos, v);
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        return this;
    }

    @Override
    public AttributeModifier putLongs(int baseVertex, int baseComponent, LongBuffer values) {
        Format f = attribute.getFormat();
        int vertPos = vertexToPosition(baseVertex);
        int bufPos = values.position();
        while (values.hasRemaining()) {
            f.getComponent(baseComponent).putLong(buffer, vertPos, values.get());
            if (++baseComponent >= f.getNumComponents()) {
                baseComponent = 0;
                vertPos = vertexToPosition(++baseVertex);
            }
        }
        values.position(bufPos);
        return this;
    }

    @Override
    public byte getByte(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getByte(buffer, vertexToPosition(vertex));
    }

    @Override
    public short getShort(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getShort(buffer, vertexToPosition(vertex));
    }

    @Override
    public int getInt(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getInt(buffer, vertexToPosition(vertex));
    }

    @Override
    public float getFloat(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getFloat(buffer, vertexToPosition(vertex));
    }

    @Override
    public double getDouble(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getDouble(buffer, vertexToPosition(vertex));
    }

    @Override
    public long getLong(int vertex, int component) {
        return attribute.getFormat().getComponent(component).getLong(buffer, vertexToPosition(vertex));
    }

}
