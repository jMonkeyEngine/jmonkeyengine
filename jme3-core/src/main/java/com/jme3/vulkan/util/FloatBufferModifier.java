package com.jme3.vulkan.util;

import com.jme3.vulkan.mesh.VertexReader;
import com.jme3.vulkan.mesh.VertexWriter;

import java.nio.FloatBuffer;

public class FloatBufferModifier implements VertexReader, VertexWriter {

    private final FloatBuffer buffer;
    private final int components;

    public FloatBufferModifier(FloatBuffer buffer, int components) {
        this.buffer = buffer;
        this.components = components;
    }

    public int vertexToPosition(int vertex, int component) {
        return vertex * components + component;
    }

    @Override
    public int capacity() {
        return buffer.capacity() / components;
    }

    @Override
    public int limit() {
        return buffer.limit() / components;
    }

    @Override
    public int components() {
        return components;
    }

    @Override
    public VertexWriter limit(int vertex) {
        buffer.limit(vertexToPosition(vertex, 0));
        return this;
    }

    @Override
    public VertexWriter putByte(int vertex, int component, byte value) {
        buffer.put(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putShort(int vertex, int component, short value) {
        buffer.put(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putInt(int vertex, int component, int value) {
        buffer.put(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putFloat(int vertex, int component, float value) {
        buffer.put(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putDouble(int vertex, int component, double value) {
        buffer.put(vertexToPosition(vertex, component), (float)value);
        return this;
    }

    @Override
    public VertexWriter putLong(int vertex, int component, long value) {
        buffer.put(vertexToPosition(vertex, component), value);
        return this;
    }

    @Override
    public VertexWriter putFloats(int baseVertex, int baseComponent, float... values) {
        int p = buffer.position();
        buffer.position(vertexToPosition(baseVertex, baseComponent));
        buffer.put(values);
        buffer.position(p);
        return this;
    }

    @Override
    public VertexWriter putFloats(int baseVertex, int baseComponent, FloatBuffer values) {
        int p = buffer.position();
        buffer.position(vertexToPosition(baseVertex, baseComponent));
        buffer.put(values);
        buffer.position(p);
        return this;
    }

    @Override
    public byte getByte(int vertex, int component) {
        return (byte)buffer.get(vertexToPosition(vertex, component));
    }

    @Override
    public short getShort(int vertex, int component) {
        return (short)buffer.get(vertexToPosition(vertex, component));
    }

    @Override
    public int getInt(int vertex, int component) {
        return (int)buffer.get(vertexToPosition(vertex, component));
    }

    @Override
    public float getFloat(int vertex, int component) {
        return buffer.get(vertexToPosition(vertex, component));
    }

    @Override
    public double getDouble(int vertex, int component) {
        return buffer.get(vertexToPosition(vertex, component));
    }

    @Override
    public long getLong(int vertex, int component) {
        return (long)buffer.get(vertexToPosition(vertex, component));
    }

}
