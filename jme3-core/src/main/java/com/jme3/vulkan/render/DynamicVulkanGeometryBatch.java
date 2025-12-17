package com.jme3.vulkan.render;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.pipeline.VertexPipeline;

import java.util.Comparator;

public class DynamicVulkanGeometryBatch extends GeometryBatch<DynamicVulkanGeometryBatch.Element> {



    public DynamicVulkanGeometryBatch(Camera camera, Comparator<Element> comparator) {
        super(camera, comparator);
    }

    @Override
    public void render(CommandBuffer cmd) {

    }

    @Override
    protected boolean fastAdd(Geometry geometry) {
        return queue.add(new Element(geometry));
    }

    public class Element implements BatchElement {

        private final Geometry geometry;
        private final VulkanMesh mesh;
        private final VulkanMaterial material;
        private final VertexPipeline pipeline;

        protected Element(Geometry geometry) {
            this.geometry = geometry;
            if (!(geometry.getMesh() instanceof VulkanMesh)) {
                throw new ClassCastException(geometry + " must have a VulkanMesh.");
            }
            if (!(geometry.getMaterial() instanceof VulkanMaterial)) {
                throw new ClassCastException(geometry + " must have a VulkanMaterial.");
            }
            this.mesh = (VulkanMesh)geometry.getMesh();
            this.material = (VulkanMaterial)geometry.getMaterial();

        }

        @Override
        public float computeDistanceSq() {
            return 0;
        }

        @Override
        public float computeDistance() {
            return 0;
        }

        @Override
        public Camera getCamera() {
            return null;
        }

        @Override
        public Geometry getGeometry() {
            return null;
        }

        @Override
        public Material getMaterial() {
            return null;
        }

        @Override
        public Mesh getMesh() {
            return null;
        }

    }

}
