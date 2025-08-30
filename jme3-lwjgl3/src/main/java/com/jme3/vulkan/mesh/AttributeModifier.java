package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.vulkan.Format;

import java.nio.ByteBuffer;

public class AttributeModifier implements AutoCloseable {
    
    private final VertexAttribute attribute;
    private final ByteBuffer buffer;

    public AttributeModifier(VertexAttribute attribute) {
        this.attribute = attribute;
        this.buffer = attribute.getBinding().map();
    }

    @Override
    public void close() {
        attribute.getBinding().unmap();
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
        vertex = transformPosition(vertex);
        attribute.getFormat().getComponent(baseComponent++).putFloat(buffer, vertex, value.x);
        attribute.getFormat().getComponent(baseComponent  ).putFloat(buffer, vertex, value.y);
        return this;
    }

    public AttributeModifier putVector3(int vertex, int baseComponent, Vector3f value) {
        vertex = transformPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.x);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.y);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, value.z);
        return this;
    }

    public AttributeModifier putVector4(int vertex, int baseComponent, Vector4f value) {
        vertex = transformPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.x);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.y);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.z);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, value.w);
        return this;
    }

    public AttributeModifier putColor(int vertex, int baseComponent, ColorRGBA value) {
        vertex = transformPosition(vertex);
        Format f = attribute.getFormat();
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.r);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.g);
        f.getComponent(baseComponent++).putFloat(buffer, vertex, value.b);
        f.getComponent(baseComponent  ).putFloat(buffer, vertex, value.a);
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
