package com.jme3.vulkan.mesh;

import com.jme3.math.Vector3f;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.SingleCommand;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.update.CommandBatch;

import java.nio.ShortBuffer;

public class MyCustomMesh extends AdaptiveMesh {

    private final LogicalDevice<?> device;
    private final UpdateFrameManager frames;
    private final CommandBatch updateStaticBuffers;

    public MyCustomMesh(LogicalDevice<?> device,
                        UpdateFrameManager frames,
                        MeshDescription description,
                        CommandBatch updateSharedBuffers,
                        Vector3f normal, Vector3f up, float width, float height, float centerX, float centerY) {
        super(description);
        this.device = device;
        this.frames = frames;
        this.updateStaticBuffers = updateSharedBuffers;
        VersionedResource<? extends GpuBuffer> indices = createStaticBuffer(MemorySize.shorts(6));
        indexBuffers.add(indices);
        for (GpuBuffer buf : indices) {
            ShortBuffer iBuf = buf.mapShorts();
            iBuf.put((short)0).put((short)2).put((short)3)
                .put((short)0).put((short)1).put((short)2);
            buf.unmap();
        }
        try (Builder m = buildVertexBuffers(4)) {
            m.setMode(BuiltInAttribute.Position, VertexMode.Static);
            m.setMode(BuiltInAttribute.TexCoord, VertexMode.Static);
            m.setMode(BuiltInAttribute.Normal, VertexMode.Static);
        }
        Vector3f x = normal.cross(up);
        Vector3f y = normal.cross(x);
        Vector3f tempX = new Vector3f();
        Vector3f tempY = new Vector3f();
        try (AttributeModifier position = modifyAttribute(BuiltInAttribute.Position);
             AttributeModifier normals = modifyAttribute(BuiltInAttribute.Normal);
             AttributeModifier texCoord = modifyAttribute(BuiltInAttribute.TexCoord)) {
            position.putVector3(0, 0, x.mult(-width * centerX, tempX).addLocal(y.mult(height * (1f - centerY), tempY)));
            position.putVector3(1, 0, x.mult(width * (1.0f - centerX), tempX).addLocal(y.mult(height * (1f - centerY), tempY)));
            position.putVector3(2, 0, x.mult(width * (1.0f - centerX), tempX).addLocal(y.mult(-height * centerY, tempY)));
            position.putVector3(3, 0, x.mult(-width * centerX, tempX).addLocal(y.mult(-height * centerY, tempY)));
            normals.putVector3(0, 0, normal);
            normals.putVector3(1, 0, normal);
            normals.putVector3(2, 0, normal);
            normals.putVector3(3, 0, normal);
            texCoord.putVector2(0, 0, 0f, 0f);
            texCoord.putVector2(1, 0, 1f, 0f);
            texCoord.putVector2(2, 0, 1f, 1f);
            texCoord.putVector2(3, 0, 0f, 1f);
        }
    }

    @Override
    protected VersionedResource<? extends GpuBuffer> createStreamingBuffer(MemorySize size) {
        throw new UnsupportedOperationException("Streaming buffers not implemented.");
    }

    @Override
    protected VersionedResource<? extends GpuBuffer> createDynamicBuffer(MemorySize size) {
        throw new UnsupportedOperationException("Dynamic buffers not implemented.");
    }

    @Override
    protected VersionedResource<? extends GpuBuffer> createStaticBuffer(MemorySize size) {
        return updateStaticBuffers.add(new SingleCommand<>(new StaticBuffer(
                device, size, BufferUsage.Vertex, MemoryProp.DeviceLocal, false)));
    }

}
