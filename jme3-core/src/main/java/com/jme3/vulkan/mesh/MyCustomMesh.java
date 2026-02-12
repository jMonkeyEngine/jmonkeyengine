package com.jme3.vulkan.mesh;

import com.jme3.math.Vector3f;
import com.jme3.scene.GlVertexBuffer.Type;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;

import java.nio.ShortBuffer;

public class MyCustomMesh extends AdaptiveMesh {

    private static final short[] INDICES = {0, 2, 3, 0, 1, 2};

    public MyCustomMesh(MeshDescription description,
                        BufferGenerator<?> generator,
                        Vector3f normal, Vector3f up, float width, float height, float centerX, float centerY) {
        super(description, generator);
        MappableBuffer indices = generator.createBuffer(MemorySize.shorts(6), BufferUsage.Index, DataAccess.Static);
        indexBuffers.put(0, indices);
        ShortBuffer iBuf = indices.mapShorts();
        iBuf.put(INDICES);
        indices.unmap();
        Vector3f x = normal.cross(up);
        Vector3f y = normal.cross(x);
        Vector3f tempX = new Vector3f();
        Vector3f tempY = new Vector3f();
        try (AttributeModifier position = modify(Type.Position.name());
             AttributeModifier normals = modify(Type.Normal.name());
             AttributeModifier texCoord = modify(Type.TexCoord.name())) {
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

}
