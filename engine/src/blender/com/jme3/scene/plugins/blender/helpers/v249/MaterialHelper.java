/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.scene.plugins.blender.helpers.v249;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

public class MaterialHelper extends AbstractBlenderHelper {

    private static final Logger LOGGER = Logger.getLogger(MaterialHelper.class.getName());
    protected static final float DEFAULT_SHININESS = 20.0f;
    public static final String TEXTURE_TYPE_COLOR = "ColorMap";
    public static final String TEXTURE_TYPE_DIFFUSE = "DiffuseMap";
    public static final String TEXTURE_TYPE_NORMAL = "NormalMap";
    public static final String TEXTURE_TYPE_SPECULAR = "SpecularMap";
    public static final String TEXTURE_TYPE_GLOW = "GlowMap";
    public static final String TEXTURE_TYPE_ALPHA = "AlphaMap";
    public static final Integer ALPHA_MASK_NONE = Integer.valueOf(0);
    public static final Integer ALPHA_MASK_CIRCLE = Integer.valueOf(1);
    public static final Integer ALPHA_MASK_CONE = Integer.valueOf(2);
    public static final Integer ALPHA_MASK_HYPERBOLE = Integer.valueOf(3);
    protected final Map<Integer, AlphaMask> alphaMasks = new HashMap<Integer, AlphaMask>();

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
    /** Face cull mode. Should be excplicitly set before this helper is used. */
    protected FaceCullMode faceCullMode;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * 
     * @param blenderVersion
     *        the version read from the blend file
     */
    public MaterialHelper(String blenderVersion) {
        super(blenderVersion);
        // setting alpha masks
        alphaMasks.put(ALPHA_MASK_NONE, new AlphaMask() {

            @Override
            public void setImageSize(int width, int height) {
            }

            @Override
            public byte getAlpha(float x, float y) {
                return (byte) 255;
            }
        });
        alphaMasks.put(ALPHA_MASK_CIRCLE, new AlphaMask() {

            private float r;
            private float[] center;

            @Override
            public void setImageSize(int width, int height) {
                r = Math.min(width, height) * 0.5f;
                center = new float[]{width * 0.5f, height * 0.5f};
            }

            @Override
            public byte getAlpha(float x, float y) {
                float d = FastMath.abs(FastMath.sqrt((x - center[0]) * (x - center[0]) + (y - center[1]) * (y - center[1])));
                return (byte) (d >= r ? 0 : 255);
            }
        });
        alphaMasks.put(ALPHA_MASK_CONE, new AlphaMask() {

            private float r;
            private float[] center;

            @Override
            public void setImageSize(int width, int height) {
                r = Math.min(width, height) * 0.5f;
                center = new float[]{width * 0.5f, height * 0.5f};
            }

            @Override
            public byte getAlpha(float x, float y) {
                float d = FastMath.abs(FastMath.sqrt((x - center[0]) * (x - center[0]) + (y - center[1]) * (y - center[1])));
                return (byte) (d >= r ? 0 : -255.0f * d / r + 255.0f);
            }
        });
        alphaMasks.put(ALPHA_MASK_HYPERBOLE, new AlphaMask() {

            private float r;
            private float[] center;

            @Override
            public void setImageSize(int width, int height) {
                r = Math.min(width, height) * 0.5f;
                center = new float[]{width * 0.5f, height * 0.5f};
            }

            @Override
            public byte getAlpha(float x, float y) {
                float d = FastMath.abs(FastMath.sqrt((x - center[0]) * (x - center[0]) + (y - center[1]) * (y - center[1]))) / r;
                return d >= 1.0f ? 0 : (byte) ((-FastMath.sqrt((2.0f - d) * d) + 1.0f) * 255.0f);
            }
        });
    }

    /**
     * This method sets the face cull mode to be used with every loaded material.
     * 
     * @param faceCullMode
     *        the face cull mode
     */
    public void setFaceCullMode(FaceCullMode faceCullMode) {
        this.faceCullMode = faceCullMode;
    }

