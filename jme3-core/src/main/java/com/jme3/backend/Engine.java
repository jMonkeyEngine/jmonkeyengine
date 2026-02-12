package com.jme3.backend;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.texture.Texture;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.buffers.BufferGenerator;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.attribute.Attribute;

public interface Engine extends BufferGenerator<MappableBuffer> {

    Material createMaterial();

    Material createMaterial(String matdefName);

    Mesh createMesh(int vertices, int instances);

    <T extends Struct> Uniform<T> createUniformBuffer(StructLayout layout, T struct);

    <T extends Struct> Uniform<T> createShaderStorageUniform(StructLayout layout, T struct);

    Uniform<Texture> createTextureUniform();


    default Mesh createMesh(int vertices) {
        return createMesh(vertices, 1);
    }

    default Mesh createQuadMesh() {
        return createQuadMesh(1);
    }

    default Mesh createQuadMesh(int instances) {
        Mesh mesh = createMesh(4, instances);
        Vector3f temp3 = new Vector3f();
        mesh.mapAttribute(GlVertexBuffer.Type.Position, (Attribute<Vector3f> pos) -> {
            pos.set(0, temp3.set(-1, -1, 0));
            pos.set(1, temp3.set( 1, -1, 0));
            pos.set(2, temp3.set(-1,  1, 0));
            pos.set(3, temp3.set( 1,  1, 0));
        });
        mesh.mapAttribute(GlVertexBuffer.Type.TexCoord, (Attribute<Vector2f> tex) -> {
            Vector2f temp2 = new Vector2f();
            tex.set(0, temp2.set(0, 1));
            tex.set(1, temp2.set(1, 1));
            tex.set(2, temp2.set(0, 0));
            tex.set(3, temp2.set(1, 0));
        });
        mesh.mapAttribute(GlVertexBuffer.Type.Normal, (Attribute<Vector3f> norm) -> {
            for (Vector3f n : norm.write(temp3)) {
                n.set(0, 0, 1);
            }
        });
        mesh.setLevelOfDetail(0, createByteBuffer(BufferUsage.Vertex, GlVertexBuffer.Usage.Static,
                new byte[] {0, 1, 2, 2, 1, 3}));
        mesh.pushElements(InputRate.Vertex);
        return mesh;
    }

}
