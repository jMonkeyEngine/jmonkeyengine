/*
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.material;

import com.jme3.asset.Asset;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.export.*;
import com.jme3.light.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.TechniqueDef.ShadowMode;
import com.jme3.math.*;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.util.ListMap;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>Material</code> describes the rendering style for a given
 * {@link Geometry}.
 * <p>A material is essentially a list of {@link MatParam parameters},
 * those parameters map to uniforms which are defined in a shader.
 * Setting the parameters can modify the behavior of a
 * shader.
 * <p/>
 * @author Kirill Vainer
 */
public class Material implements Asset, Cloneable, Savable, Comparable<Material> {

    // Version #2: Fixed issue with RenderState.apply*** flags not getting exported
    public static final int SAVABLE_VERSION = 2;
    
    private static final Logger logger = Logger.getLogger(Material.class.getName());
    private static final RenderState additiveLight = new RenderState();
    private static final RenderState depthOnly = new RenderState();
    private static final Quaternion nullDirLight = new Quaternion(0, -1, 0, -1);

    static {
        depthOnly.setDepthTest(true);
        depthOnly.setDepthWrite(true);
        depthOnly.setFaceCullMode(RenderState.FaceCullMode.Back);
        depthOnly.setColorWrite(false);

        additiveLight.setBlendMode(RenderState.BlendMode.AlphaAdditive);
        additiveLight.setDepthWrite(false);
    }
    private AssetKey key;
    private String name;
    private MaterialDef def;
    private ListMap<String, MatParam> paramValues = new ListMap<String, MatParam>();
    private Technique technique;
    private HashMap<String, Technique> techniques = new HashMap<String, Technique>();
    private int nextTexUnit = 0;
    private RenderState additionalState = null;
    private RenderState mergedRenderState = new RenderState();
    private boolean transparent = false;
    private boolean receivesShadows = false;
    private int sortingId = -1;
    private transient ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);

    public Material(MaterialDef def) {
        if (def == null) {
            throw new NullPointerException("Material definition cannot be null");
        }
        this.def = def;

        // Load default values from definition (if any)
        for (MatParam param : def.getMaterialParams()){
            if (param.getValue() != null){
                setParam(param.getName(), param.getVarType(), param.getValue());
            }
        }
    }

    public Material(AssetManager contentMan, String defName) {
        this((MaterialDef) contentMan.loadAsset(new AssetKey(defName)));
    }

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public Material() {
    }

    /**
     * Returns the asset key name of the asset from which this material was loaded.
     *
     * <p>This value will be <code>null</code> unless this material was loaded
     * from a .j3m file.
     *
     * @return Asset key name of the j3m file
     */
    public String getAssetName() {
        return key != null ? key.getName() : null;
    }
    
    /**
     * @return the name of the material (not the same as the asset name), the returned value can be null
     */
    public String getName() {
		return name;
	}
    
    /**
     * This method sets the name of the material.
     * The name is not the same as the asset name.
     * It can be null and there is no guarantee of its uniqness.
     * @param name the name of the material
     */
    public void setName(String name) {
		this.name = name;
	}

    public void setKey(AssetKey key) {
        this.key = key;
    }

    public AssetKey getKey() {
        return key;
    }

    /**
     * Returns the sorting ID or sorting index for this material.
     *
     * <p>The sorting ID is used internally by the system to sort rendering
     * of geometries. It sorted to reduce shader switches, if the shaders
     * are equal, then it is sorted by textures.
     *
     * @return The sorting ID used for sorting geometries for rendering.
     */
    public int getSortId() {
        Technique t = getActiveTechnique();
        if (sortingId == -1 && t != null && t.getShader() != null) {
            int texId = -1;
            for (int i = 0; i < paramValues.size(); i++) {
                MatParam param = paramValues.getValue(i);
                if (param instanceof MatParamTexture) {
                    MatParamTexture tex = (MatParamTexture) param;
                    if (tex.getTextureValue() != null && tex.getTextureValue().getImage() != null) {
                        if (texId == -1) {
                            texId = 0;
                        }
                        texId += tex.getTextureValue().getImage().getId() % 0xff;
                    }
                }
            }
            sortingId = texId + t.getShader().getId() * 1000;
        }
        return sortingId;
    }

    /**
     * Uses the sorting ID for each material to compare them.
     *
     * @param m The other material to compare to.
     *
     * @return zero if the materials are equal, returns a negative value
     * if <code>this</code> has a lower sorting ID than <code>m</code>,
     * otherwise returns a positive value.
     */
    public int compareTo(Material m) {
        return m.getSortId() - getSortId();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Material){
            return ((Material)obj).compareTo(this) == 0;
        }
        return super.equals(obj);
    }
    
    /**
     * Clones this material. The result is returned.
     */
    @Override
    public Material clone() {
        try {
            Material mat = (Material) super.clone();

            if (additionalState != null) {
                mat.additionalState = additionalState.clone();
            }
            mat.technique = null;
            mat.techniques = new HashMap<String, Technique>();

            mat.paramValues = new ListMap<String, MatParam>();
            for (int i = 0; i < paramValues.size(); i++) {
                Map.Entry<String, MatParam> entry = paramValues.getEntry(i);
                mat.paramValues.put(entry.getKey(), entry.getValue().clone());
            }

            return mat;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Returns the currently active technique.
     * <p>
     * The technique is selected automatically by the {@link RenderManager}
     * based on system capabilities. Users may select their own
     * technique by using
     * {@link #selectTechnique(java.lang.String, com.jme3.renderer.RenderManager) }.
     *
     * @return the currently active technique.
     *
     * @see #selectTechnique(java.lang.String, com.jme3.renderer.RenderManager)
     */
    public Technique getActiveTechnique() {
        return technique;
    }

    /**
     * Check if the transparent value marker is set on this material.
     * @return True if the transparent value marker is set on this material.
     * @see #setTransparent(boolean)
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Set the transparent value marker.
     *
     * <p>This value is merely a marker, by itself it does nothing.
     * Generally model loaders will use this marker to indicate further
     * up that the material is transparent and therefore any geometries
     * using it should be put into the {@link Bucket#Transparent transparent
     * bucket}.
     *
     * @param transparent the transparent value marker.
     */
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    /**
     * Check if the material should receive shadows or not.
     *
     * @return True if the material should receive shadows.
     *
     * @see Material#setReceivesShadows(boolean)
     */
    public boolean isReceivesShadows() {
        return receivesShadows;
    }

    /**
     * Set if the material should receive shadows or not.
     *
     * <p>This value is merely a marker, by itself it does nothing.
     * Generally model loaders will use this marker to indicate
     * the material should receive shadows and therefore any
     * geometries using it should have the {@link ShadowMode#Receive} set
     * on them.
     *
     * @param receivesShadows if the material should receive shadows or not.
     */
    public void setReceivesShadows(boolean receivesShadows) {
        this.receivesShadows = receivesShadows;
    }

    /**
     * Acquire the additional {@link RenderState render state} to apply
     * for this material.
     *
     * <p>The first call to this method will create an additional render
     * state which can be modified by the user to apply any render
     * states in addition to the ones used by the renderer. Only render
     * states which are modified in the additional render state will be applied.
     *
     * @return The additional render state.
     */
    public RenderState getAdditionalRenderState() {
        if (additionalState == null) {
            additionalState = RenderState.ADDITIONAL.clone();
        }
        return additionalState;
    }

    /**
     * Get the material definition (j3md file info) that <code>this</code>
     * material is implementing.
     *
     * @return the material definition this material implements.
     */
    public MaterialDef getMaterialDef() {
        return def;
    }

    /**
     * Returns the parameter set on this material with the given name,
     * returns <code>null</code> if the parameter is not set.
     *
     * @param name The parameter name to look up.
     * @return The MatParam if set, or null if not set.
     */
    public MatParam getParam(String name) {
        MatParam param = paramValues.get(name);
        return param;
    }

    /**
     * Returns the texture parameter set on this material with the given name,
     * returns <code>null</code> if the parameter is not set.
     *
     * @param name The parameter name to look up.
     * @return The MatParamTexture if set, or null if not set.
     */
    public MatParamTexture getTextureParam(String name) {
        MatParam param = paramValues.get(name);
        if (param instanceof MatParamTexture) {
            return (MatParamTexture) param;
        }
        return null;
    }

    /**
     * Returns a collection of all parameters set on this material.
     *
     * @return a collection of all parameters set on this material.
     *
     * @see #setParam(java.lang.String, com.jme3.shader.VarType, java.lang.Object)
     */
    public Collection<MatParam> getParams() {
        return paramValues.values();
    }

    private String checkSetParam(VarType type, String name) {
        MatParam paramDef = def.getMaterialParam(name);
        String newName = name;

        if (paramDef == null && name.startsWith("m_")) {
            newName = name.substring(2);
            paramDef = def.getMaterialParam(newName);
            if (paramDef == null) {
                throw new IllegalArgumentException("Material parameter is not defined: " + name);
            } else {
                logger.log(Level.WARNING, "Material parameter {0} uses a deprecated naming convention use {1} instead ", new Object[]{name, newName});
            }
        } else if (paramDef == null) {
            throw new IllegalArgumentException("Material parameter is not defined: " + name);
        }

        if (type != null && paramDef.getVarType() != type) {
            logger.log(Level.WARNING, "Material parameter being set: {0} with "
                    + "type {1} doesn''t match definition types {2}", new Object[]{name, type.name(), paramDef.getVarType()} );
        }

        return newName;
    }

    /**
     * Pass a parameter to the material shader.
     *
     * @param name the name of the parameter defined in the material definition (j3md)
     * @param type the type of the parameter {@link VarType}
     * @param value the value of the parameter
     */
    public void setParam(String name, VarType type, Object value) {
        name = checkSetParam(type, name);

        MatParam val = getParam(name);
        if (technique != null) {
            technique.notifySetParam(name, type, value);
        }
        if (val == null) {
            MatParam paramDef = def.getMaterialParam(name);
            paramValues.put(name, new MatParam(type, name, value, paramDef.getFixedFuncBinding()));
        } else {
            val.setValue(value);
        }
    }

    /**
     * Clear a parameter from this material. The parameter must exist
     * @param name the name of the parameter to clear
     */
    public void clearParam(String name) {
        //On removal, we don't check if the param exists in the paramDef, and just go on with the process.
        // name = checkSetParam(null, name);

        MatParam matParam = getParam(name);
        if (matParam != null) {
            paramValues.remove(name);
            if (technique != null) {
                technique.notifyClearParam(name);
            }
            if (matParam instanceof MatParamTexture) {
                int texUnit = ((MatParamTexture) matParam).getUnit();
                nextTexUnit--;
                for (MatParam param : paramValues.values()) {
                    if (param instanceof MatParamTexture) {
                        MatParamTexture texParam = (MatParamTexture) param;
                        if (texParam.getUnit() > texUnit) {
                            texParam.setUnit(texParam.getUnit() - 1);
                        }
                    }
                }
            }
        }
//        else {
//            throw new IllegalArgumentException("The given parameter is not set.");
//        }
    }

    private void clearTextureParam(String name) {
        name = checkSetParam(null, name);

        MatParamTexture val = getTextureParam(name);
        if (val == null) {
            throw new IllegalArgumentException("The given texture for parameter \"" + name + "\" is null.");
        }

        int texUnit = val.getUnit();
        paramValues.remove(name);
        nextTexUnit--;
        for (MatParam param : paramValues.values()) {
            if (param instanceof MatParamTexture) {
                MatParamTexture texParam = (MatParamTexture) param;
                if (texParam.getUnit() > texUnit) {
                    texParam.setUnit(texParam.getUnit() - 1);
                }
            }
        }

        sortingId = -1;
    }

    /**
     * Set a texture parameter.
     *
     * @param name The name of the parameter
     * @param type The variable type {@link VarType}
     * @param value The texture value of the parameter.
     *
     * @throws IllegalArgumentException is value is null
     */
    public void setTextureParam(String name, VarType type, Texture value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        name = checkSetParam(type, name);
        MatParamTexture val = getTextureParam(name);
        if (val == null) {
            paramValues.put(name, new MatParamTexture(type, name, value, nextTexUnit++));
        } else {
            val.setTextureValue(value);
        }

        if (technique != null) {
            technique.notifySetParam(name, type, nextTexUnit - 1);
        }

        // need to recompute sort ID
        sortingId = -1;
    }

    /**
     * Pass a texture to the material shader.
     *
     * @param name the name of the texture defined in the material definition
     * (j3md) (for example Texture for Lighting.j3md)
     * @param value the Texture object previously loaded by the asset manager
     */
    public void setTexture(String name, Texture value) {
        if (value == null) {
            // clear it
            clearTextureParam(name);
            return;
        }

        VarType paramType = null;
        switch (value.getType()) {
            case TwoDimensional:
                paramType = VarType.Texture2D;
                break;
            case TwoDimensionalArray:
                paramType = VarType.TextureArray;
                break;
            case ThreeDimensional:
                paramType = VarType.Texture3D;
                break;
            case CubeMap:
                paramType = VarType.TextureCubeMap;
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + value.getType());
        }

        setTextureParam(name, paramType, value);
    }

    /**
     * Pass a Matrix4f to the material shader.
     *
     * @param name the name of the matrix defined in the material definition (j3md)
     * @param value the Matrix4f object
     */
    public void setMatrix4(String name, Matrix4f value) {
        setParam(name, VarType.Matrix4, value);
    }

    /**
     * Pass a boolean to the material shader.
     *
     * @param name the name of the boolean defined in the material definition (j3md)
     * @param value the boolean value
     */
    public void setBoolean(String name, boolean value) {
        setParam(name, VarType.Boolean, value);
    }

    /**
     * Pass a float to the material shader.
     *
     * @param name the name of the float defined in the material definition (j3md)
     * @param value the float value
     */
    public void setFloat(String name, float value) {
        setParam(name, VarType.Float, value);
    }

    /**
     * Pass an int to the material shader.
     *
     * @param name the name of the int defined in the material definition (j3md)
     * @param value the int value
     */
    public void setInt(String name, int value) {
        setParam(name, VarType.Int, value);
    }

    /**
     * Pass a Color to the material shader.
     *
     * @param name the name of the color defined in the material definition (j3md)
     * @param value the ColorRGBA value
     */
    public void setColor(String name, ColorRGBA value) {
        setParam(name, VarType.Vector4, value);
    }

    /**
     * Pass a Vector2f to the material shader.
     *
     * @param name the name of the Vector2f defined in the material definition (j3md)
     * @param value the Vector2f value
     */
    public void setVector2(String name, Vector2f value) {
        setParam(name, VarType.Vector2, value);
    }

    /**
     * Pass a Vector3f to the material shader.
     *
     * @param name the name of the Vector3f defined in the material definition (j3md)
     * @param value the Vector3f value
     */
    public void setVector3(String name, Vector3f value) {
        setParam(name, VarType.Vector3, value);
    }

    /**
     * Pass a Vector4f to the material shader.
     *
     * @param name the name of the Vector4f defined in the material definition (j3md)
     * @param value the Vector4f value
     */
    public void setVector4(String name, Vector4f value) {
        setParam(name, VarType.Vector4, value);
    }

    private ColorRGBA getAmbientColor(LightList lightList) {
        ambientLightColor.set(0, 0, 0, 1);
        for (int j = 0; j < lightList.size(); j++) {
            Light l = lightList.get(j);
            if (l instanceof AmbientLight) {
                ambientLightColor.addLocal(l.getColor());
            }
        }
        ambientLightColor.a = 1.0f;
        return ambientLightColor;
    }

    /**
     * Uploads the lights in the light list as two uniform arrays.<br/><br/>
     *      * <p>
     * <code>uniform vec4 g_LightColor[numLights];</code><br/>
     * // g_LightColor.rgb is the diffuse/specular color of the light.<br/>
     * // g_Lightcolor.a is the type of light, 0 = Directional, 1 = Point, <br/>
     * // 2 = Spot. <br/>
     * <br/>
     * <code>uniform vec4 g_LightPosition[numLights];</code><br/>
     * // g_LightPosition.xyz is the position of the light (for point lights)<br/>
     * // or the direction of the light (for directional lights).<br/>
     * // g_LightPosition.w is the inverse radius (1/r) of the light (for attenuation) <br/>
     * </p>
     */
    protected void updateLightListUniforms(Shader shader, Geometry g, int numLights) {
        if (numLights == 0) { // this shader does not do lighting, ignore.
            return;
        }

        LightList lightList = g.getWorldLightList();
        Uniform lightColor = shader.getUniform("g_LightColor");
        Uniform lightPos = shader.getUniform("g_LightPosition");
        Uniform lightDir = shader.getUniform("g_LightDirection");
        lightColor.setVector4Length(numLights);
        lightPos.setVector4Length(numLights);
        lightDir.setVector4Length(numLights);

        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");
        ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList));

        int lightIndex = 0;

        for (int i = 0; i < numLights; i++) {
            if (lightList.size() <= i) {
                lightColor.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);
                lightPos.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);
            } else {
                Light l = lightList.get(i);
                ColorRGBA color = l.getColor();
                lightColor.setVector4InArray(color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        l.getType().getId(),
                        i);

                switch (l.getType()) {
                    case Directional:
                        DirectionalLight dl = (DirectionalLight) l;
                        Vector3f dir = dl.getDirection();
                        lightPos.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, lightIndex);
                        break;
                    case Point:
                        PointLight pl = (PointLight) l;
                        Vector3f pos = pl.getPosition();
                        float invRadius = pl.getInvRadius();
                        lightPos.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, lightIndex);
                        break;
                    case Spot:
                        SpotLight sl = (SpotLight) l;
                        Vector3f pos2 = sl.getPosition();
                        Vector3f dir2 = sl.getDirection();
                        float invRange = sl.getInvSpotRange();
                        float spotAngleCos = sl.getPackedAngleCos();

                        lightPos.setVector4InArray(pos2.getX(), pos2.getY(), pos2.getZ(), invRange, lightIndex);
                        lightDir.setVector4InArray(dir2.getX(), dir2.getY(), dir2.getZ(), spotAngleCos, lightIndex);
                        break;
                    case Ambient:
                        // skip this light. Does not increase lightIndex
                        continue;
                    default:
                        throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
                }
            }

            lightIndex++;
        }

        while (lightIndex < numLights) {
            lightColor.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);
            lightPos.setVector4InArray(0f, 0f, 0f, 0f, lightIndex);

            lightIndex++;
        }
    }

    protected void renderMultipassLighting(Shader shader, Geometry g, RenderManager rm) {

        Renderer r = rm.getRenderer();
        LightList lightList = g.getWorldLightList();
        Uniform lightDir = shader.getUniform("g_LightDirection");
        Uniform lightColor = shader.getUniform("g_LightColor");
        Uniform lightPos = shader.getUniform("g_LightPosition");
        Uniform ambientColor = shader.getUniform("g_AmbientLightColor");
        boolean isFirstLight = true;
        boolean isSecondLight = false;

        for (int i = 0; i < lightList.size(); i++) {
            Light l = lightList.get(i);
            if (l instanceof AmbientLight) {
                continue;
            }

            if (isFirstLight) {
                // set ambient color for first light only
                ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList));
                isFirstLight = false;
                isSecondLight = true;
            } else if (isSecondLight) {
                ambientColor.setValue(VarType.Vector4, ColorRGBA.Black);
                // apply additive blending for 2nd and future lights
                r.applyRenderState(additiveLight);
                isSecondLight = false;
            }

            TempVars vars = TempVars.get();
            Quaternion tmpLightDirection = vars.quat1;
            Quaternion tmpLightPosition = vars.quat2;
            ColorRGBA tmpLightColor = vars.color;
            Vector4f tmpVec = vars.vect4f;

            ColorRGBA color = l.getColor();
            tmpLightColor.set(color);
            tmpLightColor.a = l.getType().getId();
            lightColor.setValue(VarType.Vector4, tmpLightColor);

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();

                    tmpLightPosition.set(dir.getX(), dir.getY(), dir.getZ(), -1);
                    lightPos.setValue(VarType.Vector4, tmpLightPosition);
                    tmpLightDirection.set(0, 0, 0, 0);
                    lightDir.setValue(VarType.Vector4, tmpLightDirection);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getInvRadius();

                    tmpLightPosition.set(pos.getX(), pos.getY(), pos.getZ(), invRadius);
                    lightPos.setValue(VarType.Vector4, tmpLightPosition);
                    tmpLightDirection.set(0, 0, 0, 0);
                    lightDir.setValue(VarType.Vector4, tmpLightDirection);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight) l;
                    Vector3f pos2 = sl.getPosition();
                    Vector3f dir2 = sl.getDirection();
                    float invRange = sl.getInvSpotRange();
                    float spotAngleCos = sl.getPackedAngleCos();

                    tmpLightPosition.set(pos2.getX(), pos2.getY(), pos2.getZ(), invRange);
                    lightPos.setValue(VarType.Vector4, tmpLightPosition);

                    //We transform the spot directoin in view space here to save 5 varying later in the lighting shader
                    //one vec4 less and a vec4 that becomes a vec3
                    //the downside is that spotAngleCos decoding happen now in the frag shader.
                    tmpVec.set(dir2.getX(), dir2.getY(), dir2.getZ(),0);
                    rm.getCurrentCamera().getViewMatrix().mult(tmpVec, tmpVec);
                    tmpLightDirection.set(tmpVec.getX(), tmpVec.getY(), tmpVec.getZ(), spotAngleCos);

                    lightDir.setValue(VarType.Vector4, tmpLightDirection);

                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }
            vars.release();
            r.setShader(shader);
            r.renderMesh(g.getMesh(), g.getLodLevel(), 1);
        }

        if (isFirstLight && lightList.size() > 0) {
            // There are only ambient lights in the scene. Render
            // a dummy "normal light" so we can see the ambient
            ambientColor.setValue(VarType.Vector4, getAmbientColor(lightList));
            lightColor.setValue(VarType.Vector4, ColorRGBA.BlackNoAlpha);
            lightPos.setValue(VarType.Vector4, nullDirLight);
            r.setShader(shader);
            r.renderMesh(g.getMesh(), g.getLodLevel(), 1);
        }
    }

    /**
     * Select the technique to use for rendering this material.
     * <p>
     * If <code>name</code> is "Default", then one of the
     * {@link MaterialDef#getDefaultTechniques() default techniques}
     * on the material will be selected. Otherwise, the named technique
     * will be found in the material definition.
     * <p>
     * Any candidate technique for selection (either default or named)
     * must be verified to be compatible with the system, for that, the
     * <code>renderManager</code> is queried for capabilities.
     *
     * @param name The name of the technique to select, pass "Default" to
     * select one of the default techniques.
     * @param renderManager The {@link RenderManager render manager}
     * to query for capabilities.
     *
     * @throws IllegalArgumentException If "Default" is passed and no default
     * techniques are available on the material definition, or if a name
     * is passed but there's no technique by that name.
     * @throws UnsupportedOperationException If no candidate technique supports
     * the system capabilities.
     */
    public void selectTechnique(String name, RenderManager renderManager) {
        // check if already created
        Technique tech = techniques.get(name);
        if (tech == null) {
            // When choosing technique, we choose one that
            // supports all the caps.
            EnumSet<Caps> rendererCaps = renderManager.getRenderer().getCaps();

            if (name.equals("Default")) {
                List<TechniqueDef> techDefs = def.getDefaultTechniques();
                if (techDefs == null || techDefs.isEmpty()) {
                    throw new IllegalArgumentException("No default techniques are available on material '" + def.getName() + "'");
                }

                TechniqueDef lastTech = null;
                for (TechniqueDef techDef : techDefs) {
                    if (rendererCaps.containsAll(techDef.getRequiredCaps())) {
                        // use the first one that supports all the caps
                        tech = new Technique(this, techDef);
                        techniques.put(name, tech);
                        break;
                    }
                    lastTech = techDef;
                }
                if (tech == null) {
                    throw new UnsupportedOperationException("No default technique on material '" + def.getName() + "'\n"
                            + " is supported by the video hardware. The caps "
                            + lastTech.getRequiredCaps() + " are required.");
                }

            } else {
                // create "special" technique instance
                TechniqueDef techDef = def.getTechniqueDef(name);
                if (techDef == null) {
                    throw new IllegalArgumentException("For material " + def.getName() + ", technique not found: " + name);
                }

                if (!rendererCaps.containsAll(techDef.getRequiredCaps())) {
                    throw new UnsupportedOperationException("The explicitly chosen technique '" + name + "' on material '" + def.getName() + "'\n"
                            + "requires caps " + techDef.getRequiredCaps() + " which are not "
                            + "supported by the video renderer");
                }

                tech = new Technique(this, techDef);
                techniques.put(name, tech);
            }
        } else if (technique == tech) {
            // attempting to switch to an already
            // active technique.
            return;
        }

        technique = tech;
        tech.makeCurrent(def.getAssetManager());

        // shader was changed
        sortingId = -1;
    }

    private void autoSelectTechnique(RenderManager rm) {
        if (technique == null) {
            // NOTE: Not really needed anymore since we have technique
            // selection by caps. Rename all "FixedFunc" techniques to "Default"
            // and remove this hack.
            if (!rm.getRenderer().getCaps().contains(Caps.GLSL100)) {
                selectTechnique("FixedFunc", rm);
            } else {
                selectTechnique("Default", rm);
            }
        } else if (technique.isNeedReload()) {
            technique.makeCurrent(def.getAssetManager());
        }
    }

    /**
     * Preloads this material for the given render manager.
     * <p>
     * Preloading the material can ensure that when the material is first
     * used for rendering, there won't be any delay since the material has
     * been already been setup for rendering.
     *
     * @param rm The render manager to preload for
     */
    public void preload(RenderManager rm) {
        autoSelectTechnique(rm);

        Renderer r = rm.getRenderer();
        TechniqueDef techDef = technique.getDef();

        Collection<MatParam> params = paramValues.values();
        for (MatParam param : params) {
            if (param instanceof MatParamTexture) {
                MatParamTexture texParam = (MatParamTexture) param;
                r.setTexture(0, texParam.getTextureValue());
            } else {
                if (!techDef.isUsingShaders()) {
                    continue;
                }

                technique.updateUniformParam(param.getName(),
                        param.getVarType(),
                        param.getValue(), true);
            }
        }

        Shader shader = technique.getShader();
        if (techDef.isUsingShaders()) {
            r.setShader(shader);
        }
    }

    private void clearUniformsSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            u.clearSetByCurrentMaterial();
        }
    }

    private void resetUniformsNotSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            if (!u.isSetByCurrentMaterial()) {
                u.clearValue();
            }
        }
    }

    /**
     * Called by {@link RenderManager} to render the geometry by
     * using this material.
     *
     * @param geom The geometry to render
     * @param rm The render manager requesting the rendering
     */
    public void render(Geometry geom, RenderManager rm) {
        autoSelectTechnique(rm);

        Renderer r = rm.getRenderer();

        TechniqueDef techDef = technique.getDef();

        if (techDef.getLightMode() == LightMode.MultiPass
                && geom.getWorldLightList().size() == 0) {
            return;
        }

        if (rm.getForcedRenderState() != null) {
            r.applyRenderState(rm.getForcedRenderState());
        } else {
            if (techDef.getRenderState() != null) {
                r.applyRenderState(techDef.getRenderState().copyMergedTo(additionalState, mergedRenderState));
            } else {
                r.applyRenderState(RenderState.DEFAULT.copyMergedTo(additionalState, mergedRenderState));
            }
        }


        // update camera and world matrices
        // NOTE: setWorldTransform should have been called already
        if (techDef.isUsingShaders()) {
            // reset unchanged uniform flag
            clearUniformsSetByCurrent(technique.getShader());
            rm.updateUniformBindings(technique.getWorldBindUniforms());
        }

        // setup textures and uniforms
        for (int i = 0; i < paramValues.size(); i++) {
            MatParam param = paramValues.getValue(i);
            param.apply(r, technique);
        }

        Shader shader = technique.getShader();

        // send lighting information, if needed
        switch (techDef.getLightMode()) {
            case Disable:
                r.setLighting(null);
                break;
            case SinglePass:
                updateLightListUniforms(shader, geom, 4);
                break;
            case FixedPipeline:
                r.setLighting(geom.getWorldLightList());
                break;
            case MultiPass:
                // NOTE: Special case!
                resetUniformsNotSetByCurrent(shader);
                renderMultipassLighting(shader, geom, rm);
                // very important, notice the return statement!
                return;
        }

        // upload and bind shader
        if (techDef.isUsingShaders()) {
            // any unset uniforms will be set to 0
            resetUniformsNotSetByCurrent(shader);
            r.setShader(shader);
        }

        r.renderMesh(geom.getMesh(), geom.getLodLevel(), 1);
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(def.getAssetName(), "material_def", null);
        oc.write(additionalState, "render_state", null);
        oc.write(transparent, "is_transparent", false);
        oc.writeStringSavableMap(paramValues, "parameters", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        additionalState = (RenderState) ic.readSavable("render_state", null);
        transparent = ic.readBoolean("is_transparent", false);

        // Load the material def
        String defName = ic.readString("material_def", null);
        HashMap<String, MatParam> params = (HashMap<String, MatParam>) ic.readStringSavableMap("parameters", null);

        boolean enableVcolor = false;
        boolean separateTexCoord = false;
        boolean applyDefaultValues = false;
        boolean guessRenderStateApply = false;

        int ver = ic.getSavableVersion(Material.class);
        if (ver < 1){
            applyDefaultValues = true;
        }
        if (ver < 2){
            guessRenderStateApply = true;
        }
        if (im.getFormatVersion() == 0) {
            // Enable compatibility with old models
            if (defName.equalsIgnoreCase("Common/MatDefs/Misc/VertexColor.j3md")) {
                // Using VertexColor, switch to Unshaded and set VertexColor=true
                enableVcolor = true;
                defName = "Common/MatDefs/Misc/Unshaded.j3md";
            } else if (defName.equalsIgnoreCase("Common/MatDefs/Misc/SimpleTextured.j3md")
                    || defName.equalsIgnoreCase("Common/MatDefs/Misc/SolidColor.j3md")) {
                // Using SimpleTextured/SolidColor, just switch to Unshaded
                defName = "Common/MatDefs/Misc/Unshaded.j3md";
            } else if (defName.equalsIgnoreCase("Common/MatDefs/Misc/WireColor.j3md")) {
                // Using WireColor, set wireframe renderstate = true and use Unshaded
                getAdditionalRenderState().setWireframe(true);
                defName = "Common/MatDefs/Misc/Unshaded.j3md";
            } else if (defName.equalsIgnoreCase("Common/MatDefs/Misc/Unshaded.j3md")) {
                // Uses unshaded, ensure that the proper param is set
                MatParam value = params.get("SeperateTexCoord");
                if (value != null && ((Boolean) value.getValue()) == true) {
                    params.remove("SeperateTexCoord");
                    separateTexCoord = true;
                }
            }
            assert applyDefaultValues && guessRenderStateApply;
        }

        def = (MaterialDef) im.getAssetManager().loadAsset(new AssetKey(defName));
        paramValues = new ListMap<String, MatParam>();

        // load the textures and update nextTexUnit
        for (Map.Entry<String, MatParam> entry : params.entrySet()) {
            MatParam param = entry.getValue();
            if (param instanceof MatParamTexture) {
                MatParamTexture texVal = (MatParamTexture) param;

                if (nextTexUnit < texVal.getUnit() + 1) {
                    nextTexUnit = texVal.getUnit() + 1;
                }

                // the texture failed to load for this param
                // do not add to param values
                if (texVal.getTextureValue() == null || texVal.getTextureValue().getImage() == null) {
                    continue;
                }
            }
            param.setName(checkSetParam(param.getVarType(), param.getName()));
            paramValues.put(param.getName(), param);
        }
        
        if (applyDefaultValues){
            // compatability with old versions where default vars were
            // not available
            for (MatParam param : def.getMaterialParams()){
                if (param.getValue() != null && paramValues.get(param.getName()) == null){
                    setParam(param.getName(), param.getVarType(), param.getValue());
                }
            }
        }
        if (guessRenderStateApply && additionalState != null){
            // Try to guess values of "apply" render state based on defaults
            // if value != default then set apply to true
            additionalState.applyPolyOffset = additionalState.offsetEnabled;
            additionalState.applyAlphaFallOff = additionalState.alphaTest;
            additionalState.applyAlphaTest = additionalState.alphaTest;
            additionalState.applyBlendMode = additionalState.blendMode != BlendMode.Off;
            additionalState.applyColorWrite = !additionalState.colorWrite; 
            additionalState.applyCullMode = additionalState.cullMode != FaceCullMode.Back;
            additionalState.applyDepthTest = !additionalState.depthTest;
            additionalState.applyDepthWrite = !additionalState.depthWrite;
            additionalState.applyPointSprite = additionalState.pointSprite;
            additionalState.applyStencilTest = additionalState.stencilTest;
            additionalState.applyWireFrame = additionalState.wireframe;
        }
        if (enableVcolor) {
            setBoolean("VertexColor", true);
        }
        if (separateTexCoord) {
            setBoolean("SeparateTexCoord", true);
        }
    }
}
