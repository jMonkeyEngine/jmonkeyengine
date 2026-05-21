package com.jme3.vulkan.render.bucket;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

public interface RenderElement {

    void bind();

    void render();

    Geometry getGeometry();

    Mesh getMesh();

    Material getMaterial();

    long getPipelineSortPosition();

    long getMaterialSortPosition();

    float computeDistanceSq();

    float computeDistance();

}
