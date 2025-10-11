package com.jme3.vulkan.render;

import com.jme3.material.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.material.VkMaterial;
import com.jme3.vulkan.mesh.VkMesh;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.pipelines.Pipeline;
import com.jme3.vulkan.pipelines.PipelineBindPoint;
import com.jme3.vulkan.pipelines.PipelineCache;
import com.jme3.vulkan.pipelines.newstate.PipelineState;

import java.util.Comparator;

public class VulkanGeometryBatch extends GeometryBatch<VulkanGeometryBatch.Element> {

    private final PipelineCache cache;
    private PipelineState overrideState = null;

    public VulkanGeometryBatch(Camera camera, PipelineCache cache, Comparator<Element> comparator) {
        super(camera, comparator);
        this.cache = cache;
    }

    @Override
    public void render(CommandBuffer cmd) {
        Pipeline currentPipeline = null;
        VkMaterial currentMaterial = null;
        VkMesh currentMesh = null;
        for (Element e : queue) {
            // I'm not sure if it's safe or optimal to allow controls to muck with
            // geometry/material/mesh at this point, but this would technically
            // be the location to do this.
            //e.getGeometry().runControlRender(rm, cmd, camera);

            // Provide transform matrices to the material. This requires
            // that the appropriate buffers be updated on or after this point.
            e.getGeometry().updateMatrixTransforms(camera);

            // bind the selected pipeline if necessary
            if (e.getPipeline() != currentPipeline) {
                if (!e.getPipeline().isMaterialEquivalent(currentPipeline)) {
                    currentMaterial = null; // must explicitly bind next material
                }
                (currentPipeline = e.getPipeline()).bind(cmd);
            }

            // bind the material if necessary
            if (e.getMaterial() != currentMaterial && !(currentMaterial = e.getMaterial()).bind(cmd, currentPipeline)) {
                throw new IllegalStateException("Attempted to bind material with an incompatible pipeline.");
            }

            // bind the mesh if necessary
            if (currentMesh != e.getMesh()) {
                (currentMesh = e.getMesh()).bind(cmd, e.getGeometry().getLodLevel());
            }

            // render
            currentMesh.render(cmd, e.getGeometry().getLodLevel());
        }
    }

    @Override
    protected boolean fastAdd(Geometry geometry) {
        return queue.add(new Element(geometry));
    }

    public void setOverrideState(PipelineState overrideState) {
        this.overrideState = overrideState;
    }

    public PipelineCache getCache() {
        return cache;
    }

    public PipelineState getOverrideState() {
        return overrideState;
    }

    public class Element {

        private final Geometry geometry;
        private final VkMaterial material;
        private final VkMesh mesh;
        private final Pipeline pipeline;
        private float distanceSq = Float.NaN;

        private Element(Geometry geometry) {
            this.geometry = geometry;
            Material mat = forcedMaterial != null ? forcedMaterial : this.geometry.getMaterial();
            if (!(mat instanceof NewMaterial)) {
                throw new ClassCastException("Cannot render " + mat.getClass() + " in a Vulkan context.");
            }
            this.material = (VkMaterial)mat;
            Mesh m = forcedMesh != null ? forcedMesh : this.geometry.getMesh();
            if (!(m instanceof VkMesh)) {
                throw new ClassCastException("Cannot render " + mat.getClass() + " in a Vulkan context.");
            }
            this.mesh = (VkMesh)m;
            this.pipeline = this.material.selectPipeline(cache, this.mesh.getDescription(), forcedTechnique, overrideState);
            if (!this.pipeline.getBindPoint().is(PipelineBindPoint.Graphics)) {
                throw new IllegalStateException("Cannot render geometry with a non-graphical pipeline.");
            }
        }

        public float computeDistanceSq() {
            if (!Float.isNaN(distanceSq)) return distanceSq;
            return (distanceSq = camera.getLocation().distanceSquared(geometry.getWorldTranslation()));
        }

        public Camera getCamera() {
            return camera;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public VkMaterial getMaterial() {
            return material;
        }

        public VkMesh getMesh() {
            return mesh;
        }

        public Pipeline getPipeline() {
            return pipeline;
        }

    }

}
