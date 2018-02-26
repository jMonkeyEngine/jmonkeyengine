package com.jme3.scene.mesh;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.scene.VertexBuffer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.EnumMap;

public class MorphTarget implements Savable {
    private EnumMap<VertexBuffer.Type, FloatBuffer> buffers = new EnumMap<>(VertexBuffer.Type.class);

    public void setBuffer(VertexBuffer.Type type, FloatBuffer buffer) {
        buffers.put(type, buffer);
    }

    public FloatBuffer getBuffer(VertexBuffer.Type type) {
        return buffers.get(type);
    }

    public EnumMap<VertexBuffer.Type, FloatBuffer> getBuffers() {
        return buffers;
    }

    public int getNumBuffers() {
        return buffers.size();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {

    }

    @Override
    public void read(JmeImporter im) throws IOException {

    }
}
