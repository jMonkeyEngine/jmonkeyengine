package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.bih.BIHTree;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.GlMesh;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.GlNativeBuffer;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.tmp.EffectivelyFinal;
import com.jme3.vulkan.tmp.EffectivelyFinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh implements VulkanMesh, GlMesh {

    @EffectivelyFinal
    private MeshLayout layout;
    @EffectivelyFinal
    private long vertexCapacity, instanceCapacity;

    private final Map<VertexBinding, VertexBuffer> vertexBuffers = new IdentityHashMap<>();
    protected final List<MappableBuffer> indexBuffers = new ArrayList<>(1);
    private final Map<String, GlVertexBuffer.Usage> attributeUsages = new HashMap<>();
    private MappableBuffer selectedIndex;
    private long vertices, instances;
    private final BoundingVolume volume = new BoundingBox();
    private BIHTree collisionTree;
    private IntEnum<Topology> topology = Topology.TriangleList;

    @SerializationOnly
    protected AdaptiveMesh() {}

    public AdaptiveMesh(MeshLayout layout, long vertices, long instances) {
        this.layout = layout;
        this.vertices = this.vertexCapacity = vertices;
        this.instances = this.instanceCapacity = instances;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(layout, "layout", null);
        out.write(vertexCapacity, "vertexCapacity", 0);
        out.write(instanceCapacity, "instanceCapacity", 0);
        out.write(vertices, "vertices", 0);
        out.write(instances, "instances", 0);
        out.write(indexBuffers.size(), "numLodLevels", 0);
        for (ListIterator<MappableBuffer> it = indexBuffers.listIterator(); it.hasNext();) {
            MappableBuffer buf = it.next();
            if (buf != null) try (BufferMapping m = buf.map()) {
                out.write(m.getBytes(), "lod" + it.previousIndex(), null);
            }
        }
    }

    @Override
    @EffectivelyFinalWriter
    public void read(JmeImporter im) throws IOException {

    }

    @Override
    public void render(CommandBuffer cmd, VertexPipeline pipeline) {
        if (vertices <= 0 || instances <= 0) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer verts = stack.mallocLong(layout.getBindings().size());
            LongBuffer offsets = stack.mallocLong(layout.getBindings().size());
            for (VertexBinding b : layout.getBindings()) {
                if (pipeline.isVertexBindingCompatible(b)) {
                    verts.put(((GpuBuffer<Long>)getVertexBuffer(b).getData().getBuffer()).getGpuObject());
                    offsets.put(b.getOffset());
                }
            }
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts.flip(), offsets.flip());
        }
        if (selectedIndex == null) {
            selectLevelOfDetail(0);
        }
        vkCmdBindIndexBuffer(cmd.getBuffer(), ((GpuBuffer<Long>)selectedIndex).getGpuObject(), 0, IndexType.of(selectedIndex).getEnum());
        vkCmdDrawIndexed(cmd.getBuffer(), (int)selectedIndex.size().getElements(), (int)instances, 0, 0, 0);
    }

    @Override
    public void render(GLRenderer renderer) {
        if (vertices <= 0 || instances <= 0) {
            return;
        }
        for (VertexBuffer vb : vertexBuffers.values()) {
            renderer.setVertexAttrib(vb);
        }
        renderer.clearVertexAttribs();
        if (selectedIndex == null) {
            selectLevelOfDetail(0);
        }
        renderer.drawTriangleList((GlNativeBuffer)selectedIndex, topology, (int)instances, (int)vertices);
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name) {
        return layout.mapAttribute(this, name);
    }

    @Override
    public MappableBuffer getIndexBuffer() {
        return indexBuffers.get(0);
    }

    @Override
    public MappableBuffer selectLevelOfDetail(int level) {
        selectedIndex = null;
        for (; level >= 0; level--) {
            if ((selectedIndex = indexBuffers.get(level)) != null) break;
        }
        if (selectedIndex == null) {
            throw new NullPointerException("No index buffer found at or below the specified level of detail.");
        }
        return selectedIndex;
    }

    @Override
    public MappableBuffer getLevelOfDetail(int level) {
        for (; level >= 0; level--) {
            MappableBuffer index = indexBuffers.get(level);
            if (index != null) return index;
        }
        return null;
    }

    @Override
    public VertexBuffer getVertexBuffer(VertexBinding binding) {
        VertexBuffer vb = vertexBuffers.get(binding);
        if (vb == null) {
            GlVertexBuffer.Usage usage = GlVertexBuffer.Usage.Static;
            for (NamedAttribute attr : binding.getAttributes()) {
                GlVertexBuffer.Usage u = attributeUsages.get(attr.getName());
                if (u != null && u.ordinal() > usage.ordinal()) {
                    usage = u;
                }
            }
            vertexBuffers.put(binding, vb = new VertexBuffer(binding, getCapacity(binding.getInputRate()), usage));
        }
        return vb;
    }

    @Override
    public long setElements(IntEnum<InputRate> rate, long elements) {
        if (rate.is(InputRate.Vertex)) return vertices = Math.min(elements, vertexCapacity);
        if (rate.is(InputRate.Instance)) return instances = Math.min(elements, instanceCapacity);
        throw new IllegalArgumentException("Input rate enum \"" + rate + "\" is not supported.");
    }

    @Override
    public void pushElements(IntEnum<InputRate> rate, long baseElement, long elements) {
        for (VertexBuffer vb : vertexBuffers.values()) {
            if (vb.getBinding().getInputRate().is(rate)) {
                vb.getData().stage(vb.getBinding().getOffset() + baseElement * vb.getBinding().getStride(), elements * vb.getBinding().getStride());
            }
        }
    }

    @Override
    public void pushElements(IntEnum<InputRate> rate) {
        pushElements(rate, 0, getElements(rate));
    }

    @Override
    public long getElements(IntEnum<InputRate> rate) {
        if (rate.is(InputRate.Vertex)) return vertices;
        if (rate.is(InputRate.Instance)) return instances;
        return 0;
    }

    @Override
    public long getCapacity(IntEnum<InputRate> rate) {
        if (rate.is(InputRate.Vertex)) return vertexCapacity;
        if (rate.is(InputRate.Instance)) return instanceCapacity;
        return 0;
    }

    @Override
    public Collection<VertexBuffer> getVertexBuffers() {
        return Collections.unmodifiableCollection(vertexBuffers.values());
    }

    @Override
    public IntEnum<Topology> getTopology() {
        return topology;
    }

    @Override
    public void setUsage(String attributeName, GlVertexBuffer.Usage usage) {
        attributeUsages.put(attributeName, usage);
    }

    @Override
    public boolean attributeExists(String name) {
        return layout.attributeExists(name);
    }

    @Override
    public MeshLayout getLayout() {
        return layout;
    }

}
