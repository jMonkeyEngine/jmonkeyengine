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
package com.jme3.scene.plugins.blender.materials;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

public class MaterialHelper extends AbstractBlenderHelper {
    private static final Logger              LOGGER                = Logger.getLogger(MaterialHelper.class.getName());
    protected static final float             DEFAULT_SHININESS     = 20.0f;

    public static final String               TEXTURE_TYPE_COLOR    = "ColorMap";
    public static final String               TEXTURE_TYPE_DIFFUSE  = "DiffuseMap";
    public static final String               TEXTURE_TYPE_NORMAL   = "NormalMap";
    public static final String               TEXTURE_TYPE_SPECULAR = "SpecularMap";
    public static final String               TEXTURE_TYPE_GLOW     = "GlowMap";
    public static final String               TEXTURE_TYPE_ALPHA    = "AlphaMap";
    public static final String               TEXTURE_TYPE_LIGHTMAP = "LightMap";

    public static final Integer              ALPHA_MASK_NONE       = Integer.valueOf(0);
    public static final Integer              ALPHA_MASK_CIRCLE     = Integer.valueOf(1);
    public static final Integer              ALPHA_MASK_CONE       = Integer.valueOf(2);
    public static final Integer              ALPHA_MASK_HYPERBOLE  = Integer.valueOf(3);
    protected final Map<Integer, IAlphaMask> alphaMasks            = new HashMap<Integer, IAlphaMask>();

    /**
     * The type of the material's diffuse shader.
     */
    public static enum DiffuseShader {
        LAMBERT, ORENNAYAR, TOON, MINNAERT, FRESNEL
    }

