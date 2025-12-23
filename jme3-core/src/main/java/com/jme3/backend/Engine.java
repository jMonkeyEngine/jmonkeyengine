package com.jme3.backend;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.LodBuffer;
import com.jme3.vulkan.mesh.VertexBinding;
import com.jme3.vulkan.mesh.attribute.Normal;
import com.jme3.vulkan.mesh.attribute.Position;
import com.jme3.vulkan.mesh.attribute.TexCoord;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface Engine {

    Material createMaterial();

    Material createMaterial(String matdefName);

    Mesh createMesh(int vertices, int instances);

    @Deprecated
    VertexBinding createMeshVertexBinding(IntEnum<InputRate> rate, Consumer<VertexBinding.Builder> config);

    GpuBuffer createBuffer(MemorySize size, Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage);

    default Mesh createQuadMesh() {
        Mesh mesh = createMesh(4, 1);
        mesh.mapAttribute(GlVertexBuffer.Type.Position, (Position pos) -> {
            pos.set(0, -1, -1, 0);
            pos.set(1, 1, -1, 0);
            pos.set(2, -1, 1, 0);
            pos.set(3, 1, 1, 0);
        });
        mesh.mapAttribute(GlVertexBuffer.Type.TexCoord, (TexCoord tex) -> {
            tex.set(0, 0, 1);
            tex.set(1, 1, 1);
            tex.set(2, 0, 0);
            tex.set(3, 1, 0);
        });
        mesh.mapAttribute(GlVertexBuffer.Type.Normal, (Normal norm) -> {
            for (Vector3f n : norm.readWrite(new Vector3f())) {
                n.set(0, 0, 1);
            }
        });
        GpuBuffer index = createByteBuffer(BufferUsage.Vertex, GlVertexBuffer.Usage.Static,
                new byte[] {0, 1, 2, 2, 1, 3});
        mesh.addLevelOfDetail(new LodBuffer(index, 0f));
        mesh.pushElements(InputRate.Vertex);
        return mesh;
    }

    default GpuBuffer createByteBuffer(Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage, byte... bytes) {
        GpuBuffer buffer = createBuffer(MemorySize.bytes(bytes.length), bufUsage, dataUsage);
        ByteBuffer indexBuf = buffer.mapBytes();
        indexBuf.put(bytes);
        buffer.unmap();
        buffer.push();
        return buffer;
    }

}
