/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.framegraph.light;

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Rasters light info to a set of textures.
 * 
 * @author codex
 */
public class LightImagePacker {
    
    private final Texture2D[] textures = new Texture2D[5];
    private final ImageRaster[] rasters = new ImageRaster[5];
    private final ColorRGBA tempColor = new ColorRGBA();
    private final LightFrustum frustum = new LightFrustum();
    private boolean hasAmbient = false;
    private ArrayList<LinkedList<Integer>> tileIndices = new ArrayList<>();
    
    public LightImagePacker() {}
    
    /**
     * Sets the textures to raster light information to.
     * 
     * @param tex1 texture to contain light color and type
     * @param tex2 texture to contain light position/direction and coefficients
     * @param tex3 texture to contain spot coefficients
     * @param tiles texture to contain start indices for each tile (or null to not calculate tiles)
     * @param indices texture to contain light indices referenced by tiles (or null to not calculate tiles)
     */
    public void setTextures(Texture2D tex1, Texture2D tex2, Texture2D tex3, Texture2D tiles, Texture2D indices) {
        validateSize(tex1, tex2);
        validateSize(tex1, tex3);
        updateTexture(0, tex1);
        updateTexture(1, tex2);
        updateTexture(2, tex3);
        updateTexture(3, tiles);
        updateTexture(4, indices);
    }
    /**
     * Gets the array of textures to raster to.
     * 
     * @return 
     */
    public Texture2D[] getTextures() {
        return textures;
    }
    /**
     * Gets the texture at the index.
     * 
     * @param i
     * @return 
     */
    public Texture2D getTexture(int i) {
        return textures[i];
    }
    
