package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.commands.CommandBuffer;

public interface NewMesh extends Collidable {

    void bind(CommandBuffer cmd);

    void draw(CommandBuffer cmd);

    int getVertexCount();

    int getTriangleCount();

    int collideWith(Collidable other, Geometry geometry, CollisionResults results);

    BoundingVolume getBound();

}
