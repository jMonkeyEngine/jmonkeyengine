/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.renderer;

import com.jme3.scene.Mesh;
import com.jme3.shader.Shader;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.IntMap;

/**
 * Allows tracking of real-time rendering statistics.
 *
 * <p>The <code>Statistics</code> can be retrieved by using {@link Renderer#getStatistics() }.
 *
 * @author Kirill Vainer
 */
public class Statistics {

    /**
     * Enables or disables updates.
     */
    protected boolean enabled = false;

    /**
     * Number of object used during the current frame.
     */
    protected int numObjects;
    /**
     * Number of mesh primitives rendered during the current frame.
     */
    protected int numTriangles;
    /**
     * Number of mesh vertices rendered during the current frame.
     */
    protected int numVertices;
    /**
     * Number of shader switches during the current frame.
     */
    protected int numShaderSwitches;
    /**
     * Number of texture binds during the current frame.
     */
    protected int numTextureBinds;
    /**
     * Number of FBO switches during the current frame.
     */
    protected int numFboSwitches;
    /**
     * Number of uniforms set during the current frame.
     */
    protected int numUniformsSet;

    /**
     * Number of active shaders.
     */
    protected int memoryShaders;
    /**
     * Number of active frame buffers.
     */
    protected int memoryFrameBuffers;
    /**
     * Number of active textures.
     */
    protected int memoryTextures;

    /**
     * IDs of all shaders in use.
     */
    protected IntMap<Void> shadersUsed = new IntMap<>();
    /**
     * IDs of all textures in use.
     */
    protected IntMap<Void> texturesUsed = new IntMap<>();
    /**
     * IDs of all FBOs in use.
     */
    protected IntMap<Void> fbosUsed = new IntMap<>();

    /**
     * ID of the most recently used shader.
     */
    protected int lastShader = -1;

    /**
     * Returns a list of labels corresponding to each statistic.
     *
     * @return a list of labels corresponding to each statistic.
     *
     * @see #getData(int[])
     */
    public String[] getLabels(){
        return new String[]{ "Vertices",
                             "Triangles",
                             "Uniforms",

                             "Objects",

                             "Shaders (S)",
                             "Shaders (F)",
                             "Shaders (M)",

                             "Textures (S)",
                             "Textures (F)",
                             "Textures (M)",

                             "FrameBuffers (S)",
                             "FrameBuffers (F)",
                             "FrameBuffers (M)" };

    }

    /**
     * Retrieves the statistics data into the given array.
     * The array should be as large as the array given in
     * {@link #getLabels() }.
     *
     * @param data The data array to write to
     */
    public void getData(int[] data) {
        data[0] = numVertices;
        data[1] = numTriangles;
        data[2] = numUniformsSet;
        data[3] = numObjects;

        data[4] = numShaderSwitches;
        data[5] = shadersUsed.size();
        data[6] = memoryShaders;

        data[7] = numTextureBinds;
        data[8] = texturesUsed.size();
        data[9] = memoryTextures;

        data[10] = numFboSwitches;
        data[11] = fbosUsed.size();
        data[12] = memoryFrameBuffers;
    }

    /**
     * Called by the Renderer when a mesh has been drawn.
     *
     * @param mesh the Mesh that was drawn (not null)
     * @param lod which level of detail
     * @param count multiplier for triangles and vertices
     */
    public void onMeshDrawn(Mesh mesh, int lod, int count) {
        if (!enabled) {
            return;
        }

        numObjects += 1;
        numTriangles += mesh.getTriangleCount(lod) * count;
        numVertices += mesh.getVertexCount() * count;
    }

    /**
     * Called by the Renderer when a mesh has been drawn.
     *
     * @param mesh the Mesh that was drawn (not null)
     * @param lod which level of detail
     */
    public void onMeshDrawn(Mesh mesh, int lod) {
        onMeshDrawn(mesh, lod, 1);
    }

