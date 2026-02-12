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
package com.jme3.material;

import com.jme3.export.Savable;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructBuffer;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.material.technique.NewTechnique;
import com.jme3.vulkan.material.uniforms.Uniform;

import java.util.Objects;

/**
 * <code>Material</code> describes the rendering style for a given
 * {@link Geometry}.
 *
 * <p>A material is essentially a list of {@link MatParam parameters},
 * those parameters map to uniforms which are defined in a shader.
 * Setting the parameters can modify the behavior of a
 * shader.</p>
 *
 * @author codex
 */
public interface Material {

    <T extends Uniform> T setUniform(String name, T uniform);

    void setTechnique(String name, NewTechnique technique);

    <T extends Uniform> T getUniform(String name);

    NewTechnique getTechnique(String name);

    RenderState getAdditionalRenderState();

    default int getSortId() {
        return 0;
    }

    default void set(String name, Object value) {
        getUniform(name).set(value);
    }

}
