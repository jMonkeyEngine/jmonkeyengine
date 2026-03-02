package com.jme3.vulkan.mesh.test;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;

public class Vertex extends Struct {

    public final Vector3f position = new Vector3f();
    public final Vector2f texCoord = new Vector2f();
    public final Vector3f normal = new Vector3f();

    public Vertex(StructLayout layout) {
        addFields(new Field(position), new Field(texCoord), new Field(normal));
        setLayout(layout); // stage layout, not calculated immediately
    }



}
