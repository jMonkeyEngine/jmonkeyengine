/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.shadow.next.pssm;

import com.jme3.light.Light;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.Texture2D;
import com.jme3.shadow.next.ShadowMapSlice;

public abstract class BaseShadowMapSlice<T extends Light> implements ShadowMapSlice<T> {

    protected final FrameBuffer frameBuffer;
    protected final Texture2D depthTexture;
    protected final Camera shadowCamera;
    protected final Vector3f[] points;
    protected final Matrix4f biasedViewProjectionMatrix = new Matrix4f();

    public BaseShadowMapSlice(int size, Vector3f[] points) {
        this.depthTexture = new Texture2D(size, size, Image.Format.Depth16);
        this.depthTexture.setAnisotropicFilter(1);
        this.depthTexture.setShadowCompareMode(ShadowCompareMode.LessOrEqual);
        this.depthTexture.setMagFilter(MagFilter.Bilinear);
        this.depthTexture.setMinFilter(MinFilter.BilinearNoMipMaps);
        this.shadowCamera = new Camera(size, size);
        this.frameBuffer = new FrameBuffer(size, size, 1);
        this.frameBuffer.setDepthTexture(depthTexture);
        this.points = points;
    }

    @Override
    public void renderShadowMap(RenderManager renderManager, T light, ViewPort viewPort, GeometryList shadowCasters) {
        Renderer renderer = renderManager.getRenderer();

        renderer.setFrameBuffer(frameBuffer);
        renderer.clearBuffers(false, true, false);

        if (shadowCasters.size() > 0) {
            renderManager.setCamera(shadowCamera, false);
            viewPort.getQueue().renderShadowQueue(shadowCasters, renderManager, shadowCamera, true);
        }
    }

    @Override
    public Matrix4f getBiasedViewProjectionMatrix() {
        return BIAS_MATRIX.mult(shadowCamera.getViewProjectionMatrix(), biasedViewProjectionMatrix);
    }
}
