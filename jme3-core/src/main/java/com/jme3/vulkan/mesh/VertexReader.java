package com.jme3.vulkan.mesh;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public interface VertexReader {

    int capacity();

    int limit();

    byte getByte(int vertex, int component);

    short getShort(int vertex, int component);

    int getInt(int vertex, int component);

    float getFloat(int vertex, int component);

    double getDouble(int vertex, int component);

    long getLong(int vertex, int component);

    default Vector2f getVector2(int vertex, int baseComponent, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default Vector3f getVector3(int vertex, int baseComponent, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default Vector4f getVector4(int vertex, int baseComponent, Vector4f store) {
        if (store == null) {
            store = new Vector4f();
        }
        return store.set(
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent++),
                getFloat(vertex, baseComponent  ));
    }

    default ColorRGBA getColor(int vertex, int baseComponent, ColorRGBA store) {
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
