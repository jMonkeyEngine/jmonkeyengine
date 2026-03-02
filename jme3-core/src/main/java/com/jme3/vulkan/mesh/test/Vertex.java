package com.jme3.vulkan.mesh.test;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;

public class Vertex extends Struct {

    private final Field<Vector3f> position = new Field<>(new Vector3f());
    private final Field<Vector2f> texCoord = new Field<>(new Vector2f());
    private final Field<Vector3f> normal = new Field<>(new Vector3f());

    public Vertex() {
        addFields(position, texCoord, normal);
    }

    public void bind(StructLayout layout, long address) {
        for (Field f : fields) {
            f.bind(layout, address);
        }
    }

    public void setPosition(Vector3f position) {
        updateLayout();
        this.position.set(address, position);
    }

    public void setPosition(float x, float y, float z) {
        position.set(position.getAlias().set(x, y, z));
    }

    public Vector3f getPosition() {
        return position.get(address);
    }

    public void setTexCoord(Vector2f texCoord) {
        this.texCoord.set(address, texCoord);
    }

    public void setTexCoord(float x, float y) {
        texCoord.set(address, texCoord.getAlias().set(x, y));
    }

    public Vector2f getTexCoord() {
        return texCoord.get(address);
    }

}
