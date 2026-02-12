package com.jme3.vulkan.mesh;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.bih.BIHTree;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.GlMesh;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.buffers.GlNativeBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.pipeline.VertexPipeline;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class AdaptiveMesh implements VulkanMesh, GlMesh {

    private final MeshLayout layout;
    private final List<VertexBuffer> vertexBuffers = new ArrayList<>();
    private final List<MappableBuffer> indexBuffers = new ArrayList<>(1);
    private final Map<String, GlVertexBuffer.Usage> attributeUsages = new HashMap<>();
    private final int vertexCapacity, instanceCapacity;
    private MappableBuffer selectedIndex;
    private int vertices, instances;
    private final BoundingVolume volume = new BoundingBox();
    private BIHTree collisionTree;
    private IntEnum<Topology> topology = Topology.TriangleList;

    public AdaptiveMesh(MeshLayout layout, int vertices, int instances) {
        this.layout = layout;
        this.vertices = this.vertexCapacity = vertices;
        this.instances = this.instanceCapacity = instances;
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
                if (pipeline.vertexBindingCompatible(b)) {
                    verts.put(getVertexBuffer(b).getData().getId());
                    offsets.put(b.getOffset());
                }
            }
            verts.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts, offsets);
        }
        if (selectedIndex != null) {
            vkCmdBindIndexBuffer(cmd.getBuffer(), ((VulkanBuffer)selectedIndex).getId(), 0, IndexType.of(selectedIndex).getEnum());
            vkCmdDrawIndexed(cmd.getBuffer(), selectedIndex.size().getElements(), instances, 0, 0, 0);
        } else {
            vkCmdDraw(cmd.getBuffer(), vertices, instances, 0, 0);
        }
    }

    @Override
    public void render(GLRenderer renderer) {
        if (vertices <= 0 || instances <= 0) {
            return;
        }
        for (VertexBuffer vb : vertexBuffers) {
            renderer.setVertexAttrib(vb);
        }
        renderer.clearVertexAttribs(); // misleading name here
        if (selectedIndex != null) {
            renderer.drawTriangleList((GlNativeBuffer)selectedIndex, topology, instances, vertices);
        } else {
            renderer.drawTriangleArray(topology, instances, vertices);
        }
    }

    @Override
    public <T extends Attribute> T mapAttribute(String name) {
        return layout.mapAttribute(this, name);
    }

    @Override
    public void setLevelOfDetail(int level, MappableBuffer buffer) {
        while (indexBuffers.size() <= level) {
            indexBuffers.add(null);
        }
        indexBuffers.set(level, buffer);
        if (selectedIndex == null) {
            selectedIndex = buffer;
        }
    }

    @Override
    public MappableBuffer selectLevelOfDetail(int level) {
        selectedIndex = null;
        for (; level >= 0; level--) {
            if ((selectedIndex = indexBuffers.get(level)) != null) break;
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
        VertexBuffer vb = vertexBuffers.stream()
                .filter(v -> v.getBinding() == binding)
                .findAny().orElse(null);
        if (vb == null) {
            GlVertexBuffer.Usage usage = GlVertexBuffer.Usage.Static;
            for (NamedAttribute attr : binding.getAttributes()) {
                GlVertexBuffer.Usage u = attributeUsages.get(attr.getName());
                if (u != null && u.ordinal() > usage.ordinal()) {
                    usage = u;
                }
            }
            vb = new VertexBuffer(binding, getCapacity(binding.getInputRate()), usage);
            vertexBuffers.add(vb);
        }
        return vb;
    }

    @Override
    public int setElements(IntEnum<InputRate> rate, int elements) {
        if (rate.is(InputRate.Vertex)) return vertices = Math.min(elements, vertexCapacity);
        if (rate.is(InputRate.Instance)) return instances = Math.min(elements, instanceCapacity);
        throw new IllegalArgumentException("Input rate enum \"" + rate + "\" is not supported.");
    }

    @Override
    public void pushElements(IntEnum<InputRate> rate, int baseElement, int elements) {
        for (VertexBuffer vb : vertexBuffers) {
            if (vb.getBinding().getInputRate().is(rate)) {
                vb.getData().push((int)vb.getBinding().getOffset() + baseElement * vb.getBinding().getStride(), elements * vb.getBinding().getStride());
            }
        }
    }

    @Override
    public void pushElements(IntEnum<InputRate> rate) {
        pushElements(rate, 0, getElements(rate));
    }

    @Override
    public int getElements(IntEnum<InputRate> rate) {
        if (rate.is(InputRate.Vertex)) return vertices;
        if (rate.is(InputRate.Instance)) return instances;
        return 0;
    }

    @Override
    public int getCapacity(IntEnum<InputRate> rate) {
        if (rate.is(InputRate.Vertex)) return vertexCapacity;
        if (rate.is(InputRate.Instance)) return instanceCapacity;
        return 0;
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
