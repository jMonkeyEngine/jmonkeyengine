package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.collision.bih.BIHTree;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.VulkanMesh;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.generate.BufferGenerator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public abstract class AdaptiveMesh implements VulkanMesh {

    protected final MeshDescription description;
    protected final BufferGenerator generator;
    protected final Map<Integer, GpuBuffer> indexBuffers = new HashMap<>();
    protected final VertexBuffer[] vertexBuffers;
    protected BoundingVolume volume = new BoundingBox();
    private int vertices;
    private int instances = 1;
    private int maxLod = 0;
    private BIHTree collisionTree;

    public AdaptiveMesh(MeshDescription description, BufferGenerator generator) {
        this.description = description;
        this.generator = generator;
        this.vertexBuffers = new VertexBuffer[description.getBindings().size()];
    }

    @Override
    public void bind(CommandBuffer cmd, int lod) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(vertexBuffers.length);
            LongBuffer offsets = stack.mallocLong(vertexBuffers.length);
            for (int i = 0; i < vertexBuffers.length; i++) {
                VertexBuffer vb = vertexBuffers[i];
                if (vb == null) {
                    vb = vertexBuffers[i] = new VulkanVertexBuffer(description.getBinding(i), generator, vertices);
                }
                verts.put(vb.getBuffer().getId());
                offsets.put(vb.getOffset());
            }
            verts.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts, offsets);
        }
        if (!indexBuffers.isEmpty()) {
            GpuBuffer indices = selectIndexBuffer(lod);
            vkCmdBindIndexBuffer(cmd.getBuffer(), indices.getId(), 0, IndexType.of(indices).getEnum());
        }
    }

    @Override
    public void render(CommandBuffer cmd, int lod) {
        if (!indexBuffers.isEmpty()) {
            vkCmdDrawIndexed(cmd.getBuffer(), selectIndexBuffer(lod).size().getElements(), instances, 0, 0, 0);
        } else {
            vkCmdDraw(cmd.getBuffer(), vertices, instances, 0, 0);
        }
    }

    private GpuBuffer selectIndexBuffer(int lod) {
        lod = Math.min(lod, maxLod);
        GpuBuffer indices = null;
        while (lod >= 0 && (indices = indexBuffers.get(lod)) == null) {
            lod--;
        }
        if (indices == null) {
            throw new NullPointerException("No index buffers specified.");
        }
        return indices;
    }

    @Override
    public AttributeModifier modify(String attribute) {
        VertexAttribute attr = description.getAttribute(attribute);
        if (attr != null) {
            VertexBuffer buffer = vertexBuffers[attr.getBinding().getBindingIndex()];
            if (buffer == null) {
                buffer = new VulkanVertexBuffer(attr.getBinding(), generator, vertices);
                vertexBuffers[attr.getBinding().getBindingIndex()] = buffer;
            }
            return new VulkanAttributeModifier(buffer, attr);
        } else {
            return new NullAttributeModifier();
        }
    }

    @Override
    public void setAccessFrequency(String attributeName, AccessRate access) {
        VertexAttribute attr = description.getAttribute(attributeName);
        if (attr != null) {
            VertexBuffer buffer = vertexBuffers[attr.getBinding().getBindingIndex()];
            if (buffer == null) {
                buffer = new VulkanVertexBuffer(attr.getBinding(), generator, vertices);
                vertexBuffers[attr.getBinding().getBindingIndex()] = buffer;
            } else {
                buffer.setAccessFrequency(access);
            }
        }
    }

    @Override
    public void setVertexCount(int vertices) {
        if (this.vertices != vertices) {
            this.vertices = vertices;
            for (VertexBuffer vb : vertexBuffers) {
                if (vb != null && !vb.isInstanceBuffer()) {
                    vb.setNumVertices(vertices);
                }
            }
        }
    }

    @Override
    public void setTriangleCount(int lod, int triangles) {
        GpuBuffer indices = indexBuffers.get(lod);
        maxLod = Math.max(maxLod, lod);
        if (indices == null) {
            // todo: allow element size in bytes to be configurable
            indexBuffers.put(lod, new AdaptiveBuffer(MemorySize.ints(triangles * 3), BufferUsage.Index, generator));
        } else {
            indices.resize(triangles * 3);
        }
    }

    @Override
    public void setInstanceCount(int instances) {
        if (this.instances != instances) {
            this.instances = instances;
            for (VertexBuffer vb : vertexBuffers) {
                if (vb != null && vb.isInstanceBuffer()) {
                    vb.setNumVertices(instances);
                }
            }
        }
    }

    @Override
    public int getVertexCount() {
        return vertices;
    }

    @Override
    public int getTriangleCount(int lod) {
        GpuBuffer indices = indexBuffers.get(lod);
        if (indices != null) {
            return indices.size().getElements() / 3;
        } else return 0;
    }

    @Override
    public int getInstanceCount() {
        return instances;
    }

    @Override
    public int collideWith(Collidable other, Geometry geometry, CollisionResults results) {
        if (collisionTree == null) try (AttributeModifier pos = modifyPosition()) {
            GpuBuffer indices = indexBuffers.get(0);
            collisionTree = new BIHTree(pos, indices.mapIndices());
            collisionTree.construct();
            indices.unmap();
        }
        return collisionTree.collideWith(other, geometry.getWorldMatrix(), geometry.getWorldBound(), results);
    }

    @Override
    public void updateBound() {
        try (AttributeModifier pos = modifyPosition()) {
            volume.computeFromPoints(pos);
        }
    }

    @Override
    public void setBound(BoundingVolume volume) {
        this.volume = volume;
    }

    @Override
    public BoundingVolume getBound() {
        return volume;
    }

    @Override
    public int getNumLodLevels() {
        return indexBuffers.size();
    }

    @Override
    public MeshDescription getDescription() {
        return description;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {

    }

    @Override
    public void read(JmeImporter im) throws IOException {

    }

}
