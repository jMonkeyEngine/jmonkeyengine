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
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.frames.SingleResource;
import com.jme3.vulkan.frames.VersionedResource;

/**
 * <code>Material</code> describes the rendering style for a given
 * {@link Geometry}.
 * <p>A material is essentially a list of {@link MatParam parameters},
 * those parameters map to uniforms which are defined in a shader.
 * Setting the parameters can modify the behavior of a
 * shader.
 * </p>
 *
 * @author Kirill Vainer
 */
public interface Material extends Savable {

    String DEFAULT_UNIFORM_BUFFER = "DefaultUniformBuffer";

    void setUniform(String name, GpuBuffer buffer);

    void setTexture(String name, Texture texture);

    void setParam(String uniform, String param, Object value);

    /* ----- COMPATABILITY WITH OLD MATERIAL ----- */

    default void setParam(String param, Object value) {
        setParam(DEFAULT_UNIFORM_BUFFER, param, value);
    }

    default void setBoolean(String param, boolean value) {
        setParam(param, value);
    }

    default void setInt(String param, int value) {
        setParam(param, value);
    }

    default void setFloat(String param, float value) {
        setParam(param, value);
    }

    default void setFloat(String param, Float value) {
        setParam(param, value);
    }

    default void setColor(String param, ColorRGBA value) {
        setParam(param, value);
    }

    default void setVector2(String param, Vector2f value) {
        setParam(param, value);
    }

    default void setVector3(String param, Vector3f value) {
        setParam(param, value);
    }

    default void setVector4(String param, Vector4f value) {
        setParam(param, value);
    }

    default void setMatrix4(String param, Matrix4f value) {
        setParam(param, value);
    }

}
