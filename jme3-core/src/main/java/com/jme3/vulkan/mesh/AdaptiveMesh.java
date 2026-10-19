package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.bih.BIHTree;
import com.jme3.scene.Mesh;
import com.jme3.util.struct.FieldArray;
import com.jme3.util.struct.FieldSequence;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.util.struct.StructField;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.buffer.IdxBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.attributes.AttributeMapping;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.pipeline.FaceWinding;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.pipeline.VertexPipeline;

import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh implements Mesh {

    private final Collection<StructArray<? extends Struct<VertexAttr>>> vertexBuffers = new ArrayList<>();
    private final Collection<StructArray<? extends Struct<VertexAttr>>> instanceBuffers = new ArrayList<>();
    private IdxBuffer indexBuffer;
    private Topology topology = Topology.TriangleList;
    private FaceWinding faceWinding = FaceWinding.CounterClockwise;
    private boolean primitiveRestart = false;
    private BoundingVolume volume = new BoundingBox();
    private BIHTree collisionTree;

    public AdaptiveMesh() {}

    @Override
    public void setIndexBuffer(IdxBuffer buffer) {
        this.indexBuffer = buffer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FieldArray getPositionField() {
        for (StructArray<? extends Struct<VertexAttr>> vb : vertexBuffers) {
            for (VertexAttr a : vb.getStruct().getFields()) {
                if (POSITION_ATTRIBUTE.equals(a.getName())) {
                    VertexAttr position = a;
                    return new FieldArray(vb, s -> position);
                }
            }
        }
        throw new IllegalStateException("Mesh is missing its required position data.");
    }

    public IdxBuffer getIndexBuffer() {
        return indexBuffer;
    }

    @Override
    public void addVertexBuffer(StructArray<? extends Struct<VertexAttr>> vertexBuffer) {
        vertexBuffers.add(vertexBuffer);
    }

    /**
     * Adds a per-instance buffer, the counterpart to {@link #addVertexBuffer} for
     * {@link #getInstanceBuffers()}. Not part of {@link Mesh}'s contract since nothing
     * else populates that collection otherwise.
     *
     * @param instanceBuffer buffer to add
     */
    public void addInstanceBuffer(StructArray<? extends Struct<VertexAttr>> instanceBuffer) {
        instanceBuffers.add(instanceBuffer);
    }

    @Override
    public Collection<StructArray<? extends Struct<VertexAttr>>> getVertexBuffers() {
        return Collections.unmodifiableCollection(vertexBuffers);
    }

    @Override
    public Collection<StructArray<? extends Struct<VertexAttr>>> getInstanceBuffers() {
        return Collections.unmodifiableCollection(instanceBuffers);
    }

    @Override
    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    @Override
    public void setFaceWinding(FaceWinding winding) {
        this.faceWinding = winding;
    }

    @Override
    public void setPrimitiveRestart(boolean primitiveRestart) {
        this.primitiveRestart = primitiveRestart;
    }

    @Override
    public BoundingVolume getBounds() {
        return volume;
    }

    @Override
    public Topology getTopology() {
        return topology;
    }

    @Override
    public FaceWinding getFaceWinding() {
        return faceWinding;
    }

    @Override
    public boolean isPrimitiveRestart() {
        return primitiveRestart;
    }

    public void updateBound() {
        try (AttributeMapping m = mapAttributes(InputRate.Vertex, CommonAttributes.Position)) {
            volume.computeFromPoints(new FieldSequence<>(m, m.poll()));
        }
    }

}
