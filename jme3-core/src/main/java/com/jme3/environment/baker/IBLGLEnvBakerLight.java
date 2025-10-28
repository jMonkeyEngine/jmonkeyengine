/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.environment.baker;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import com.jme3.asset.AssetManager;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.material.GlMaterial;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.GlImage;
import com.jme3.texture.Texture2D;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.GlImage.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;

/**
 * Fully accelerated env baker for IBL that bakes the specular map and spherical
 * harmonics on the GPU.
 * 
 * This is lighter on VRAM but it is not as parallelized as IBLGLEnvBaker
 * 
 * @author Riccardo Balbo
 */
public class IBLGLEnvBakerLight extends IBLHybridEnvBakerLight {
    private static final int NUM_SH_COEFFICIENT = 9;
    private static final Logger LOG = Logger.getLogger(IBLGLEnvBakerLight.class.getName());

    /**
     * Create a new IBL env baker
     * 
     * @param rm
     *            The render manager used to render the env scene
     * @param am
     *            The asset manager used to load the baking shaders
     * @param format
     *            The format of the color buffers
     * @param depthFormat
     *            The format of the depth buffers
     * @param env_size
     *            The size in pixels of the output environment cube map (eg.
     *            1024)
     * @param specular_size
     *            The size in pixels of the output specular cube map (eg. 1024)
     */
    public IBLGLEnvBakerLight(RenderManager rm, AssetManager am, Format format, Format depthFormat, int env_size, int specular_size) {
        super(rm, am, format, depthFormat, env_size, specular_size);
    }

    @Override
    public boolean isTexturePulling() {
        return this.texturePulling;
    }

    @Override
    public void bakeSphericalHarmonicsCoefficients() {
        Box boxm = new Box(1, 1, 1);
        Geometry screen = new Geometry("BakeBox", boxm);

        Material mat = new GlMaterial(assetManager, "Common/IBLSphH/IBLSphH.j3md");
        mat.setTexture("Texture", envMap);
        mat.setVector2("Resolution", new Vector2f(envMap.getImage().getWidth(), envMap.getImage().getHeight()));
        screen.setMaterial(mat);

        float remapMaxValue = 0;
        Format format = Format.RGBA32F;
        if (!renderManager.getRenderer().getCaps().contains(Caps.FloatColorBufferRGBA)) {
            LOG.warning("Float textures not supported, using RGB8 instead. This may cause accuracy issues.");
            format = Format.RGBA8;
            remapMaxValue = 0.05f;
        }

        if (remapMaxValue > 0) {
            mat.setFloat("RemapMaxValue", remapMaxValue);
        } else {
            mat.clearParam("RemapMaxValue");
        }

        Texture2D shCoefTx[] = { new Texture2D(NUM_SH_COEFFICIENT, 1, 1, format), new Texture2D(NUM_SH_COEFFICIENT, 1, 1, format) };

        FrameBuffer shbaker[] = { new FrameBuffer(NUM_SH_COEFFICIENT, 1, 1), new FrameBuffer(NUM_SH_COEFFICIENT, 1, 1) };
        shbaker[0].setSrgb(false);
        shbaker[0].addColorTarget(FrameBufferTarget.newTarget(shCoefTx[0]));

        shbaker[1].setSrgb(false);
        shbaker[1].addColorTarget(FrameBufferTarget.newTarget(shCoefTx[1]));

        int renderOnT = -1;

        for (int faceId = 0; faceId < 6; faceId++) {
            if (renderOnT != -1) {
                int s = renderOnT;
                renderOnT = renderOnT == 0 ? 1 : 0;
                mat.setTexture("ShCoef", shCoefTx[s]);
            } else {
                renderOnT = 0;
            }

            mat.setInt("FaceId", faceId);

            screen.updateLogicalState(0);
            screen.updateGeometricState();

            renderManager.setCamera(updateAndGetInternalCamera(0, shbaker[renderOnT].getWidth(), shbaker[renderOnT].getHeight(), Vector3f.ZERO, 1, 1000), false);
            renderManager.getRenderer().setFrameBuffer(shbaker[renderOnT]);
            renderManager.renderGeometry(screen);
        }

        ByteBuffer shCoefRaw = BufferUtils.createByteBuffer(NUM_SH_COEFFICIENT * 1 * (shbaker[renderOnT].getColorTarget().getFormat().getBitsPerPixel() / 8));
        renderManager.getRenderer().readFrameBufferWithFormat(shbaker[renderOnT], shCoefRaw, shbaker[renderOnT].getColorTarget().getFormat());
        shCoefRaw.rewind();

        GlImage img = new GlImage(format, NUM_SH_COEFFICIENT, 1, shCoefRaw, ColorSpace.Linear);
        ImageRaster imgr = ImageRaster.create(img);

        shCoef = new Vector3f[NUM_SH_COEFFICIENT];
        float weightAccum = 0.0f;

        for (int i = 0; i < shCoef.length; i++) {
            ColorRGBA c = imgr.getPixel(i, 0);
            shCoef[i] = new Vector3f(c.r, c.g, c.b);
            if (weightAccum == 0) weightAccum = c.a;
            else if (weightAccum != c.a) {
                LOG.warning("SH weight is not uniform, this may cause issues.");
            }

        }

        if (remapMaxValue > 0) weightAccum /= remapMaxValue;

        for (int i = 0; i < NUM_SH_COEFFICIENT; ++i) {
            if (remapMaxValue > 0) shCoef[i].divideLocal(remapMaxValue);
            shCoef[i].multLocal(4.0f * FastMath.PI / weightAccum);
        }
        EnvMapUtils.prepareShCoefs(shCoef);
        img.dispose();

    }
}
