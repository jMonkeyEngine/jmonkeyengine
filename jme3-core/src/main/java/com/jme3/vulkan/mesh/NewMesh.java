package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.export.Savable;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.commands.CommandBuffer;

public interface NewMesh extends Collidable, Savable {

    void bind(CommandBuffer cmd);

    void draw(CommandBuffer cmd);

    AttributeModifier modifyAttribute(String name);

    int getVertexCount();

    int getTriangleCount();

    int collideWith(Collidable other, Geometry geometry, CollisionResults results);

    void updateBound();

    void setBound(BoundingVolume volume);

    BoundingVolume getBound();

    int getNumLodLevels();

    default AttributeModifier modifyAttribute(BuiltInAttribute name) {
        return modifyAttribute(name.getName());
    }

}
