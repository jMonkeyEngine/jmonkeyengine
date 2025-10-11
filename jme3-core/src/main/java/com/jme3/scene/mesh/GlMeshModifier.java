package com.jme3.scene.mesh;

import com.jme3.scene.GlVertexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.vulkan.mesh.AttributeModifier;
import com.jme3.vulkan.mesh.VertexWriter;

import java.nio.ByteBuffer;

public class GlMeshModifier implements AttributeModifier {

    private final GlVertexBuffer vertices;
    private final ByteBuffer buffer;
    private final int bytesPerComponent;

    public GlMeshModifier(GlVertexBuffer vertices) {
        this.vertices = vertices;
        this.buffer = BufferUtils.interfaceByteBuffer(vertices.getData());
        this.bytesPerComponent = vertices.getStride() / vertices.getNumComponents();
    }

    @Override
    public void close() {}

    public int vertexToPosition(int vertex) {
        return vertex * vertices.getStride();
    }

    public int vertexToPosition(int vertex, int component) {
        return vertex * vertices.getStride() + component * bytesPerComponent;
    }

    public int positionToVertex(int position) {
        return position / vertices.getStride();
    }

    @Override
    public VertexWriter limit(int vertex) {
        buffer.limit(vertexToPosition(vertex));
        return this;
    }

    @Override
    public VertexWriter putByte(int vertex, int component, byte value) {
        buffer.put(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putShort(int vertex, int component, short value) {
        buffer.putShort(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putInt(int vertex, int component, int value) {
        buffer.putInt(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putFloat(int vertex, int component, float value) {
        buffer.putFloat(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putDouble(int vertex, int component, double value) {
        buffer.putDouble(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putLong(int vertex, int component, long value) {
        buffer.putLong(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putNumber(int vertex, int component, Number value) {
        if (value instanceof Byte) {
            putByte(vertex, component, value.byteValue());
        } else if (value instanceof Short) {
            putShort(vertex, component, value.shortValue());
        } else if (value instanceof Integer) {
            putInt(vertex, component, value.intValue());
        } else if (value instanceof Float) {
            putFloat(vertex, component, value.floatValue());
        } else if (value instanceof Double) {
            putDouble(vertex, component, value.doubleValue());
        } else if (value instanceof Long) {
            putLong(vertex, component, value.longValue());
        }
        return this;
    }

    @Override
    public int capacity() {
        return positionToVertex(buffer.position());
    }

    @Override
    public int limit() {
        return positionToVertex(buffer.limit());
    }

    @Override
    public int components() {
        return vertices.getNumComponents();
    }

    @Override
    public byte getByte(int vertex, int component) {
        return buffer.get(vertexToPosition(vertex, component));
    }

    @Override
    public short getShort(int vertex, int component) {
        return buffer.getShort(vertexToPosition(vertex, component));
    }

    @Override
    public int getInt(int vertex, int component) {
        return buffer.getInt(vertexToPosition(vertex, component));
    }

    @Override
    public float getFloat(int vertex, int component) {
        return buffer.getFloat(vertexToPosition(vertex, component));
    }

    @Override
    public double getDouble(int vertex, int component) {
        return buffer.getDouble(vertexToPosition(vertex, component));
    }

    @Override
    public long getLong(int vertex, int component) {
        return buffer.getLong(vertexToPosition(vertex, component));
    }

}
