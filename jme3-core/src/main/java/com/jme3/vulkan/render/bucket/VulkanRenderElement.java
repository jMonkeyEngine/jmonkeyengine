package com.jme3.vulkan.render.bucket;

import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.pipeline.VertexPipeline;

public abstract class VulkanRenderElement implements RenderElement {

    private final Camera camera;
    private final Geometry geometry;
    private final VulkanMesh mesh;
    private final VulkanMaterial material;
    private float distance = Float.NaN;
    private float distanceSq = Float.NaN;

    public VulkanRenderElement(Camera camera, Geometry geometry) {
        this.camera = camera;
        this.geometry = geometry;
        this.mesh = (VulkanMesh)geometry.getMesh();
        this.material = (VulkanMaterial)geometry.getMaterial();
    }

    @Override
    public float computeDistanceSq() {
        if (!Float.isNaN(distanceSq)) return distanceSq;
        return distanceSq = camera.getLocation().distanceSquared(geometry.getWorldTranslation());
    }

    @Override
    public float computeDistance() {
        if (!Float.isNaN(distance)) return distance;
        return distance = FastMath.sqrt(computeDistanceSq());
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public VulkanMesh getMesh() {
        return mesh;
    }

    @Override
    public VulkanMaterial getMaterial() {
        return material;
    }

    @Override
    public long getMaterialSortPosition() {
        return 0;
    }

}
