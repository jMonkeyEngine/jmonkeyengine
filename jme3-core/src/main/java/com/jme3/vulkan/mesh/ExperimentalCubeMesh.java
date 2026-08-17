package com.jme3.vulkan.mesh;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;

public class ExperimentalCubeMesh {

    private static class MyVertex extends Struct<VertexAttr> {

        public final VertexAttr<Vector3f> position = new VertexAttr<>("position", new Vector3f());
        public final VertexAttr<Vector2f> texCoord = new VertexAttr<>("texCoord", new Vector2f());

        public MyVertex() {
            addFields(position, texCoord);
            bind(StructLayout.std140);
        }

    }

    private final StructArray<MyVertex> vertices = new StructArray<>(8, new MyVertex());
    private final EngineBuffer indices;

    public ExperimentalCubeMesh(EngineShaderBindings data) {
        vertices.bind(data.createVertexArray(vertices));
        indices = data.createTriangleArray(36);
        Vector3f tmp = new Vector3f();
        vertices.index(0).position.set(tmp.set(0f, 0f, 0f));
        vertices.index(1).position.set(tmp.set(1f, 0f, 0f));
        vertices.index(2).position.set(tmp.set(1f, 1f, 0f));
        vertices.index(3).position.set(tmp.set(0f, 1f, 0f));
        vertices.index(4).position.set(tmp.set(0f, 0f, 1f));
        vertices.index(5).position.set(tmp.set(1f, 0f, 1f));
        vertices.index(6).position.set(tmp.set(1f, 1f, 1f));
        vertices.index(7).position.set(tmp.set(0f, 1f, 1f));
        DataBuffer i = indices.cache();
        i.put(0).put(1).put(2);
        i.put(0).put(2).put(3);
        i.put(4).put(6).put(5);
        i.put(4).put(7).put(6);
        i.put(0).put(7).put(4);
        i.put(0).put(3).put(7);
        i.put(0).put(4).put(5);
        i.put(0).put(5).put(1);
        i.put(1).put(5).put(6);
        i.put(1).put(6).put(2);
        i.put(2).put(6).put(7);
        i.put(2).put(7).put(3);
    }

    public StructArray.Field<StructField<Vector3f>> getPositions() {
        return vertices.field(v -> v.position);
    }

}
