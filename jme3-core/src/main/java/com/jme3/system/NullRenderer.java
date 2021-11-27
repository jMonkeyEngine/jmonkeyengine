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
package com.jme3.system;

import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.Statistics;
import com.jme3.renderer.TextureUnitException;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.BufferObject;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;

public class NullRenderer implements Renderer {

    private final EnumSet<Caps> caps = EnumSet.allOf(Caps.class);
    private final EnumMap<Limits, Integer> limits = new EnumMap<>(Limits.class);
    private final Statistics stats = new Statistics();

    @Override
    public void initialize() {
        for (Limits limit : Limits.values()) {
            limits.put(limit, Integer.MAX_VALUE);
        }
    }

    @Override
    public EnumMap<Limits, Integer> getLimits() {
        return limits;
    }

    @Override
    public EnumSet<Caps> getCaps() {
        return caps;
    }

    @Override
    public Statistics getStatistics() {
        return stats;
    }

    @Override
    public void invalidateState(){
    }

    @Override
    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
    }

    @Override
    public void setBackgroundColor(ColorRGBA color) {
    }

    @Override
    public void applyRenderState(RenderState state) {
    }

    @Override
    public void setDepthRange(float start, float end) {
    }

    @Override
    public void postFrame() {
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
    }

    @Override
    public void setViewPort(int x, int y, int width, int height) {
    }

    @Override
    public void setClipRect(int x, int y, int width, int height) {
    }

    @Override
    public void clearClipRect() {
    }

    public void setLighting(LightList lights) {
    }

    @Override
    public void setShader(Shader shader) {
    }

    @Override
    public void deleteShader(Shader shader) {
    }

    @Override
    public void deleteShaderSource(ShaderSource source) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
    }

    @Override
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
    }

    @Override
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyColor, boolean copyDepth) {
    }
    
    
    @Override
    public void setMainFrameBufferOverride(FrameBuffer fb) {
    }
    
    @Override
    public void setFrameBuffer(FrameBuffer fb) {
    }

    @Override
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
    }

    @Override
    public void deleteFrameBuffer(FrameBuffer fb) {
    }

    @Override
    public void setTexture(int unit, Texture tex) throws TextureUnitException {
        // do nothing
    }

    @Override
    public void modifyTexture(Texture tex, Image pixels, int x, int y) {
    }

    @Override
    public void updateBufferData(VertexBuffer vb) {
    }

    @Override
    public void updateBufferData(BufferObject bo) {
    }
    @Override
    public void deleteBuffer(VertexBuffer vb) {
    }

    @Override
    public void deleteBuffer(BufferObject bo) {

    }

    @Override
    public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
    }

    @Override
    public void resetGLObjects() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void deleteImage(Image image) {
    }

    @Override
    public void setAlphaToCoverage(boolean value) {
    }

    @Override
    public void setMainFrameBufferSrgb(boolean srgb) {     
    }

    @Override
    public void setLinearizeSrgbImages(boolean linearize) {    
    }

    @Override
    public int[] generateProfilingTasks(int numTasks) {
        return new int[0];
    }

    @Override
    public void startProfiling(int id) {

    }

    @Override
    public void stopProfiling() {

    }

    @Override
    public long getProfilingTime(int taskId) {
        return 0;
    }

    @Override
    public boolean isTaskResultAvailable(int taskId) {
        return false;
    }

    @Override
    public void readFrameBufferWithFormat(FrameBuffer fb, ByteBuffer byteBuf, Image.Format format) {        
    }

    @Override
    public void setDefaultAnisotropicFilter(int level) {
    }

    /**
     * Determine the maximum allowed width for lines.
     *
     * @return the maximum width (in pixels)
     */
    @Override
    public float getMaxLineWidth() {
        return Float.MAX_VALUE;
    }

    @Override
    public boolean getAlphaToCoverage() {
        return false;
    }

    @Override
    public int getDefaultAnisotropicFilter() {
        return 0;
    }

    /**
     * Test whether images with the sRGB flag will be linearized when read by a
     * shader.
     *
     * @return true for linearization, false for no linearization
     */
    @Override
    public boolean isLinearizeSrgbImages() {
        return false;
    }

    /**
     * Test whether colors rendered to the main framebuffer undergo
     * linear-to-sRGB conversion.
     *
     * @return true for conversion, false for no conversion
     */
    @Override
    public boolean isMainFrameBufferSrgb() {
        return false;
    }
}
