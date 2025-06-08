/*
 * Copyright (c) 2009-2025 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.mesh;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.scene.VertexBuffer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Map;

/**
 * `MorphTarget` represents a single morph target within a `Mesh`.
 * A morph target contains a set of `FloatBuffer` instances, each corresponding
 * to a `VertexBuffer.Type` (e.g., `POSITION`, `NORMAL`, `TANGENT`).
 * These buffers store the delta (difference) values that, when added to the
 * base mesh's corresponding vertex buffers, create a deformed version of the mesh.
 * <p>
 * Morph targets are primarily used for skeletal animation blending, facial animation,
 * or other mesh deformation effects. Each `MorphTarget` can optionally have a name
 * for identification and control.
 */
public class MorphTarget implements Savable {

    /**
     * Stores the `FloatBuffer` instances for each `VertexBuffer.Type` that
     * this morph target affects.
     */
    private final EnumMap<VertexBuffer.Type, FloatBuffer> buffers = new EnumMap<>(VertexBuffer.Type.class);
    /**
     * An optional name for this morph target, useful for identification
     * and targeting in animations.
     */
    private String name;

    /**
     * Required for jME deserialization.
     */
    public MorphTarget() {
    }

    /**
     * Creates a new `MorphTarget` with the specified name.
     *
     * @param name The name of this morph target (can be null).
     */
    public MorphTarget(String name) {
        this.name = name;
    }

    /**
     * Sets the name of this morph target.
     *
     * @param name The new name for this morph target (can be null).
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this morph target.
     *
     * @return The name of this morph target, or null if not set.
     */
    public String getName() {
        return name;
    }

    /**
     * Associates a `FloatBuffer` with a specific `VertexBuffer.Type` for this morph target.
     * This buffer typically contains the delta values for the specified vertex attribute.
     *
     * @param type The type of vertex buffer (e.g., `POSITION`, `NORMAL`).
     * @param buffer The `FloatBuffer` containing the delta data for the given type.
     */
    public void setBuffer(VertexBuffer.Type type, FloatBuffer buffer) {
        buffers.put(type, buffer);
    }

    /**
     * Retrieves the `FloatBuffer` associated with a specific `VertexBuffer.Type` for this morph target.
     *
     * @param type The type of vertex buffer.
     * @return The `FloatBuffer` for the given type, or null if not set.
     */
    public FloatBuffer getBuffer(VertexBuffer.Type type) {
        return buffers.get(type);
    }

    /**
     * Returns the `EnumMap` containing all the `FloatBuffer` instances
     * associated with their `VertexBuffer.Type` for this morph target.
     *
     * @return An `EnumMap` of vertex buffer types to their corresponding `FloatBuffer`s.
     */
    public EnumMap<VertexBuffer.Type, FloatBuffer> getBuffers() {
        return buffers;
    }

    /**
     * Returns the number of `FloatBuffer`s (i.e., vertex attribute types)
     * contained within this morph target.
     *
     * @return The count of buffers in this morph target.
     */
    public int getNumBuffers() {
        return buffers.size();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        for (Map.Entry<VertexBuffer.Type, FloatBuffer> entry : buffers.entrySet()) {
            VertexBuffer.Type type = entry.getKey();
            FloatBuffer roData = entry.getValue().asReadOnlyBuffer();
            oc.write(roData, type.name(), null);
        }
        oc.write(name, "morphName", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        for (VertexBuffer.Type type : VertexBuffer.Type.values()) {
            FloatBuffer fb = ic.readFloatBuffer(type.name(), null);
            if (fb != null) {
                setBuffer(type, fb);
            }
        }
        name = ic.readString("morphName", null);
    }
}
