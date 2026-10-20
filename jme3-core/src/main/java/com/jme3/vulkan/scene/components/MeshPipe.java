package com.jme3.vulkan.scene.components;

import com.jme3.scene.Mesh;
import com.jme3.util.collections.DynamicArray;
import com.jme3.util.collections.DynamicIntArray;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.vulkan.alloc.ReservedStructArray;
import com.jme3.vulkan.mesh.IdxBuffer;
import com.jme3.vulkan.mesh.VertexBuffer;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.scene.Scene;

/**
 * Communicates a node's mesh to whatever {@link com.jme3.vulkan.material.experimental.ShadingTechnique}
 * renders it, the same way {@code StructPipe}/{@code DescriptorPipe} communicate material
 * parameters and textures: observing {@link Scene.MeshType}, and on change writing a compact
 * {@link MeshDescriptor} into a GPU-visible slot rather than handing the technique the
 * {@link Mesh} object itself.
 * <p>
 * The descriptor is shaped for vertex pulling, not for binding {@link Mesh}'s vertex buffers
 * as fixed-function vertex input: each attribute slot is a raw GPU buffer device address (0
 * if the mesh has no such attribute), plus vertex/index counts and an index buffer address.
 * The vertex shader dereferences these directly (a {@code buffer_reference} block per
 * attribute, indexed by {@code gl_VertexIndex}) instead of the pipeline's fixed-function
 * vertex input stage reading from bound attribute buffers. This means a technique never binds
 * per-mesh vertex buffers at all, and selecting "which mesh" for a draw is just an index into
 * this pipe's array rather than a {@code vkCmdBindVertexBuffers} call - which is also what
 * makes it safe for unrelated nodes, even in different scenes, to share the exact same
 * underlying mesh buffers: nothing about a device address is scene- or node-specific, only the
 * descriptor slot that references it is.
 * <p>
 * This pipe is deliberately not owned by any particular technique (see {@code MeshPipe}'s
 * sibling pipes in {@code PBR}, which went the other way originally and had to be corrected):
 * any {@link com.jme3.vulkan.material.experimental.ShadingTechnique} that needs mesh data
 * fetches the one shared instance via {@link Scene#getPipe}.
 */
@Deprecated
public class MeshPipe implements ComponentPipe {

    public static final int POSITION_BIT = 1;
    public static final int NORMAL_BIT = 1 << 1;
    public static final int TANGENT_BIT = 1 << 2;
    public static final int TEXCOORD_BIT = 1 << 3;
    public static final int COLOR_BIT = 1 << 4;

    private final Scene scene;
    private final ReservedStructArray<MeshDescriptor> array;
    private final DynamicArray<Integer> slotMapping = new DynamicIntArray(128);

    public MeshPipe(Scene scene, ReservedStructArray<MeshDescriptor> array) {
        this.scene = scene;
        this.array = array;
    }

    @Override
    public void onComponentChanged(Component type, int node) {
        Integer slot = slotMapping.get(node);
        if (slot == null) {
            return;
        }
        Mesh mesh = (Mesh)type.get(node);
        MeshDescriptor desc = array.index(slot);
        if (mesh == null) {
            desc.presentAttributes.set(0);
            desc.vertexCount.set(0);
            desc.indexAddress.set(0L);
            desc.indexCount.set(0);
            return;
        }
        int present = 0;
        present |= writeAttribute(desc.positionAddress, mesh, CommonAttributes.Position, POSITION_BIT);
        present |= writeAttribute(desc.normalAddress, mesh, CommonAttributes.Normal, NORMAL_BIT);
        present |= writeAttribute(desc.tangentAddress, mesh, CommonAttributes.Tangent, TANGENT_BIT);
        present |= writeAttribute(desc.texCoordAddress, mesh, CommonAttributes.TexCoord, TEXCOORD_BIT);
        present |= writeAttribute(desc.colorAddress, mesh, CommonAttributes.Color, COLOR_BIT);
        desc.presentAttributes.set(present);
        desc.vertexCount.set(mesh.getVertexCount());
        // IdxBuffer doesn't exist yet. It needs to expose its backing buffer's device address
        // and index type the same way vertex attribute buffers do below, so the shader can
        // pull indices the same way it pulls vertices instead of an indexed draw binding it.
        IdxBuffer indices = mesh.getBaseIndexBuffer();
        if (indices != null) {
            desc.indexAddress.set(indices.getBuffer().getDeviceAddress());
            desc.indexType.set(indices.getIndexType().ordinal());
            desc.indexCount.set(indices.getIndexCount());
        } else {
            desc.indexAddress.set(0L);
            desc.indexCount.set(0);
        }
    }

    private int writeAttribute(Struct.Field<Long> field, Mesh mesh, String attributeName, int bit) {
        VertexBuffer vb = mesh.getVertexBuffer(attributeName);
        if (vb == null) {
            field.set(0L);
            return 0;
        }
        // MappableBuffer doesn't exist yet either. Vertex pulling needs it to expose the
        // device address of its backing GpuBuffer - see VmaMemoryAllocator's existing
        // VK_KHR_buffer_device_address probing - so the shader can dereference it directly
        // instead of this buffer ever being bound as a fixed-function vertex attribute.
        field.set(vb.getBuffer().getDeviceAddress());
        return bit;
    }

    @Override
    public void activate(int node) {
        if (slotMapping.fillIfNull(node, () -> array.acquire())) {
            for (Class<? extends Component> t : getObservedTypes()) {
                onComponentChanged(scene.getComponent(t), node);
            }
        }
    }

    @Override
    public void deactivate(int node) {
        Integer slot = slotMapping.get(node);
        if (slot != null) {
            array.release(slot);
            slotMapping.set(node, null);
        }
    }

    public StructArray<MeshDescriptor> getArray() {
        return array;
    }

    /**
     * One node's mesh, described for vertex pulling: a GPU buffer device address per attribute
     * (0 where the mesh has none), {@link #presentAttributes} marking which addresses are
     * actually valid (bits from {@link #POSITION_BIT} etc.), and the counts/index data needed
     * to issue a pull-based draw without ever binding this mesh's buffers as vertex input.
     */
    public static class MeshDescriptor extends Struct {

        public final Field<Long> positionAddress = new Field<>(0L);
        public final Field<Long> normalAddress = new Field<>(0L);
        public final Field<Long> tangentAddress = new Field<>(0L);
        public final Field<Long> texCoordAddress = new Field<>(0L);
        public final Field<Long> colorAddress = new Field<>(0L);
        public final Field<Long> indexAddress = new Field<>(0L);
        public final Field<Integer> indexType = new Field<>(0);
        public final Field<Integer> vertexCount = new Field<>(0);
        public final Field<Integer> indexCount = new Field<>(0);
        public final Field<Integer> presentAttributes = new Field<>(0);

        public MeshDescriptor() {
            add(positionAddress, normalAddress, tangentAddress, texCoordAddress, colorAddress,
                    indexAddress, indexType, vertexCount, indexCount, presentAttributes);
        }

    }

}