    /**
     * Rasters the lights in the LightList into the current textures.
     * 
     * @param lights lights to pack
     * @param ambient stores the accumulated ambient light
     * @param probes stores all LightProbes
     * @param cam camera for tile calculations
     * @param tileInfo tile demensions for tile calculations
     * @return number of directional, point, and spot lights
     */
    public int packLights(LightList lights, ColorRGBA ambient,
            List<LightProbe> probes, Camera cam, TiledRenderGrid tileInfo) {
        ambient.set(0, 0, 0, 0);
        probes.clear();
        if (lights.size() == 0) {
            return 0;
        }
        int i = 0;
        final int limit = textures[0].getImage().getWidth();
        final boolean useTiles = textures[3] != null && textures[4] != null && cam != null && tileInfo != null;
        if (useTiles) {
            tempColor.set(ColorRGBA.BlackNoAlpha);
            frustum.calculateCamera(cam);
            // match tile index lists to number of tiles
            int n = tileInfo.getNumTiles();
            while (tileIndices.size() > n) {
                tileIndices.remove(tileIndices.size()-1);
            }
            while (tileIndices.size() < n) {
                tileIndices.add(new LinkedList<>());
            }
        }
        boolean packedLight = false;
        boolean spotlight = false;
        hasAmbient = false;
        for (Light l : lights) {
            if (l.getType() == Light.Type.Ambient) {
                ambient.addLocal(l.getColor());
                hasAmbient = true;
                continue;
            }
            if (l.getType() == Light.Type.Probe) {
                probes.add((LightProbe)l);
                continue;
            }
            packedLight = true;
            tempColor.set(l.getColor()).setAlpha(l.getType().getId());
            rasters[0].setPixel(i, 0, tempColor);
            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight)l;
                    vectorToColor(dl.getDirection(), tempColor).a = 0;
                    rasters[1].setPixel(i, 0, tempColor);
                    if (useTiles) {
                        frustum.fromDirectional(dl).write(tileIndices, tileInfo, i);
                    }
                    break;
                case Point:
                    PointLight pl = (PointLight)l;
                    vectorToColor(pl.getPosition(), tempColor);
                    tempColor.a = pl.getInvRadius();
                    rasters[1].setPixel(i, 0, tempColor);
                    if (useTiles) {
                        frustum.fromPoint(pl).write(tileIndices, tileInfo, i);
                    }
                    break;
                case Spot:
                    SpotLight sl = (SpotLight)l;
                    vectorToColor(sl.getPosition(), tempColor);
                    tempColor.a = sl.getInvSpotRange();
                    rasters[1].setPixel(i, 0, tempColor);
                    vectorToColor(sl.getDirection(), tempColor);
                    tempColor.a = sl.getPackedAngleCos();
                    rasters[2].setPixel(i, 0, tempColor);
                    spotlight = true;
                    if (useTiles) {
                        frustum.fromSpot(sl).write(tileIndices, tileInfo, i);
                    }
                    break;
            }
            if (++i >= limit) {
                break;
            }
        }
        if (packedLight) {
            textures[0].getImage().setUpdateNeeded();
            textures[1].getImage().setUpdateNeeded();
            if (spotlight) {
                textures[2].getImage().setUpdateNeeded();
            }
            if (useTiles) {
                packLightIndices();
            }
        }
        return i;
    }
    private void packLightIndices() {
        int componentIndex = 0;
        int xIndex = 0, yIndex = 0;
        int tileX = 0, tileY = 0;
        final int indexWidth = textures[4].getImage().getWidth();
        final int tileWidth = textures[3].getImage().getWidth();
        final ColorRGBA tileInfoColor = new ColorRGBA();
        tempColor.set(0, 0, 0, 0);
        for (LinkedList<Integer> l : tileIndices) {
            // raster tile info to texture
            tileInfoColor.r = xIndex;
            tileInfoColor.g = yIndex;
            tileInfoColor.b = componentIndex;
            tileInfoColor.a = l.size();
            rasters[3].setPixel(tileX, tileY, tileInfoColor);
            if (++tileX >= tileWidth) {
                tileX = 0;
                tileY++;
            }
            // raster light indices to texture
            for (int index : l) {
                // pack 4 indices per pixel
                switch (componentIndex) {
                    case 0: tempColor.r = index; break;
                    case 1: tempColor.g = index; break;
                    case 2: tempColor.b = index; break;
                    case 3: tempColor.a = index; break;
                }
                if (++componentIndex > 3) {
                    componentIndex = 0;
                    rasters[4].setPixel(xIndex, yIndex, tempColor);
                    if (++xIndex >= indexWidth) {
                        xIndex = 0;
                        yIndex++;
                    }
                }
            }
            l.clear();
        }
        // if the index color is incomplete, raster it to the texture
        if (componentIndex != 0) {
            rasters[4].setPixel(xIndex, yIndex, tempColor);
        }
        textures[3].getImage().setUpdateNeeded();
        textures[4].getImage().setUpdateNeeded();
    }
    
    public boolean hasAmbientLight() {
        return hasAmbient;
    }
    
    private void validateSamples(Texture2D tex) {
        if (tex.getImage().getMultiSamples() != 1) {
            throw new IllegalArgumentException("Texture cannot be multisampled.");
        }
    }
    private void validateSize(Texture2D base, Texture2D target) {
        Image baseImg = base.getImage();
        Image targetImg = target.getImage();
        if (baseImg.getWidth() != targetImg.getWidth() || baseImg.getHeight() != targetImg.getHeight()) {
            throw new IllegalArgumentException("Texture has incorrect demensions.");
        }
    }
    private void updateTexture(int i, Texture2D tex) {
        if (tex == null) {
            textures[i] = null;
            rasters[i] = null;
        } else {
            validateSamples(tex);
            if (textures[i] != tex) {
                textures[i] = tex;
                rasters[i] = ImageRaster.create(tex.getImage());
            }
        }
    }
    private ColorRGBA vectorToColor(Vector3f vec, ColorRGBA color) {
        if (color == null) {
            color = new ColorRGBA();
        }
        color.r = vec.x;
        color.g = vec.y;
        color.b = vec.z;
        return color;
    }
    
}
