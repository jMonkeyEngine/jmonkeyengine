package com.jme3.vulkan.render.bucket;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.material.technique.NewTechnique;

public interface BucketElement {

    Geometry getGeometry();

    Mesh getMesh();

    Material getMaterial();

    NewTechnique getTechnique();

    long getTechniqueSortPosition();

    long getMaterialSortPosition();

    float computeDistanceSq();

    float computeDistance();

}
