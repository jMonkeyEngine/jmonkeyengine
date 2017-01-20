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
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.export.*;
import com.jme3.material.logic.TechniqueDefLogic;
import com.jme3.renderer.Caps;
import com.jme3.shader.*;
import com.jme3.shader.Shader.ShaderType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Describes a technique definition.
 *
 * @author Kirill Vainer
 */
public class TechniqueDef implements Savable, Cloneable {

    /**
     * Version #1: Separate shader language for each shader source.
     */
    public static final int SAVABLE_VERSION = 1;

    /**
     * The default technique name.
     *
     * The technique with this name is selected if no specific technique is
     * requested by the user. Currently set to "Default".
     */
    public static final String DEFAULT_TECHNIQUE_NAME = "Default";

    /**
     * Describes light rendering mode.
     */
    public enum LightMode {
        /**
         * Disable light-based rendering
         */
        Disable,

        /**
         * Enable light rendering by using a single pass.
         * <p>
         * An array of light positions and light colors is passed to the shader
         * containing the world light list for the geometry being rendered.
         */
        SinglePass,

        /**
         * Enable light rendering by using multi-pass rendering.
         * <p>
         * The geometry will be rendered once for each light. Each time the
         * light position and light color uniforms are updated to contain
         * the values for the current light. The ambient light color uniform
         * is only set to the ambient light color on the first pass, future
         * passes have it set to black.
         */
        MultiPass,

        /**
         * Enable light rendering by using a single pass, and also uses Image based lighting for global lighting
         * Usually used for PBR
         * <p>
         * An array of light positions and light colors is passed to the shader
         * containing the world light list for the geometry being rendered.
         * Also Light probes are passed to the shader.
         */
        SinglePassAndImageBased,

        /**
         * @deprecated OpenGL1 is not supported anymore
         */
        @Deprecated
        FixedPipeline,
        /**
         * Similar to {@link #SinglePass} except the type of each light is known
         * at shader compile time.
         * <p>
         * The advantage is that the shader can be much more efficient, i.e. not
         * do operations required for spot and point lights if it knows the
         * light is a directional light. The disadvantage is that the number of
         * shaders used balloons because of the variations in the number of
         * lights used by objects.
         */
        StaticPass
    }

    public enum ShadowMode {
        Disable,
        InPass,
        PostPass,
    }
    
    /**
     * Define in what space the light data should be sent to the shader.
     */
    public enum LightSpace {
        World,
        View,
        Legacy
    }

    private final EnumSet<Caps> requiredCaps = EnumSet.noneOf(Caps.class);
    private String name;
    private int sortId;
    
    private EnumMap<Shader.ShaderType,String> shaderLanguages;
    private EnumMap<Shader.ShaderType,String> shaderNames;

    private String shaderPrologue;
    private ArrayList<String> defineNames;
    private ArrayList<VarType> defineTypes;
    private HashMap<String, Integer> paramToDefineId;
    private final HashMap<DefineList, Shader> definesToShaderMap;
    
    private boolean usesNodes = false;
    private List<ShaderNode> shaderNodes;
    private ShaderGenerationInfo shaderGenerationInfo;

    private boolean noRender = false;
    private RenderState renderState;
    private RenderState forcedRenderState;

    private LightMode lightMode = LightMode.Disable;
    private ShadowMode shadowMode = ShadowMode.Disable;
    private TechniqueDefLogic logic;

    private ArrayList<UniformBinding> worldBinds;
    //The space in which the light should be transposed before sending to the shader.
    private LightSpace lightSpace;

    //used to find the best fit technique
    private float weight = 0;

    /**
     * Creates a new technique definition.
     * <p>
     * Used internally by the J3M/J3MD loader.
     *
     * @param name The name of the technique
     */
    public TechniqueDef(String name, int sortId){
        this();
        this.sortId = sortId;
        this.name = name;
    }

    /**
     * Serialization only. Do not use.
     */
    public TechniqueDef() {
        shaderLanguages = new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
        shaderNames = new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
        defineNames = new ArrayList<String>();
        defineTypes = new ArrayList<VarType>();
        paramToDefineId = new HashMap<String, Integer>();
        definesToShaderMap = new HashMap<DefineList, Shader>();
    }
    