    /**
     * Called by the Renderer when a shader has been utilized.
     *
     * @param shader The shader that was used
     * @param wasSwitched If true, the shader has required a state switch
     */
    public void onShaderUse(Shader shader, boolean wasSwitched) {
        assert shader.getId() >= 1;

        if (!enabled) {
            return;
        }

        // Reduces unnecessary hashmap lookups if
        // we already considered this shader.
        if (lastShader != shader.getId()) {
            lastShader = shader.getId();
            if (!shadersUsed.containsKey(shader.getId())) {
                shadersUsed.put(shader.getId(), null);
            }
        }

        if (wasSwitched) {
            numShaderSwitches++;
        }
    }

    /**
     * Called by the Renderer when a uniform was set.
     */
    public void onUniformSet() {
        if (!enabled) {
            return;
        }
        numUniformsSet++;
    }

    /**
     * Called by the Renderer when a texture has been set.
     *
     * @param image The image that was set
     * @param wasSwitched If true, the texture has required a state switch
     */
    public void onTextureUse(Image image, boolean wasSwitched) {
        assert image.getId() >= 1;

        if (!enabled) {
            return;
        }

        if (!texturesUsed.containsKey(image.getId())) {
            texturesUsed.put(image.getId(), null);
        }

        if (wasSwitched) {
            numTextureBinds++;
        }
    }

    /**
     * Called by the Renderer when a framebuffer has been set.
     *
     * @param fb The framebuffer that was set
     * @param wasSwitched If true, the framebuffer required a state switch
     */
    public void onFrameBufferUse(FrameBuffer fb, boolean wasSwitched) {
        if (!enabled) {
            return;
        }

        if (fb != null) {
            assert fb.getId() >= 1;

            if (!fbosUsed.containsKey(fb.getId())) {
                fbosUsed.put(fb.getId(), null);
            }
        }

        if (wasSwitched) {
            numFboSwitches++;
        }
    }

    /**
     * Clears all frame-specific statistics such as objects used per frame.
     */
    public void clearFrame() {
        shadersUsed.clear();
        texturesUsed.clear();
        fbosUsed.clear();

        numObjects = 0;
        numTriangles = 0;
        numVertices = 0;
        numShaderSwitches = 0;
        numTextureBinds = 0;
        numFboSwitches = 0;
        numUniformsSet = 0;

        lastShader = -1;
    }

    /**
     * Called by the Renderer when it creates a new shader.
     */
    public void onNewShader() {
        if (!enabled) {
            return;
        }
        memoryShaders++;
    }

    /**
     * Called by the Renderer when it creates a new texture.
     */
    public void onNewTexture() {
        if (!enabled) {
            return;
        }
        memoryTextures++;
    }

    /**
     * Called by the Renderer when it creates a new framebuffer.
     */
    public void onNewFrameBuffer() {
        if (!enabled) {
            return;
        }
        memoryFrameBuffers++;
    }

    /**
     * Called by the Renderer when it deletes a shader.
     */
    public void onDeleteShader() {
        if (!enabled) {
            return;
        }
        memoryShaders--;
    }

    /**
     * Called by the Renderer when it deletes a texture.
     */
    public void onDeleteTexture() {
        if (!enabled) {
            return;
        }
        memoryTextures--;
    }

    /**
     * Called by the Renderer when it deletes a framebuffer.
     */
    public void onDeleteFrameBuffer() {
        if (!enabled) {
            return;
        }
        memoryFrameBuffers--;
    }

    /**
     * Called when video memory is cleared.
     */
    public void clearMemory() {
        memoryFrameBuffers = 0;
        memoryShaders = 0;
        memoryTextures = 0;
    }

    /**
     * Enables or disables updates.
     *
     * @param f true to enable, false to disable
     */
    public void setEnabled(boolean f) {
        this.enabled = f;
    }

    /**
     * Tests whether updates are enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }
}
