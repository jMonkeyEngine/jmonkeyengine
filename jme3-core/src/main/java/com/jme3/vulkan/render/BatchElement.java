package com.jme3.vulkan.render;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.pipeline.Pipeline;

public interface BatchElement {

    float computeDistanceSq();

    float computeDistance();

    Camera getCamera();

    Geometry getGeometry();

    Material getMaterial();

    Mesh getMesh();

}
