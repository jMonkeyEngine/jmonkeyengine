package com.jme3.vulkan.render.batching;

import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.pipeline.VertexPipeline;

import java.util.Comparator;
import java.util.function.Consumer;

public abstract class VulkanGeometryBatch extends GeometryBatch<VulkanGeometryBatch.Element> {

    private final DescriptorPool pool;

    public VulkanGeometryBatch(Comparator<? super Element> comparator, DescriptorPool pool) {
        super(comparator);
        this.pool = pool;
    }

    public void render(CommandBuffer cmd, Consumer<Element> perRender) {
        if (queue.isEmpty()) {
            return;
        }
        cmd.applySettings();
        VertexPipeline currentPipeline = null;
        VulkanMaterial currentMaterial = null;
        for (Element e : queue) {
            if (e.getPipeline() != currentPipeline) {
                (currentPipeline = e.getPipeline()).bind(cmd);
                currentMaterial = null; // current material binding is invalidated
            }
            if (e.getMaterial() != currentMaterial) {
                (currentMaterial = e.getMaterial()).bind(cmd, currentPipeline, pool);
            }
            perRender.accept(e);
            e.getMesh().render(cmd, e.getPipeline());
        }
    }

    @Override
    public boolean add(Geometry geometry) {
        if (geometry.getMaterial().getTechnique(forcedTechnique) == null) {
            return false;
        }
        return queue.add(new Element(geometry));
    }

    protected abstract VertexPipeline createPipeline(Element e);

    public DescriptorPool getPool() {
        return pool;
    }

    public class Element extends AbstractBatchElement implements VulkanBatchElement {

        private final VulkanTechnique technique;
        private final VulkanMaterial material;
        private final VulkanMesh mesh;
        private final VertexPipeline pipeline;

        public Element(Geometry geometry) {
            super(geometry);
            this.material = (VulkanMaterial)(forcedMaterial != null ? forcedMaterial : geometry.getMaterial());
            this.technique = (VulkanTechnique) material.getTechnique(forcedTechnique != null ? forcedTechnique : "main");
            this.mesh = (VulkanMesh) (forcedMesh != null ? forcedMesh : geometry.getMesh());
            this.pipeline = createPipeline(this);
        }

        @Override
        public Camera getCamera() {
            return camera;
        }

        @Override
        public VulkanMaterial getMaterial() {
            return material;
        }

        @Override
        public VulkanMesh getMesh() {
            return mesh;
        }

        @Override
        public long getPipelineSortId() {
            return pipeline.getSortPosition();
        }

        @Override
        public long getMaterialSortId() {
            return 0;
        }

        @Override
        public VulkanTechnique getTechnique() {
            return technique;
        }

        @Override
        public VertexPipeline getPipeline() {
            return pipeline;
        }

    }

}