    /**
     * @return A unique sort ID. 
     * No other technique definition can have the same ID.
     */
    public int getSortId() {
        return sortId;
    }

    /**
     * Returns the name of this technique as specified in the J3MD file.
     * Default
     * techniques have the name {@link #DEFAULT_TECHNIQUE_NAME}.
     *
     * @return the name of this technique
     */
    public String getName(){
        return name;
    }

    /**
     * Returns the light mode.
     * @return the light mode.
     * @see LightMode
     */
    public LightMode getLightMode() {
        return lightMode;
    }

    /**
     * Set the light mode
     *
     * @param lightMode the light mode
     *
     * @see LightMode
     */
    public void setLightMode(LightMode lightMode) {
        this.lightMode = lightMode;
        //if light space is not specified we set it to Legacy
        if(lightSpace == null){
            if(lightMode== LightMode.MultiPass){
                lightSpace = LightSpace.Legacy;
            }else{
                lightSpace = LightSpace.World;
            }
        }
    }
    
    public void setLogic(TechniqueDefLogic logic) {
        this.logic = logic;
    }

    public TechniqueDefLogic getLogic() {
        return logic;
    }
    
    /**
     * Returns the shadow mode.
     * @return the shadow mode.
     */
    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    /**
     * Set the shadow mode.
     *
     * @param shadowMode the shadow mode.
     *
     * @see ShadowMode
     */
    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }

    /**
     * Returns the render state that this technique is using
     * @return the render state that this technique is using
     * @see #setRenderState(com.jme3.material.RenderState)
     */
    public RenderState getRenderState() {
        return renderState;
    }

    /**
     * Sets the render state that this technique is using.
     *
     * @param renderState the render state that this technique is using.
     *
     * @see RenderState
     */
    public void setRenderState(RenderState renderState) {
        this.renderState = renderState;
    }

    /**
     * Sets if this technique should not be used to render.
     *
     * @param noRender not render or render ?
     *
     * @see NoRender
     */
    public void setNoRender(boolean noRender) {
        this.noRender = noRender;
    }

    /**
     * Returns true if this technique should not be used to render.
     * (eg. to not render a material with default technique)
     *
     * @return true if this technique should not be rendered, false otherwise.
     *
     */
    public boolean isNoRender(){
        return noRender;
    }

    /**
     * Returns true if this technique uses Shader Nodes, false otherwise.
     *
     * @return true if this technique uses Shader Nodes, false otherwise.
     *
     */
    public boolean isUsingShaderNodes(){
        return usesNodes;
    }

    /**
     * Gets the {@link Caps renderer capabilities} that are required
     * by this technique.
     *
     * @return the required renderer capabilities
     */
    public EnumSet<Caps> getRequiredCaps() {
        return requiredCaps;
    }

    /**
     * Sets the shaders that this technique definition will use.
     *
     * @param vertexShader The name of the vertex shader
     * @param fragmentShader The name of the fragment shader
     * @param vertLanguage The vertex shader language
     * @param fragLanguage The fragment shader language
     */
    public void setShaderFile(String vertexShader, String fragmentShader, String vertLanguage, String fragLanguage) {
        this.shaderLanguages.put(Shader.ShaderType.Vertex, vertLanguage);
        this.shaderNames.put(Shader.ShaderType.Vertex, vertexShader);
        this.shaderLanguages.put(Shader.ShaderType.Fragment, fragLanguage);
        this.shaderNames.put(Shader.ShaderType.Fragment, fragmentShader);

        requiredCaps.clear();
        Caps vertCap = Caps.valueOf(vertLanguage);
        requiredCaps.add(vertCap);
        Caps fragCap = Caps.valueOf(fragLanguage);
        requiredCaps.add(fragCap);

        weight = Math.max(vertCap.ordinal(), fragCap.ordinal());
    }

    /**
     * Set a string which is prepended to every shader used by this technique.
     * 
     * Typically this is used for preset defines.
     * 
     * @param shaderPrologue The prologue to append before the technique's shaders.
     */
    public void setShaderPrologue(String shaderPrologue) {
        this.shaderPrologue = shaderPrologue;
    }
    
    /**
     * @return the shader prologue which is prepended to every shader.
     */
    public String getShaderPrologue() {
        return shaderPrologue;
    }
    
    /**
     * Returns the define name which the given material parameter influences.
     *
     * @param paramName The parameter name to look up
     * @return The define name
     *
     * @see #addShaderParamDefine(java.lang.String, java.lang.String)
     */
    public String getShaderParamDefine(String paramName){
        Integer defineId = paramToDefineId.get(paramName);
        if (defineId != null) {
            return defineNames.get(defineId);
        } else {
            return null;
        }
    }
    
    /**
     * Get the define ID for a given material parameter.
     *
     * @param paramName The parameter name to look up
     * @return The define ID, or null if not found.
     */
    public Integer getShaderParamDefineId(String paramName) {
        return paramToDefineId.get(paramName);
    }

    /**
     * Get the type of a particular define.
     *
     * @param defineId The define ID to lookup.
     * @return The type of the define, or null if not found.
     */
    public VarType getDefineIdType(int defineId) {
        return defineId < defineTypes.size() ? defineTypes.get(defineId) : null;
    }
    
    /**
     * Adds a define linked to a material parameter.
     * <p>
     * Any time the material parameter on the parent material is altered,
     * the appropriate define on the technique will be modified as well.
     * When set, the material parameter will be mapped to an integer define, 
     * typically <code>1</code> if it is set, unless it is an integer or a float,
     * in which case it will converted into an integer.
     *
     * @param paramName The name of the material parameter to link to.
     * @param paramType The type of the material parameter to link to.
     * @param defineName The name of the define parameter, e.g. USE_LIGHTING
     */
    public void addShaderParamDefine(String paramName, VarType paramType, String defineName){
        int defineId = defineNames.size();
        
        if (defineId >= DefineList.MAX_DEFINES) {
            throw new IllegalStateException("Cannot have more than " + 
                    DefineList.MAX_DEFINES + " defines on a technique.");
        }
        
        paramToDefineId.put(paramName, defineId);
        defineNames.add(defineName);
        defineTypes.add(paramType);
    }

    /**
     * Add an unmapped define which can only be set by define ID.
     * 
     * Unmapped defines are used by technique renderers to 
     * configure the shader internally before rendering.
     * 
     * @param defineName The define name to create
     * @return The define ID of the created define
     */
    public int addShaderUnmappedDefine(String defineName, VarType defineType) {
        int defineId = defineNames.size();
        
        if (defineId >= DefineList.MAX_DEFINES) {
            throw new IllegalStateException("Cannot have more than " + 
                    DefineList.MAX_DEFINES + " defines on a technique.");
        }
        
        defineNames.add(defineName);
        defineTypes.add(defineType);
        return defineId;
    }

    /**
     * Get the names of all defines declared on this technique definition.
     *
     * The defines are returned in order of declaration.
     *
     * @return the names of all defines declared.
     */
    public String[] getDefineNames() {
        return defineNames.toArray(new String[0]);
    }

    /**
     * Get the types of all defines declared on this technique definition.
     *
     * The types are returned in order of declaration.
     *
     * @return the types of all defines declared.
     */
    public VarType[] getDefineTypes() {
        return defineTypes.toArray(new VarType[0]);
    }
    
    /**
     * Create a define list with the size matching the number
     * of defines on this technique.
     * 
     * @return a define list with the size matching the number
     * of defines on this technique.
     */
    public DefineList createDefineList() {
        return new DefineList(defineNames.size());
    }
    
    private Shader loadShader(AssetManager assetManager, EnumSet<Caps> rendererCaps, DefineList defines) {
        StringBuilder sb = new StringBuilder();
        sb.append(shaderPrologue);
        defines.generateSource(sb, defineNames, defineTypes);
        String definesSourceCode = sb.toString();

        Shader shader;
        if (isUsingShaderNodes()) {
            ShaderGenerator shaderGenerator = assetManager.getShaderGenerator(rendererCaps);
            if (shaderGenerator == null) {
                throw new UnsupportedOperationException("ShaderGenerator was not initialized, "
                        + "make sure assetManager.getGenerator(caps) has been called");
            }
            shaderGenerator.initialize(this);
            shader = shaderGenerator.generateShader(definesSourceCode);
        } else {
            shader = new Shader();
            for (ShaderType type : ShaderType.values()) {
                String language = shaderLanguages.get(type);
                String shaderSourceAssetName = shaderNames.get(type);
                if (language == null || shaderSourceAssetName == null) {
                    continue;
                }
                String shaderSourceCode = (String) assetManager.loadAsset(shaderSourceAssetName);
                shader.addSource(type, shaderSourceAssetName, shaderSourceCode, definesSourceCode, language);
            }
        }

        if (getWorldBindings() != null) {
           for (UniformBinding binding : getWorldBindings()) {
               shader.addUniformBinding(binding);
           }
        }
        
        return shader;
    }
    
    public Shader getShader(AssetManager assetManager, EnumSet<Caps> rendererCaps, DefineList defines) {
          Shader shader = definesToShaderMap.get(defines);
          if (shader == null) {
              shader = loadShader(assetManager, rendererCaps, defines);
              definesToShaderMap.put(defines.deepClone(), shader);
          }
          return shader;
     }
    
    /**
     * Sets the shaders that this technique definition will use.
     *
     * @param shaderNames EnumMap containing all shader names for this stage
     * @param shaderLanguages EnumMap containing all shader languages for this stage
     */
    public void setShaderFile(EnumMap<Shader.ShaderType, String> shaderNames, EnumMap<Shader.ShaderType, String> shaderLanguages) {
        requiredCaps.clear();

        weight = 0;
        for (Shader.ShaderType shaderType : shaderNames.keySet()) {
            String language = shaderLanguages.get(shaderType);
            String shaderFile = shaderNames.get(shaderType);

            this.shaderLanguages.put(shaderType, language);
            this.shaderNames.put(shaderType, shaderFile);

            Caps cap = Caps.valueOf(language);
            requiredCaps.add(cap);
            weight = Math.max(weight, cap.ordinal());

            if (shaderType.equals(Shader.ShaderType.Geometry)) {
                requiredCaps.add(Caps.GeometryShader);
            } else if (shaderType.equals(Shader.ShaderType.TessellationControl)) {
                requiredCaps.add(Caps.TesselationShader);
            }
        }
    }

    /**
     * Returns the name of the fragment shader used by the technique, or null
     * if no fragment shader is specified.
     *
     * @return the name of the fragment shader to be used.
     */
    public String getFragmentShaderName() {
        return shaderNames.get(Shader.ShaderType.Fragment);
    }


    /**
     * Returns the name of the vertex shader used by the technique, or null
     * if no vertex shader is specified.
     *
     * @return the name of the vertex shader to be used.
     */
    public String getVertexShaderName() {
        return shaderNames.get(Shader.ShaderType.Vertex);
    }

    /**
     * Returns the language of the fragment shader used in this technique.
     */
    public String getFragmentShaderLanguage() {
        return shaderLanguages.get(Shader.ShaderType.Fragment);
    }

    /**
     * Returns the language of the vertex shader used in this technique.
     */
    public String getVertexShaderLanguage() {
        return shaderLanguages.get(Shader.ShaderType.Vertex);
    }

    /**Returns the language for each shader program
     * @param shaderType
     */
    public String getShaderProgramLanguage(Shader.ShaderType shaderType){
        return shaderLanguages.get(shaderType);
    }
    /**Returns the name for each shader program
     * @param shaderType
     */
    public String getShaderProgramName(Shader.ShaderType shaderType){
        return shaderNames.get(shaderType);
    }

    /**
     * returns the weight of the technique def
     *
     * @return
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Adds a new world parameter by the given name.
     *
     * @param name The world parameter to add.
     * @return True if the world parameter name was found and added
     * to the list of world parameters, false otherwise.
     */
    public boolean addWorldParam(String name) {
        if (worldBinds == null){
            worldBinds = new ArrayList<UniformBinding>();
        }

        try {
            worldBinds.add( UniformBinding.valueOf(name) );
            return true;
        } catch (IllegalArgumentException ex){
            return false;
        }
    }

    public RenderState getForcedRenderState() {
        return forcedRenderState;
    }

    public void setForcedRenderState(RenderState forcedRenderState) {
        this.forcedRenderState = forcedRenderState;
    }

    /**
     * Returns a list of world parameters that are used by this
     * technique definition.
     *
     * @return The list of world parameters
     */
    public List<UniformBinding> getWorldBindings() {
        return worldBinds;
    }

    public void write(JmeExporter ex) throws IOException{
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", null);

        oc.write(shaderNames.get(Shader.ShaderType.Vertex), "vertName", null);
        oc.write(shaderNames.get(Shader.ShaderType.Fragment), "fragName", null);
        oc.write(shaderNames.get(Shader.ShaderType.Geometry), "geomName", null);
        oc.write(shaderNames.get(Shader.ShaderType.TessellationControl), "tsctrlName", null);
        oc.write(shaderNames.get(Shader.ShaderType.TessellationEvaluation), "tsevalName", null);
        oc.write(shaderLanguages.get(Shader.ShaderType.Vertex), "vertLanguage", null);
        oc.write(shaderLanguages.get(Shader.ShaderType.Fragment), "fragLanguage", null);
        oc.write(shaderLanguages.get(Shader.ShaderType.Geometry), "geomLanguage", null);
        oc.write(shaderLanguages.get(Shader.ShaderType.TessellationControl), "tsctrlLanguage", null);
        oc.write(shaderLanguages.get(Shader.ShaderType.TessellationEvaluation), "tsevalLanguage", null);

        oc.write(shaderPrologue, "shaderPrologue", null);
        oc.write(lightMode, "lightMode", LightMode.Disable);
        oc.write(shadowMode, "shadowMode", ShadowMode.Disable);
        oc.write(renderState, "renderState", null);
        oc.write(noRender, "noRender", false);
        oc.write(usesNodes, "usesNodes", false);
        oc.writeSavableArrayList((ArrayList)shaderNodes,"shaderNodes", null);
        oc.write(shaderGenerationInfo, "shaderGenerationInfo", null);

        // TODO: Finish this when Map<String, String> export is available
//        oc.write(defineParams, "defineParams", null);
        // TODO: Finish this when List<Enum> export is available
//        oc.write(worldBinds, "worldBinds", null);
    }

    public void read(JmeImporter im) throws IOException{
        InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", null);
        shaderNames.put(Shader.ShaderType.Vertex,ic.readString("vertName", null));
        shaderNames.put(Shader.ShaderType.Fragment,ic.readString("fragName", null));
        shaderNames.put(Shader.ShaderType.Geometry,ic.readString("geomName", null));
        shaderNames.put(Shader.ShaderType.TessellationControl,ic.readString("tsctrlName", null));
        shaderNames.put(Shader.ShaderType.TessellationEvaluation,ic.readString("tsevalName", null));
        shaderPrologue = ic.readString("shaderPrologue", null);
        lightMode = ic.readEnum("lightMode", LightMode.class, LightMode.Disable);
        shadowMode = ic.readEnum("shadowMode", ShadowMode.class, ShadowMode.Disable);
        renderState = (RenderState) ic.readSavable("renderState", null);
        noRender = ic.readBoolean("noRender", false);

        if (ic.getSavableVersion(TechniqueDef.class) == 0) {
            // Old version
            shaderLanguages.put(Shader.ShaderType.Vertex,ic.readString("shaderLang", null));
            shaderLanguages.put(Shader.ShaderType.Fragment,shaderLanguages.get(Shader.ShaderType.Vertex));
        } else {
            // New version
            shaderLanguages.put(Shader.ShaderType.Vertex,ic.readString("vertLanguage", null));
            shaderLanguages.put(Shader.ShaderType.Fragment,ic.readString("fragLanguage", null));
            shaderLanguages.put(Shader.ShaderType.Geometry,ic.readString("geomLanguage", null));
            shaderLanguages.put(Shader.ShaderType.TessellationControl,ic.readString("tsctrlLanguage", null));
            shaderLanguages.put(Shader.ShaderType.TessellationEvaluation,ic.readString("tsevalLanguage", null));
        }

        usesNodes = ic.readBoolean("usesNodes", false);
        shaderNodes = ic.readSavableArrayList("shaderNodes", null);
        shaderGenerationInfo = (ShaderGenerationInfo) ic.readSavable("shaderGenerationInfo", null);
    }

    public List<ShaderNode> getShaderNodes() {
        return shaderNodes;
    }

    public void setShaderNodes(List<ShaderNode> shaderNodes) {
        this.shaderNodes = shaderNodes;
        usesNodes = true;
    }

    /**
     * Returns the Enum containing the ShaderProgramNames;
     * @return
     */
    public EnumMap<Shader.ShaderType, String> getShaderProgramNames() {
        return shaderNames;
    }

    /**
     * Returns the Enum containing the ShaderProgramLanguages;
     * @return
     */
    public EnumMap<Shader.ShaderType, String> getShaderProgramLanguages() {
        return shaderLanguages;
    }

    public ShaderGenerationInfo getShaderGenerationInfo() {
        return shaderGenerationInfo;
    }

    public void setShaderGenerationInfo(ShaderGenerationInfo shaderGenerationInfo) {
        this.shaderGenerationInfo = shaderGenerationInfo;
    }

    @Override
    public String toString() {
        return "TechniqueDef[name=" + name
                + ", requiredCaps=" + requiredCaps
                + ", noRender=" + noRender
                + ", lightMode=" + lightMode
                + ", usesNodes=" + usesNodes
                + ", renderState=" + renderState
                + ", forcedRenderState=" + forcedRenderState + "]";
    }

    /**
     * Returns the space in which the light data should be passed to the shader.
     * @return the light space
     */
    public LightSpace getLightSpace() {
        return lightSpace;
    }

    /**
     * Sets the space in which the light data should be passed to the shader.
     * @param lightSpace the light space
     */
    public void setLightSpace(LightSpace lightSpace) {
        this.lightSpace = lightSpace;
    }

    @Override
    public TechniqueDef clone() throws CloneNotSupportedException {
        //cannot use super.clone because of the final fields instance that would be shared by the clones.
        TechniqueDef clone = new TechniqueDef(name, sortId);

        clone.noRender = noRender;
        clone.lightMode = lightMode;
        clone.shadowMode = shadowMode;
        clone.lightSpace = lightSpace;
        clone.usesNodes = usesNodes;
        clone.shaderPrologue = shaderPrologue;

        clone.setShaderFile(shaderNames, shaderLanguages);

        clone.defineNames = new ArrayList<>(defineNames.size());
        clone.defineNames.addAll(defineNames);

        clone.defineTypes = new ArrayList<>(defineTypes.size());
        clone.defineTypes.addAll(defineTypes);

        clone.paramToDefineId = new HashMap<>(paramToDefineId.size());
        clone.paramToDefineId.putAll(paramToDefineId);

        if (shaderNodes != null) {
            for (ShaderNode shaderNode : shaderNodes) {
                clone.shaderNodes.add(shaderNode.clone());
            }
            clone.shaderGenerationInfo = shaderGenerationInfo.clone();
        }

        if (renderState != null) {
            clone.setRenderState(renderState.clone());
        }
        if (forcedRenderState != null) {
            clone.setForcedRenderState(forcedRenderState.clone());
        }

        try {
            clone.logic = logic.getClass().getConstructor(TechniqueDef.class).newInstance(clone);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        if (worldBinds != null) {
            clone.worldBinds = new ArrayList<>(worldBinds.size());
            clone.worldBinds.addAll(worldBinds);
        }

        return clone;
    }
}
