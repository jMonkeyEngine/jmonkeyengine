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
package com.jme3.material;

import com.jme3.asset.AssetKey;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.asset.AssetManager;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.util.ListMap;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Material implements Cloneable, Savable, Comparable<Material> {

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

    public static class MatParamTexture extends MatParam {

        private Texture texture;
        private int unit;
//        private transient TextureKey key;

        public MatParamTexture(VarType type, String name, Texture texture, int unit) {
            super(type, name, texture, null);
            this.texture = texture;
            this.unit = unit;
        }

        public MatParamTexture() {
        }

        public Texture getTextureValue() {
            return texture;
        }

        public void setTextureValue(Texture value) {
            this.value = value;
            this.texture = value;
        }

        public void setUnit(int unit) {
            this.unit = unit;
        }

        public int getUnit() {
            return unit;
        }

        @Override
        public void apply(Renderer r, Technique technique) {
            TechniqueDef techDef = technique.getDef();
            r.setTexture(getUnit(), getTextureValue());
            if (techDef.isUsingShaders()) {
                technique.updateUniformParam(getName(), getVarType(), getUnit(), true);
            }
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            super.write(ex);
            OutputCapsule oc = ex.getCapsule(this);
            oc.write(unit, "texture_unit", -1);
            oc.write(texture, "texture", null);
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            super.read(im);
            InputCapsule ic = im.getCapsule(this);
            unit = ic.readInt("texture_unit", -1);
            texture = (Texture) ic.readSavable("texture", null);
//            key = texture.getTextureKey();
        }
    }

    public Material(MaterialDef def) {
        if (def == null) {
            throw new NullPointerException("Material definition cannot be null");
        }

        this.def = def;
    }

    public Material(AssetManager contentMan, String defName) {
        this((MaterialDef) contentMan.loadAsset(new AssetKey(defName)));
    }

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public Material() {
    }

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

    public int compareTo(Material m) {
        return m.getSortId() - getSortId();
    }

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

    public Technique getActiveTechnique() {
        return technique;
    }

    /**
     * Should only be used in Technique.makeCurrent()
     * @param tech
     */
    public void setActiveTechnique(Technique tech) {
        technique = tech;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    public boolean isReceivesShadows() {
        return receivesShadows;
    }

    public void setReceivesShadows(boolean receivesShadows) {
        this.receivesShadows = receivesShadows;
    }

    public RenderState getAdditionalRenderState() {
        if (additionalState == null) {
            additionalState = RenderState.ADDITIONAL.clone();
        }
        return additionalState;
    }

    public MaterialDef getMaterialDef() {
        return def;
    }

//    void updateUniformLinks(){
//        for (MatParam param : paramValues.values()){
//            param.uniform = technique.getShader().getUniform(param.name);
//        }
//    }
    public MatParam getParam(String name) {
        MatParam param = paramValues.get(name);
        if (param instanceof MatParam) {
            return (MatParam) param;
        }

        return null;
    }

    public MatParamTexture getTextureParam(String name) {
        MatParam param = paramValues.get(name);
        if (param instanceof MatParamTexture) {
            return (MatParamTexture) param;
        }
        return null;
    }

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
        }else if (paramDef == null){
            throw new IllegalArgumentException("Material parameter is not defined: " + name);
        }

        if (type != null && paramDef.getVarType() != type) {
            logger.logp(Level.WARNING, "Material parameter being set: {0} with "
                    + "type {1} doesn't match definition type {2}",
                    name, type.name(), paramDef.getVarType());
        }
        
        return newName;
    }

    /**
     * Pass a parameter to the material shader
     * @param name the name of the parameter defined in the material definition (j3md)
     * @param type the type of the parameter @see com.jme3.shaderVarType
     * @param value the value of the param
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
     * Clear a parameter from this material. The param must exist
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

    /**
     * 
     * @param name 
     * @deprecated use clearParam instead
     */
    @Deprecated
    public void clearTextureParam(String name) {
        name = checkSetParam(null, name);

        MatParamTexture val = getTextureParam(name);
        if (val == null) {
            throw new IllegalArgumentException("The given texture parameter is not set.");
        } else {
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
        }
    }

    public void setTextureParam(String name, VarType type, Texture value) {
        if (value == null) {
            throw new NullPointerException();
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
    }

    /**
     * Pass a texture to the material shader
     * @param name the name of the texture defined in the material definition (j3md) (for example Texture for Lighting.j3md)
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
     * Pass a Matrix4f to the material shader
     * @param name the name of the matrix defined in the material definition (j3md)
     * @param value the Matrix4f object
     */
    public void setMatrix4(String name, Matrix4f value) {
        setParam(name, VarType.Matrix4, value);
    }

    /**
     * Pass a boolean to the material shader
     * @param name the name of the boolean defined in the material definition (j3md)
     * @param value the boolean value
     */
    public void setBoolean(String name, boolean value) {
        setParam(name, VarType.Boolean, value);
    }

    /**
     * Pass a float to the material shader
     * @param name the name of the float defined in the material definition (j3md)
     * @param value the float value
     */
    public void setFloat(String name, float value) {
        setParam(name, VarType.Float, value);
    }

    /**
     * Pass an int to the material shader
     * @param name the name of the int defined in the material definition (j3md)
     * @param value the int value
     */
    public void setInt(String name, int value) {
        setParam(name, VarType.Int, value);
    }

    /**
     * Pass a Color to the material shader
     * @param name the name of the color defined in the material definition (j3md)
     * @param value the ColorRGBA value
     */
    public void setColor(String name, ColorRGBA value) {
        setParam(name, VarType.Vector4, value);
    }

    /**
     * Pass a Vector2f to the material shader
     * @param name the name of the Vector2f defined in the material definition (j3md)
     * @param value the Vector2f value
     */
    public void setVector2(String name, Vector2f value) {
        setParam(name, VarType.Vector2, value);
    }

    /**
     * Pass a Vector3f to the material shader
     * @param name the name of the Vector3f defined in the material definition (j3md)
     * @param value the Vector3f value
     */
    public void setVector3(String name, Vector3f value) {
        setParam(name, VarType.Vector3, value);
    }

    /**
     * Pass a Vector4f to the material shader
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
     *
     * @param shader
     * @param lightList
     */
    protected void updateLightListUniforms(Shader shader, Geometry g, int numLights) {
        if (numLights == 0) // this shader does not do lighting, ignore.
        {
            return;
        }

        LightList lightList = g.getWorldLightList();
        Uniform lightColor = shader.getUniform("g_LightColor");
        Uniform lightPos = shader.getUniform("g_LightPosition");
        lightColor.setVector4Length(numLights);
        lightPos.setVector4Length(numLights);

        for (int i = 0; i < numLights; i++) {
            if (lightList.size() <= i) {
                lightColor.setVector4InArray(0f, 0f, 0f, 0f, i);
                lightPos.setVector4InArray(0f, 0f, 0f, 0f, i);
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
                        lightPos.setVector4InArray(dir.getX(), dir.getY(), dir.getZ(), -1, i);
                        break;
                    case Point:
                        PointLight pl = (PointLight) l;
                        Vector3f pos = pl.getPosition();
                        float invRadius = pl.getRadius();
                        if (invRadius != 0) {
                            invRadius = 1f / invRadius;
                        }
                        lightPos.setVector4InArray(pos.getX(), pos.getY(), pos.getZ(), invRadius, i);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
                }
            }
        }
    }

    protected void renderMultipassLighting(Shader shader, Geometry g, Renderer r) {
        LightList lightList = g.getWorldLightList();
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

            ColorRGBA color = l.getColor();
            ColorRGBA color2;
            if (lightColor.getValue() != null) {
                color2 = (ColorRGBA) lightColor.getValue();
            } else {
                color2 = new ColorRGBA();
            }
            color2.set(color);
            color2.a = l.getType().getId();
            lightColor.setValue(VarType.Vector4, color2);

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    Vector3f dir = dl.getDirection();
                    Quaternion q1;
                    if (lightPos.getValue() != null) {
                        q1 = (Quaternion) lightPos.getValue();
                    } else {
                        q1 = new Quaternion();
                    }
                    q1.set(dir.getX(), dir.getY(), dir.getZ(), -1);
                    lightPos.setValue(VarType.Vector4, q1);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    Vector3f pos = pl.getPosition();
                    float invRadius = pl.getRadius();
                    if (invRadius != 0) {
                        invRadius = 1f / invRadius;
                    }
                    Quaternion q2;
                    if (lightPos.getValue() != null) {
                        q2 = (Quaternion) lightPos.getValue();
                    } else {
                        q2 = new Quaternion();
                    }
                    q2.set(pos.getX(), pos.getY(), pos.getZ(), invRadius);
                    lightPos.setValue(VarType.Vector4, q2);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown type of light: " + l.getType());
            }

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

    public void selectTechnique(String name, RenderManager renderManager) {
        // check if already created
        Technique tech = techniques.get(name);
        if (tech == null) {
            // When choosing technique, we choose one that
            // supports all the caps.
            EnumSet<Caps> rendererCaps = renderManager.getRenderer().getCaps();

            if (name.equals("Default")) {
                List<TechniqueDef> techDefs = def.getDefaultTechniques();
                if (techDefs == null || techDefs.size() == 0) {
                    throw new IllegalStateException("No default techniques are available on material '" + def.getName() + "'");
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
                            + "requires caps " + techDef.getRequiredCaps() + " which are not"
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
     * "Pre-load" the material, including textures and shaders, to the 
     * renderer.
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
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform u = uniforms.getValue(i);
            u.clearSetByCurrentMaterial();
        }
    }

    private void resetUniformsNotSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform u = uniforms.getValue(i);
            if (!u.isSetByCurrentMaterial()) {
                u.clearValue();
            }
        }
    }

    /**
     * Should be called after selectTechnique()
     * @param geom
     * @param r
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
                renderMultipassLighting(shader, geom, r);
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
        String defName = ic.readString("material_def", null);
        def = (MaterialDef) im.getAssetManager().loadAsset(new AssetKey(defName));
        additionalState = (RenderState) ic.readSavable("render_state", null);
        transparent = ic.readBoolean("is_transparent", false);

        HashMap<String, MatParam> params = (HashMap<String, MatParam>) ic.readStringSavableMap("parameters", null);
//        paramValues.putAll(params);
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
                if (texVal.texture == null || texVal.texture.getImage() == null) {
                    continue;
                }
            }
            param.setName(checkSetParam(param.getVarType(), param.getName()));
            paramValues.put(param.getName(), param);
        }
    }
}