    @SuppressWarnings("unchecked")
    public Material toMaterial(Structure structure, DataRepository dataRepository) throws BlenderFileException {
        LOGGER.log(Level.INFO, "Loading material.");
        if (structure == null) {
            return dataRepository.getDefaultMaterial();
        }
        Material result = (Material) dataRepository.getLoadedFeature(structure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (result != null) {
            return result;
        }

        int mode = ((Number) structure.getFieldValue("mode")).intValue();
        boolean shadeless = (mode & 0x4) != 0;
        boolean vertexColor = (mode & 0x16) != 0;
        boolean transparent = (mode & 0x64) != 0;

        if (shadeless) {
            result = new Material(dataRepository.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        } else {
            result = new Material(dataRepository.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        }

        result.getAdditionalRenderState().setFaceCullMode(faceCullMode);

        if (transparent) {
            result.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        }

        String name = structure.getName();
        LOGGER.log(Level.INFO, "Material's name: {0}", name);
        if (vertexColor) {
            if (shadeless) {
                result.setBoolean("VertexColor", true);
            } else {
                result.setBoolean("UseVertexColor", true);
            }
        }

        MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
        ColorRGBA diffuseColor = null;
        if (shadeless) {
            // color of shadeless? doesn't seem to work in blender ..
        } else {
            result.setBoolean("UseMaterialColors", true);

            // setting the colors
            DiffuseShader diffuseShader = materialHelper.getDiffuseShader(structure);
            result.setBoolean("Minnaert", diffuseShader == DiffuseShader.MINNAERT);
            diffuseColor = materialHelper.getDiffuseColor(structure, diffuseShader);
            result.setColor("Diffuse", diffuseColor);

            SpecularShader specularShader = materialHelper.getSpecularShader(structure);
            result.setBoolean("WardIso", specularShader == SpecularShader.WARDISO);
            result.setColor("Specular", materialHelper.getSpecularColor(structure, specularShader));

            result.setColor("Ambient", materialHelper.getAmbientColor(structure));
            result.setFloat("Shininess", materialHelper.getShininess(structure));
        }

        // texture
        if ((dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.TEXTURES) != 0) {
            TextureHelper textureHelper = dataRepository.getHelper(TextureHelper.class);
            DynamicArray<Pointer> mtexs = (DynamicArray<Pointer>) structure.getFieldValue("mtex");
            for (int i = 0; i < mtexs.getTotalSize(); ++i) {
                Pointer p = mtexs.get(i);
                if (!p.isNull()) {
                    List<Structure> mtex = p.fetchData(dataRepository.getInputStream());
                    if (mtex.size() == 1) {
                        Structure textureLink = mtex.get(0);
                        int texflag = ((Number) textureLink.getFieldValue("texflag")).intValue();
                        // int texco = ((Number) textureLink.getFieldValue("texco")).intValue();
                        boolean negateTexture = (texflag & 0x04) == 0;

                        // if(texco == 0x10) {//TEXCO_UV (this is only supported now)
                        int mapto = ((Number) textureLink.getFieldValue("mapto")).intValue();
                        if (mapto != 0) {
                            Pointer pTex = (Pointer) textureLink.getFieldValue("tex");
                            Structure tex = pTex.fetchData(dataRepository.getInputStream()).get(0);
                            Texture texture = textureHelper.getTexture(tex, dataRepository);
                            if (texture != null) {
                                if ((mapto & 0x01) != 0) {// Col
                                    result.setBoolean("UseMaterialColors", Boolean.FALSE);
                                    // blending the texture with material color and texture's defined color
                                    int blendType = ((Number) textureLink.getFieldValue("blendtype")).intValue();
                                    float[] color = new float[]{((Number) textureLink.getFieldValue("r")).floatValue(), ((Number) textureLink.getFieldValue("g")).floatValue(), ((Number) textureLink.getFieldValue("b")).floatValue()};
                                    float colfac = ((Number) textureLink.getFieldValue("colfac")).floatValue();
                                    texture = textureHelper.blendTexture(diffuseColor.getColorArray(), texture, color, colfac, blendType, negateTexture, dataRepository);
                                    texture.setWrap(WrapMode.Repeat);
                                    if (shadeless) {
                                        result.setTexture(TEXTURE_TYPE_COLOR, texture);
                                    } else {
                                        result.setTexture(TEXTURE_TYPE_DIFFUSE, texture);
                                    }
                                }
                                if ((mapto & 0x02) != 0) {// Nor
                                    result.setTexture(TEXTURE_TYPE_NORMAL, texture);
                                }
                                if ((mapto & 0x20) != 0) {// Spec
                                    result.setTexture(TEXTURE_TYPE_SPECULAR, texture);
                                }
                                if ((mapto & 0x40) != 0) {// Emit
                                    result.setTexture(TEXTURE_TYPE_GLOW, texture);
                                }
                                if ((mapto & 0x80) != 0) {// Alpha
                                    result.setTexture(TEXTURE_TYPE_ALPHA, texture);
                                }
                            } else {
                                LOGGER.log(Level.WARNING, "Texture not found!");
                            }
                        }
                        // } else {
                        // Pointer pTex = (Pointer)textureLink.getFieldValue("tex");
                        // List<Structure> texs = pTex.fetchData(dataRepository.getInputStream());
                        // Structure tex = texs.get(0);
                        // LOGGER.log(Level.WARNING, "Unsupported texture type: " + texco);
                        // }
                    } else {
                        LOGGER.log(Level.WARNING, "Many textures. Not solved yet!");// TODO
                    }
                }
            }
        }
        dataRepository.addLoadedFeatures(structure.getOldMemoryAddress(), structure.getName(), structure, result);
        return result;
    }

    /**
     * This method returns a material similar to the one given but without textures. If the material has no textures it is not cloned but
     * returned itself.
     * 
     * @param material
     *        a material to be cloned without textures
     * @param imageType
     *        type of image defined by blender; the constants are defined in TextureHelper
     * @return material without textures of a specified type
     */
    public Material getNonTexturedMaterial(Material material, int imageType) {
        String[] textureParamNames = new String[]{TEXTURE_TYPE_DIFFUSE, TEXTURE_TYPE_NORMAL, TEXTURE_TYPE_GLOW, TEXTURE_TYPE_SPECULAR, TEXTURE_TYPE_ALPHA};
        Map<String, Texture> textures = new HashMap<String, Texture>(textureParamNames.length);
        for (String textureParamName : textureParamNames) {
            MatParamTexture matParamTexture = material.getTextureParam(textureParamName);
            if (matParamTexture != null) {
                textures.put(textureParamName, matParamTexture.getTextureValue());
            }
        }
        if (textures.isEmpty()) {
            return material;
        } else {
            // clear all textures first so that wo de not waste resources cloning them
            for (Entry<String, Texture> textureParamName : textures.entrySet()) {
                String name = textureParamName.getValue().getName();
                try {
                    int type = Integer.parseInt(name);
                    if (type == imageType) {
                        material.clearParam(textureParamName.getKey());
                    }
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "The name of the texture does not contain the texture type value! {0} will not be removed!", name);
                }
            }
            Material result = material.clone();
            // put the textures back in place
            for (Entry<String, Texture> textureEntry : textures.entrySet()) {
                material.setTexture(textureEntry.getKey(), textureEntry.getValue());
            }
            return result;
        }
    }

    /**
     * This method converts the given material into particles-usable material.
     * The texture and glow color are being copied.
     * The method assumes it receives the Lighting type of material.
     * @param material
     *        the source material
     * @param dataRepository
     *        the data repository
     * @return material converted into particles-usable material
     */
    public Material getParticlesMaterial(Material material, Integer alphaMaskIndex, DataRepository dataRepository) {
        Material result = new Material(dataRepository.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");

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
            AlphaMask iAlphaMask = alphaMasks.get(alphaMaskIndex);
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
     * This method indicates if the material has a texture of a specified type.
     * 
     * @param material
     *        the material
     * @param textureType
     *        the type of the texture
     * @return <b>true</b> if the texture exists in the material and <B>false</b> otherwise
     */
    public boolean hasTexture(Material material, String textureType) {
        if (material != null) {
            return material.getTextureParam(textureType) != null;
        }
        return false;
    }

    /**
     * This method returns the diffuse color
     * 
     * @param materialStructure
     * @param diffuseShader
     * @return
     */
    public ColorRGBA getDiffuseColor(Structure materialStructure, DiffuseShader diffuseShader) {
        // bitwise 'or' of all textures mappings
        int commonMapto = ((Number) materialStructure.getFieldValue("mapto")).intValue();

        // diffuse color
        float r = ((Number) materialStructure.getFieldValue("r")).floatValue();
        float g = ((Number) materialStructure.getFieldValue("g")).floatValue();
        float b = ((Number) materialStructure.getFieldValue("b")).floatValue();
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        if ((commonMapto & 0x01) == 0x01) {// Col
            return new ColorRGBA(r, g, b, alpha);
        } else {
            switch (diffuseShader) {
                case FRESNEL:
                case ORENNAYAR:
                case TOON:
                    break;// TODO: find what is the proper modification
                case MINNAERT:
                case LAMBERT:// TODO: check if that is correct
                    float ref = ((Number) materialStructure.getFieldValue("ref")).floatValue();
                    r *= ref;
                    g *= ref;
                    b *= ref;
                    break;
                default:
                    throw new IllegalStateException("Unknown diffuse shader type: " + diffuseShader.toString());
            }
            return new ColorRGBA(r, g, b, alpha);
        }
    }

    /**
     * This method returns an enum describing the type of a diffuse shader used by this material.
     * 
     * @param materialStructure
     *        the material structure filled with data
     * @return an enum describing the type of a diffuse shader used by this material
     */
    public DiffuseShader getDiffuseShader(Structure materialStructure) {
        int diff_shader = ((Number) materialStructure.getFieldValue("diff_shader")).intValue();
        return DiffuseShader.values()[diff_shader];
    }

    /**
     * This method returns an ambient color used by the material.
     * 
     * @param materialStructure
     *        the material structure filled with data
     * @return an ambient color used by the material
     */
    public ColorRGBA getAmbientColor(Structure materialStructure) {
        float r = ((Number) materialStructure.getFieldValue("ambr")).floatValue();
        float g = ((Number) materialStructure.getFieldValue("ambg")).floatValue();
        float b = ((Number) materialStructure.getFieldValue("ambb")).floatValue();
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        return new ColorRGBA(r, g, b, alpha);
    }

    /**
     * This method returns an enum describing the type of a specular shader used by this material.
     * 
     * @param materialStructure
     *        the material structure filled with data
     * @return an enum describing the type of a specular shader used by this material
     */
    public SpecularShader getSpecularShader(Structure materialStructure) {
        int spec_shader = ((Number) materialStructure.getFieldValue("spec_shader")).intValue();
        return SpecularShader.values()[spec_shader];
    }

    /**
     * This method returns a specular color used by the material.
     * 
     * @param materialStructure
     *        the material structure filled with data
     * @return a specular color used by the material
     */
    public ColorRGBA getSpecularColor(Structure materialStructure, SpecularShader specularShader) {
        float r = ((Number) materialStructure.getFieldValue("specr")).floatValue();
        float g = ((Number) materialStructure.getFieldValue("specg")).floatValue();
        float b = ((Number) materialStructure.getFieldValue("specb")).floatValue();
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        switch (specularShader) {
            case BLINN:
            case COOKTORRENCE:
            case TOON:
            case WARDISO:// TODO: find what is the proper modification
                break;
            case PHONG:// TODO: check if that is correct
                float spec = ((Number) materialStructure.getFieldValue("spec")).floatValue();
                r *= spec * 0.5f;
                g *= spec * 0.5f;
                b *= spec * 0.5f;
                break;
            default:
                throw new IllegalStateException("Unknown specular shader type: " + specularShader.toString());
        }
        return new ColorRGBA(r, g, b, alpha);
    }

    /**
     * This method returns the sihiness of this material or DEFAULT_SHININESS value if not present.
     * 
     * @param materialStructure
     *        the material structure filled with data
     * @return the sihiness of this material or DEFAULT_SHININESS value if not present
     */
    public float getShininess(Structure materialStructure) {
        float shininess = ((Number) materialStructure.getFieldValue("emit")).floatValue();
        return shininess > 0.0f ? shininess : DEFAULT_SHININESS;
    }

    /**
     * This method returns the table of materials connected to the specified structure. The given structure can be of any type (ie. mesh or
     * curve) but needs to have 'mat' field/
     * 
     * @param structureWithMaterials
     *        the structure containing the mesh data
     * @param dataRepository
     *        the data repository
     * @return a list of vertices colors, each color belongs to a single vertex
     * @throws BlenderFileException
     *         this exception is thrown when the blend file structure is somehow invalid or corrupted
     */
    public Material[] getMaterials(Structure structureWithMaterials, DataRepository dataRepository) throws BlenderFileException {
        Pointer ppMaterials = (Pointer) structureWithMaterials.getFieldValue("mat");
        Material[] materials = null;
        if (!ppMaterials.isNull()) {
            List<Structure> materialStructures = ppMaterials.fetchData(dataRepository.getInputStream());
            if (materialStructures != null && materialStructures.size() > 0) {
                MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
                materials = new Material[materialStructures.size()];
                int i = 0;
                for (Structure s : materialStructures) {
                    Material material = (Material) dataRepository.getLoadedFeature(s.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
                    if (material == null) {
                        material = materialHelper.toMaterial(s, dataRepository);
                    }
                    materials[i++] = material;
                }
            }
        }
        return materials;
    }

    /**
     * This method converts rgb values to hsv values.
     * 
     * @param rgb
     *        rgb values of the color
     * @param hsv
     *        hsv values of a color (this table contains the result of the transformation)
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
     *        hue
     * @param s
     *        saturation
     * @param v
     *        value
     * @param rgb
     *        rgb result vector (should have 3 elements)
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

    /**
     * An interface used in calculating alpha mask during particles' texture calculations.
     * @author Marcin Roguski (Kaelthas)
     */
    protected static interface AlphaMask {

        /**
         * This method sets the size of the texture's image.
         * @param width
         *        the width of the image
         * @param height
         *        the height of the image
         */
        void setImageSize(int width, int height);

        /**
         * This method returns the alpha value for the specified texture position.
         * @param x
         *        the X coordinate of the texture position
         * @param y
         *        the Y coordinate of the texture position
         * @return the alpha value for the specified texture position
         */
        byte getAlpha(float x, float y);
    }
}
