package com.jme3.scene.mesh;

import com.jme3.scene.GlVertexBuffer;

import java.util.HashMap;
import java.util.Map;

public class GlMeshDescription {

    private final Map<GlVertexBuffer.Type, BufferDescription> buffers = new HashMap<>();


    private static class BufferDescription {

        public final int components;

        public BufferDescription(int components) {
            this.components = components;
        }

    }

}