    /**
     * The type of the material's specular shader.
     */
    public static enum SpecularShader {
        COOKTORRENCE, PHONG, BLINN, TOON, WARDISO
    }

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public MaterialHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
        // setting alpha masks
        alphaMasks.put(ALPHA_MASK_NONE, new IAlphaMask() {
            public void setImageSize(int width, int height) {
            }

            public byte getAlpha(float x, float y) {
                return (byte) 255;
            }
        });
        alphaMasks.put(ALPHA_MASK_CIRCLE, new IAlphaMask() {
            private float   r;
            private float[] center;

            public void setImageSize(int width, int height) {
                r = Math.min(width, height) * 0.5f;
                center = new float[] { width * 0.5f, height * 0.5f };
            }

            public byte getAlpha(float x, float y) {
                float d = FastMath.abs(FastMath.sqrt((x - center[0]) * (x - center[0]) + (y - center[1]) * (y - center[1])));
                return (byte) (d >= r ? 0 : 255);
            }
        });
        alphaMasks.put(ALPHA_MASK_CONE, new IAlphaMask() {
            private float   r;
            private float[] center;

            public void setImageSize(int width, int height) {
                r = Math.min(width, height) * 0.5f;
                center = new float[] { width * 0.5f, height * 0.5f };
            }

            public byte getAlpha(float x, float y) {
                float d = FastMath.abs(FastMath.sqrt((x - center[0]) * (x - center[0]) + (y - center[1]) * (y - center[1])));
                return (byte) (d >= r ? 0 : -255.0f * d / r + 255.0f);
            }
        });
        alphaMasks.put(ALPHA_MASK_HYPERBOLE, new IAlphaMask() {
            private float   r;
            private float[] center;

            public void setImageSize(int width, int height) {
                r = Math.min(width, height) * 0.5f;
                center = new float[] { width * 0.5f, height * 0.5f };
            }

            public byte getAlpha(float x, float y) {
                float d = FastMath.abs(FastMath.sqrt((x - center[0]) * (x - center[0]) + (y - center[1]) * (y - center[1]))) / r;
                return d >= 1.0f ? 0 : (byte) ((-FastMath.sqrt((2.0f - d) * d) + 1.0f) * 255.0f);
            }
        });
    }

    /**
     * This method converts the material structure to jme Material.
     * @param structure
     *            structure with material data
     * @param blenderContext
     *            the blender context
     * @return jme material
     * @throws BlenderFileException
     *             an exception is throw when problems with blend file occur
     */
    public MaterialContext toMaterialContext(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading material.");
        MaterialContext result = (MaterialContext) blenderContext.getLoadedFeature(structure.getOldMemoryAddress(), LoadedDataType.FEATURE);
        if (result != null) {
            return result;
        }

        result = new MaterialContext(structure, blenderContext);
        LOGGER.log(Level.FINE, "Material''s name: {0}", result.name);
        Long oma = structure.getOldMemoryAddress();
        blenderContext.addLoadedFeatures(oma, LoadedDataType.STRUCTURE, structure);
        blenderContext.addLoadedFeatures(oma, LoadedDataType.FEATURE, result);
        return result;
    }

    /**
     * This method converts the given material into particles-usable material.
     * The texture and glow color are being copied.
     * The method assumes it receives the Lighting type of material.
     * @param material
     *            the source material
     * @param blenderContext
     *            the blender context
     * @return material converted into particles-usable material
     */
    public Material getParticlesMaterial(Material material, Integer alphaMaskIndex, BlenderContext blenderContext) {
        Material result = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");

        // copying texture
        MatParam diffuseMap = material.getParam("DiffuseMap");
        if (diffuseMap != null) {
            Texture texture = ((Texture) diffuseMap.getValue()).clone();

            // applying alpha mask to the texture
            Image image = texture.getImage();
            ByteBuffer sourceBB = image.getData(0);
            sourceBB.rewind();
            int w = image.getWidth();
            int h = image.getHeight();
            ByteBuffer bb = BufferUtils.createByteBuffer(w * h * 4);
            IAlphaMask iAlphaMask = alphaMasks.get(alphaMaskIndex);
            iAlphaMask.setImageSize(w, h);

            for (int x = 0; x < w; ++x) {
                for (int y = 0; y < h; ++y) {
                    bb.put(sourceBB.get());
                    bb.put(sourceBB.get());
                    bb.put(sourceBB.get());
                    bb.put(iAlphaMask.getAlpha(x, y));
                }
            }

            image = new Image(Format.RGBA8, w, h, bb);
            texture.setImage(image);

            result.setTextureParam("Texture", VarType.Texture2D, texture);
        }

        // copying glow color
        MatParam glowColor = material.getParam("GlowColor");
        if (glowColor != null) {
            ColorRGBA color = (ColorRGBA) glowColor.getValue();
            result.setParam("GlowColor", VarType.Vector3, color);
        }
        return result;
    }

    /**
     * This method returns the table of materials connected to the specified structure. The given structure can be of any type (ie. mesh or
     * curve) but needs to have 'mat' field/
     * 
     * @param structureWithMaterials
     *            the structure containing the mesh data
     * @param blenderContext
     *            the blender context
     * @return a list of vertices colors, each color belongs to a single vertex
     * @throws BlenderFileException
     *             this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    public MaterialContext[] getMaterials(Structure structureWithMaterials, BlenderContext blenderContext) throws BlenderFileException {
        Pointer ppMaterials = (Pointer) structureWithMaterials.getFieldValue("mat");
        MaterialContext[] materials = null;
        if (ppMaterials.isNotNull()) {
            List<Structure> materialStructures = ppMaterials.fetchData();
            if (materialStructures != null && materialStructures.size() > 0) {
                MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);
                materials = new MaterialContext[materialStructures.size()];
                int i = 0;
                for (Structure s : materialStructures) {
                    materials[i++] = s == null ? null : materialHelper.toMaterialContext(s, blenderContext);
                }
            }
        }
        return materials;
    }

    /**
     * This method converts rgb values to hsv values.
     * 
     * @param r
     *            red value of the color
     * @param g
     *            green value of the color
     * @param b
     *            blue value of the color
     * @param hsv
     *            hsv values of a color (this table contains the result of the transformation)
     */
    public void rgbToHsv(float r, float g, float b, float[] hsv) {
        float cmax = r;
        float cmin = r;
        cmax = g > cmax ? g : cmax;
        cmin = g < cmin ? g : cmin;
        cmax = b > cmax ? b : cmax;
        cmin = b < cmin ? b : cmin;

        hsv[2] = cmax; /* value */
        if (cmax != 0.0) {
            hsv[1] = (cmax - cmin) / cmax;
        } else {
            hsv[1] = 0.0f;
            hsv[0] = 0.0f;
        }
        if (hsv[1] == 0.0) {
            hsv[0] = -1.0f;
        } else {
            float cdelta = cmax - cmin;
            float rc = (cmax - r) / cdelta;
            float gc = (cmax - g) / cdelta;
            float bc = (cmax - b) / cdelta;
            if (r == cmax) {
                hsv[0] = bc - gc;
            } else if (g == cmax) {
                hsv[0] = 2.0f + rc - bc;
            } else {
                hsv[0] = 4.0f + gc - rc;
            }
            hsv[0] *= 60.0f;
            if (hsv[0] < 0.0f) {
                hsv[0] += 360.0f;
            }
        }

        hsv[0] /= 360.0f;
        if (hsv[0] < 0.0f) {
            hsv[0] = 0.0f;
        }
    }

    /**
     * This method converts rgb values to hsv values.
     * 
     * @param h
     *            hue
     * @param s
     *            saturation
     * @param v
     *            value
     * @param rgb
     *            rgb result vector (should have 3 elements)
     */
    public void hsvToRgb(float h, float s, float v, float[] rgb) {
        h *= 360.0f;
        if (s == 0.0) {
            rgb[0] = rgb[1] = rgb[2] = v;
        } else {
            if (h == 360) {
                h = 0;
            } else {
                h /= 60;
            }
            int i = (int) Math.floor(h);
            float f = h - i;
            float p = v * (1.0f - s);
            float q = v * (1.0f - s * f);
            float t = v * (1.0f - s * (1.0f - f));
            switch (i) {
                case 0:
                    rgb[0] = v;
                    rgb[1] = t;
                    rgb[2] = p;
                    break;
                case 1:
                    rgb[0] = q;
                    rgb[1] = v;
                    rgb[2] = p;
                    break;
                case 2:
                    rgb[0] = p;
                    rgb[1] = v;
                    rgb[2] = t;
                    break;
                case 3:
                    rgb[0] = p;
                    rgb[1] = q;
                    rgb[2] = v;
                    break;
                case 4:
                    rgb[0] = t;
                    rgb[1] = p;
                    rgb[2] = v;
                    break;
                case 5:
                    rgb[0] = v;
                    rgb[1] = p;
                    rgb[2] = q;
                    break;
            }
        }
    }
}
