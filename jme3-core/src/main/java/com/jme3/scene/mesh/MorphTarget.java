package com.jme3.scene.mesh;

import com.jme3.export.*;
import com.jme3.scene.VertexBuffer;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Map;

public class MorphTarget implements Savable {
    final private EnumMap<VertexBuffer.Type, FloatBuffer> buffers = new EnumMap<>(VertexBuffer.Type.class);
    private String name = null;
    
    public MorphTarget() {
        
    }
    
    public MorphTarget(String name) {
        this.name = name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

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
        OutputCapsule oc = ex.getCapsule(this);
        for (Map.Entry<VertexBuffer.Type, FloatBuffer> entry : buffers.entrySet()) {
            Buffer roData = entry.getValue().asReadOnlyBuffer();
            oc.write((FloatBuffer) roData, entry.getKey().name(),null);
        }
        oc.write(name, "morphName", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        for (VertexBuffer.Type type : VertexBuffer.Type.values()) {
            FloatBuffer b = ic.readFloatBuffer(type.name(), null);
            if(b!= null){
                setBuffer(type, b);
            }
        }
        name = ic.readString("morphName", null);
    }
}
