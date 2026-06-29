package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.bih.BIHTree;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.GlMesh;
import com.jme3.scene.Mesh;
import com.jme3.util.struct.FieldSequence;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.attributes.AttributeMapping;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh implements VulkanMesh, GlMesh, Mesh {

    private final Collection<VertexBuffer> vertexBuffers = new ArrayList<>();
    private final List<IdxBuffer> indexBuffers = new ArrayList<>();
    private IdxBuffer activeIndex;
    private int vertexCapacity, instanceCapacity;
    private int vertices, instances;
    private Topology topology = Topology.TriangleList;
    private BoundingVolume volume = new BoundingBox();
    private BIHTree collisionTree;

    @SerializationOnly
    protected AdaptiveMesh() {
        vertices = vertexCapacity = 1;
        instances = instanceCapacity = 1;
    }

    public AdaptiveMesh(int vertexCapacity, int instanceCapacity) {
        this.vertices = this.vertexCapacity = vertexCapacity;
        this.instances = this.instanceCapacity = instanceCapacity;
    }

    @Override
    public void render(CommandBuffer cmd, VertexPipeline pipeline) {
        if (vertices <= 0 || instances <= 0) {
            return;
        }
        pipeline.bindVertexBuffers(cmd, vertexBuffers);
        if (activeIndex == null) {
            selectLevelOfDetail(0);
        }
        if (activeIndex != null) {
            vkCmdBindIndexBuffer(cmd.getBuffer(),
                    ((VulkanBuffer)activeIndex.getBuffer()).getBufferHandle(cmd.getPool().getDevice()),
                    0, activeIndex.getType().getEnum(VulkanEnums.instance));
            vkCmdDrawIndexed(cmd.getBuffer(), activeIndex.getElements(), instances, 0, 0, 0);
        } else {
            vkCmdDraw(cmd.getBuffer(), vertices, instances, 0, 0);
        }
    }

    @Override
    public VertexInput declareVertexInput(Function<String, Integer> attributeMapper) {
        return new VertexInput(attributeMapper, vertexBuffers);
    }

    @Override
    public void render(GLRenderer renderer) {
        if (vertices <= 0 || instances <= 0) {
            return;
        }
        for (VertexBuffer vb : vertexBuffers) {
            renderer.setVertexAttrib(vb);
        }
        renderer.clearVertexAttribs();
        if (activeIndex == null) {
            selectLevelOfDetail(0);
        }
        if (activeIndex != null) {
            renderer.drawTriangleList(activeIndex, topology, instances, vertices);
        } else {
            renderer.drawTriangleArray(topology, instances, vertices);
        }
    }

    @Override
    public void setIndexBuffer(int lod, IdxBuffer buffer) {
        while (lod >= indexBuffers.size()) {
            indexBuffers.add(null);
        }
        indexBuffers.set(lod, buffer);
    }

    @Override
    public int selectLevelOfDetail(int lod) {
        lod = Math.min(lod, indexBuffers.size() - 1);
        for (; lod >= 0; lod--) {
            activeIndex = indexBuffers.get(lod);
            if (activeIndex != null) return lod;
        }
        activeIndex = null;
        return 0;
    }

    @Override
    public IdxBuffer getIndexBuffer(int lod) {
        return indexBuffers.get(lod);
    }

    @Override
    public IdxBuffer getLevelOfDetail(int lod) {
        lod = Math.min(lod, indexBuffers.size() - 1);
        for (; lod >= 0; lod--) {
            IdxBuffer idx = indexBuffers.get(lod);
            if (idx != null) return idx;
        }
        return null;
    }

    @Override
    public void stageIndices(int lod) {
        IdxBuffer buf = indexBuffers.get(lod);
        if (buf != null) buf.stage();
    }

    @Override
    public void stageLevelOfDetail(int lod) {
        IdxBuffer buf = getLevelOfDetail(lod);
        if (buf != null) buf.stage();
    }

    @Override
    public void stageIndices() {
        for (IdxBuffer buf : indexBuffers) {
            if (buf != null) buf.stage();
        }
    }

    @Override
    public void addVertexBuffer(VertexBuffer vertexBuffer) {
        vertexBuffer.resize(getElementCapacity(vertexBuffer.getRate()));
        vertexBuffers.add(vertexBuffer);
    }

    @Override
    public Collection<VertexBuffer> getVertexBuffers() {
        return Collections.unmodifiableCollection(vertexBuffers);
    }

    @Override
    public AttributeMapping mapAttributes(InputRate rate, String... attributes) {
        Queue<StructField> fields = new LinkedList<>();
        Map<Struct, VertexBuffer> bindings = new IdentityHashMap<>();
        for (String n : attributes) {
            VertexBuffer vb = vertexBuffers.stream()
                    .filter(v -> v.getRate() == rate && v.getAttribute(n) != null)
                    .findFirst().orElse(null);
            if (vb != null) {
                fields.add(vb.getAttribute(n));
                bindings.put(vb.getStruct(), vb);
            } else {
                throw new NullPointerException("Attribute \"" + n + "\" does not exist.");
            }
        }
        return new AttributeMapping(getElementCount(rate), bindings.values(), fields);
    }

    @Override
    public void stageVertices(int baseVertex, int vertices) {
        for (VertexBuffer vb : vertexBuffers) {
            if (vb.getRate() == InputRate.Vertex) {
                vb.stage(baseVertex, vertices);
            }
        }
    }

    @Override
    public void stageInstances(int baseInstance, int instances) {
        for (VertexBuffer vb : vertexBuffers) {
            if (vb.getRate() == InputRate.Instance) {
                vb.stage(baseInstance, instances);
            }
        }
    }

    @Override
    public void setVertexCapacity(int vertexCapacity) {
        this.vertexCapacity = vertexCapacity;
        for (VertexBuffer vb : vertexBuffers) {
            if (vb.getRate() == InputRate.Vertex) {
                vb.resize(vertexCapacity);
            }
        }
    }

    @Override
    public void setInstanceCapacity(int instanceCapacity) {
        for (VertexBuffer vb : vertexBuffers) {
            if (vb.getRate() == InputRate.Instance) {
                vb.resize(instanceCapacity);
            }
        }
    }

    @Override
    public int setVertexCount(int vertices) {
        return this.vertices = Math.min(vertices, vertexCapacity);
    }

    @Override
    public int setInstanceCount(int instances) {
        return this.instances = Math.min(instances, instanceCapacity);
    }

    @Override
    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    @Override
    public int getVertexCount() {
        return vertices;
    }

    @Override
    public int getInstanceCount() {
        return instances;
    }

    @Override
    public int getVertexCapacity() {
        return vertexCapacity;
    }

    @Override
    public int getInstanceCapacity() {
        return instanceCapacity;
    }

    @Override
    public Topology getTopology() {
        return topology;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.writeSavableArrayList(new ArrayList(vertexBuffers), "vertexBuffers", null);
        out.writeSavableArrayList(new ArrayList(indexBuffers), "indexBuffers", null);
        out.write(vertices, "vertices", 1);
        out.write(instances, "instances", 1);
        out.write(vertexCapacity, "vertexCapacity", Math.max(vertices, 1));
        out.write(instanceCapacity, "instanceCapacity", Math.max(instances, 1));
        out.write(topology, "topology", Topology.TriangleList);
        out.write(volume, "volume", null);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        vertexBuffers.addAll(in.readSavableArrayList("vertexBuffers", null));
        indexBuffers.addAll(in.readSavableArrayList("indexBuffers", null));
        vertices = in.readInt("vertices", 1);
        instances = in.readInt("instances", 1);
        vertexCapacity = in.readInt("vertexCapacity", Math.max(vertices, 1));
        instanceCapacity = in.readInt("instanceCapacity", Math.max(instances, 1));
        topology = in.readEnum("topology", Topology.class, Topology.TriangleList);
        volume = (BoundingVolume)in.readSavable("volume", null);
    }

    public void updateBound() {
        try (AttributeMapping m = mapAttributes(InputRate.Vertex, CommonAttributes.Position)) {
            volume.computeFromPoints(new FieldSequence<>(m, m.poll()));
        }
    }

}
