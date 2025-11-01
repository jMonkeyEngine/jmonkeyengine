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

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jme3.asset.AssetManager;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.material.GlMaterial;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.texture.GlFrameBuffer;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.GlFrameBuffer.FrameBufferTarget;
import com.jme3.texture.GlImage.Format;
import com.jme3.texture.GlTexture.MagFilter;
import com.jme3.texture.GlTexture.MinFilter;
import com.jme3.texture.GlTexture.WrapMode;
import com.jme3.texture.image.ColorSpace;

/**
 * An env baker for IBL that bakes the specular map on the GPU and uses
 * spherical harmonics generated on the CPU for the irradiance map.
 * 
 * This is lighter on VRAM but uses the CPU to compute the irradiance map.
 * 
 * @author Riccardo Balbo
 */
public class IBLHybridEnvBakerLight extends GenericEnvBaker implements IBLEnvBakerLight {
    private static final Logger LOGGER = Logger.getLogger(IBLHybridEnvBakerLight.class.getName());
    protected TextureCubeMap specular;
    protected Vector3f[] shCoef;

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
    public IBLHybridEnvBakerLight(RenderManager rm, AssetManager am, Format format, Format depthFormat, int env_size, int specular_size) {
        super(rm, am, format, depthFormat, env_size);

        specular = new TextureCubeMap(specular_size, specular_size, format);
        specular.setWrap(WrapMode.EdgeClamp);
        specular.setMagFilter(MagFilter.Bilinear);
        specular.setMinFilter(MinFilter.Trilinear);
        specular.getImage().setColorSpace(ColorSpace.Linear);

        int nbMipMaps = (int) (Math.log(specular_size) / Math.log(2) + 1);
        nbMipMaps = limitMips(nbMipMaps, specular.getImage().getWidth(), specular.getImage().getHeight(), rm);

        int[] sizes = new int[nbMipMaps];
        for (int i = 0; i < nbMipMaps; i++) {
            int size = (int) FastMath.pow(2, nbMipMaps - 1 - i);
            sizes[i] = size * size * (specular.getImage().getGlFormat().getBitsPerPixel() / 8);
        }
        specular.getImage().setMipMapSizes(sizes);
        specular.getImage().setMipmapsGenerated(true);

    }

    @Override
    public boolean isTexturePulling() { // always pull textures from gpu
        return true;
    }

    private void bakeSpecularIBL(int mip, float roughness, Material mat, Geometry screen) throws Exception {
        mat.setFloat("Roughness", roughness);

        int mipWidth = (int) (specular.getImage().getWidth() * FastMath.pow(0.5f, mip));
        int mipHeight = (int) (specular.getImage().getHeight() * FastMath.pow(0.5f, mip));

        GlFrameBuffer specularbakers[] = new GlFrameBuffer[6];
        for (int i = 0; i < 6; i++) {
            specularbakers[i] = new GlFrameBuffer(mipWidth, mipHeight, 1);
            specularbakers[i].setSrgb(false);
            specularbakers[i].addColorTarget(FrameBufferTarget.newTarget(specular).level(mip).face(i));
            specularbakers[i].setMipMapsGenerationHint(false);
        }

        for (int i = 0; i < 6; i++) {
            GlFrameBuffer specularbaker = specularbakers[i];
            mat.setInt("FaceId", i);

            screen.updateLogicalState(0);
            screen.updateGeometricState();

            renderManager.setCamera(updateAndGetInternalCamera(i, specularbaker.getWidth(), specularbaker.getHeight(), Vector3f.ZERO, 1, 1000), false);
            renderManager.getRenderer().setFrameBuffer(specularbaker);
            renderManager.renderGeometry(screen);

            if (isTexturePulling()) {
                pull(specularbaker, specular, i);
            }

        }
        for (int i = 0; i < 6; i++) {
            specularbakers[i].dispose();
        }
    }

    @Override
    public void bakeSpecularIBL() {
        Box boxm = new Box(1, 1, 1);
        Geometry screen = new Geometry("BakeBox", boxm);

        Material mat = new GlMaterial(assetManager, "Common/IBL/IBLKernels.j3md");
        mat.setBoolean("UseSpecularIBL", true);
        mat.setTexture("EnvMap", envMap);
        screen.setMaterial(mat);

        if (isTexturePulling()) {
          startPulling();  
        } 

        int mip = 0;
        for (; mip < specular.getImage().getMipMapSizes().length; mip++) {
            try {
                float roughness = (float) mip / (float) (specular.getImage().getMipMapSizes().length - 1);
                bakeSpecularIBL(mip, roughness, mat, screen);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error while computing mip level " + mip, e);
                break;
            }
        }

        if (mip < specular.getImage().getMipMapSizes().length) {

            int[] sizes = specular.getImage().getMipMapSizes();
            sizes = Arrays.copyOf(sizes, mip);
            specular.getImage().setMipMapSizes(sizes);
            specular.getImage().setMipmapsGenerated(true);
            if (sizes.length <= 1) {
                try {
                    LOGGER.log(Level.WARNING, "Workaround driver BUG: only one mip level available, regenerate it with higher roughness (shiny fix)");
                    bakeSpecularIBL(0, 1f, mat, screen);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Error while recomputing mip level 0", e);
                }
            }
        }

        if (isTexturePulling()) {
            endPulling(specular);
        }
        specular.getImage().clearUpdateNeeded();

    }

    @Override
    public TextureCubeMap getSpecularIBL() {
        return specular;
    }

    @Override
    public void bakeSphericalHarmonicsCoefficients() {
        shCoef = EnvMapUtils.getSphericalHarmonicsCoefficents(getEnvMap());
        EnvMapUtils.prepareShCoefs(shCoef);
    }

    @Override
    public Vector3f[] getSphericalHarmonicsCoefficients() {
        return shCoef;
    }
}