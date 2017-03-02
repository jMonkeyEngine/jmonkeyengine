/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import java.nio.ByteBuffer;
import java.util.EnumSet;

import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.Statistics;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.EnumMap;

public class NullRenderer implements Renderer {

    private final EnumSet<Caps> caps = EnumSet.allOf(Caps.class);
    private final EnumMap<Limits, Integer> limits = new EnumMap<>(Limits.class);
    private final Statistics stats = new Statistics();

    public void initialize() {
        for (Limits limit : Limits.values()) {
            limits.put(limit, Integer.MAX_VALUE);
        }
    }

    @Override
    public EnumMap<Limits, Integer> getLimits() {
        return limits;
    }

    public EnumSet<Caps> getCaps() {
        return caps;
    }

    public Statistics getStatistics() {
        return stats;
    }

    public void invalidateState(){
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
    }

    public void setBackgroundColor(ColorRGBA color) {
    }

    public void applyRenderState(RenderState state) {
    }

    public void setDepthRange(float start, float end) {
    }

    public void postFrame() {
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
    }

    public void setViewPort(int x, int y, int width, int height) {
    }

    public void setClipRect(int x, int y, int width, int height) {
    }

    public void clearClipRect() {
    }

    public void setLighting(LightList lights) {
    }

    public void setShader(Shader shader) {
    }

    public void deleteShader(Shader shader) {
    }

    public void deleteShaderSource(ShaderSource source) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
    }
    
    public void setMainFrameBufferOverride(FrameBuffer fb) {
    }
    
    public void setFrameBuffer(FrameBuffer fb) {
    }

    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
    }

    public void setTexture(int unit, Texture tex) {
    }

    public void modifyTexture(Texture tex, Image pixels, int x, int y) {
    }

    public void updateBufferData(VertexBuffer vb) {
    }

    public void deleteBuffer(VertexBuffer vb) {
    }

    public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
    }

    public void resetGLObjects() {
    }

    public void cleanup() {
    }

    public void deleteImage(Image image) {
    }

    public void setAlphaToCoverage(boolean value) {
    }

    public void setMainFrameBufferSrgb(boolean srgb) {     
    }

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

    public void readFrameBufferWithFormat(FrameBuffer fb, ByteBuffer byteBuf, Image.Format format) {        
    }

    @Override
    public void setDefaultAnisotropicFilter(int level) {
    }
}
