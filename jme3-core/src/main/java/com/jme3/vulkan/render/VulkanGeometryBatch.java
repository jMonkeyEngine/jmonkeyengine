package com.jme3.vulkan.render;

import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.shader.ShaderModule;

import java.util.Comparator;

public class VulkanGeometryBatch extends GeometryBatch<VulkanGeometryBatch.Element> {

    private final AssetManager assetManager;
    private final DescriptorPool pool;
    private final Cache<Pipeline> pipelineCache;
    private final Cache<ShaderModule> shaderCache;

    public VulkanGeometryBatch(Camera camera, Comparator<Element> comparator, AssetManager assetManager, DescriptorPool pool, Cache<Pipeline> pipelineCache, Cache<ShaderModule> shaderCache) {
        super(camera, comparator);
        this.assetManager = assetManager;
        this.pool = pool;
        this.pipelineCache = pipelineCache;
        this.shaderCache = shaderCache;
    }

    @Override
    public void render(CommandBuffer cmd) {
        VertexPipeline currentPipeline = null;
        VulkanMaterial currentMaterial = null;
        for (Element e : queue) {
            // I'm not sure if it's safe or optimal to allow controls to muck with
            // geometry/material/mesh at this point, but this would technically
            // be the location to do this.
            //e.getGeometry().runControlRender(rm, cmd, camera);

            // Provide transform matrices to the material. This requires
            // that the appropriate buffers be updated on or after this point.
            e.getGeometry().updateMatrixTransforms(camera);

            // bind pipeline if necessary
            if (e.getPipeline() != currentPipeline) {
                (currentPipeline = e.getPipeline()).bind(cmd);
                currentMaterial = null;
            }

            // bind material if necessary
            if (e.getMaterial() != currentMaterial) {
                (currentMaterial = e.getMaterial()).bind(cmd, currentPipeline, pool);
            }

            // render
            e.getMesh().render(cmd, currentPipeline);
        }
    }

    @Override
    public boolean add(Geometry geometry) {
        return queue.add(new Element(geometry));
    }

    public class Element implements BatchElement {

        private final Geometry geometry;
        private final VulkanMaterial material;
        private final VulkanMesh mesh;
        private final VertexPipeline pipeline;
        private float distanceSq = Float.NaN;
        private float distance = Float.NaN;

        private Element(Geometry geometry) {
            this.geometry = geometry;
            Material mat = forcedMaterial != null ? forcedMaterial : this.geometry.getMaterial();
            if (!(mat instanceof NewMaterial)) {
                throw new ClassCastException("Cannot render " + mat.getClass() + " in a Vulkan context.");
            }
            this.material = (VulkanMaterial)mat;
            Mesh mesh = forcedMesh != null ? forcedMesh : this.geometry.getMesh();
            if (!(mesh instanceof VulkanMesh)) {
                throw new ClassCastException("Cannot render " + mesh.getClass() + " in a Vulkan context.");
            }
            this.mesh = (VulkanMesh)mesh;
            this.pipeline = GraphicsPipeline.build(pool.getDevice(), p -> {
                p.setCache(pipelineCache);
                p.applyGeometry(assetManager, Element.this.mesh, material, forcedTechnique, shaderCache);
            });
        }

        @Override
        public float computeDistanceSq() {
            if (!Float.isNaN(distanceSq)) return distanceSq;
            return (distanceSq = camera.getLocation().distanceSquared(geometry.getWorldTranslation()));
        }

        @Override
        public float computeDistance() {
            if (!Float.isNaN(distance)) return distance;
            return (distance = FastMath.sqrt(computeDistanceSq()));
        }

        @Override
        public Camera getCamera() {
            return camera;
        }

        @Override
        public Geometry getGeometry() {
            return geometry;
        }

        @Override
        public VulkanMaterial getMaterial() {
            return material;
        }

        @Override
        public VulkanMesh getMesh() {
            return mesh;
        }

        public VertexPipeline getPipeline() {
            return pipeline;
        }

    }

}
