package com.jme3.vulkan.render;

import com.jme3.material.*;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.commands.CommandSetting;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.pipeline.VertexPipeline;

import java.util.Comparator;
import java.util.function.Consumer;

public abstract class VulkanGeometryBatch extends GeometryBatch<VulkanGeometryBatch.Element> {

    private final DescriptorPool pool;

    public VulkanGeometryBatch(Comparator<? super Element> comparator, DescriptorPool pool) {
        super(comparator);
        this.pool = pool;
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

    public class Element extends AbstractBatchElement {

        private final VulkanTechnique technique;
        private final VulkanMaterial material;
        private final VulkanMesh mesh;
        private final VertexPipeline pipeline;

        private Element(Geometry geometry) {
            super(geometry);
            Material mat = forcedMaterial != null ? forcedMaterial : geometry.getMaterial();
            if (!(mat instanceof NewMaterial)) {
                throw new ClassCastException("Cannot render " + mat.getClass() + " in a Vulkan context.");
            }
            this.material = (VulkanMaterial)mat;
            technique = material.getTechnique(forcedTechnique);
            Mesh msh = forcedMesh != null ? forcedMesh : geometry.getMesh();
            if (!(msh instanceof VulkanMesh)) {
                throw new ClassCastException("Cannot render " + msh.getClass() + " in a Vulkan context.");
            }
            this.mesh = (VulkanMesh)msh;
            this.pipeline = createPipeline(this);
        }

        @Override
        public Camera getCamera() {
            return VulkanGeometryBatch.this.getCamera();
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
            return pipeline.getSortId();
        }

        @Override
        public long getMaterialSortId() {
            return 0;
        }

        public VulkanTechnique getTechnique() {
            return technique;
        }

        public VertexPipeline getPipeline() {
            return pipeline;
        }

    }

}
