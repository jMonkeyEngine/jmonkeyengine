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

import com.jme3.export.*;
import com.jme3.renderer.Caps;
import com.jme3.shader.*;

import java.io.IOException;
import java.util.*;

/**
 * Describes a technique definition.
 *
 * @author Kirill Vainer
 */
public class TechniqueDef implements Savable {

    /**
     * Version #1: Separate shader language for each shader source.
     */
    public static final int SAVABLE_VERSION = 1;

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
         * @deprecated OpenGL1 is not supported anymore
         */
        @Deprecated
        FixedPipeline,
    }

    public enum ShadowMode {
        Disable,
        InPass,
        PostPass,
    }

    private EnumSet<Caps> requiredCaps = EnumSet.noneOf(Caps.class);
    private String name;

    private EnumMap<Shader.ShaderType,String> shaderLanguages;
    private EnumMap<Shader.ShaderType,String> shaderNames;

    private DefineList presetDefines;
    private boolean usesNodes = false;
    private List<ShaderNode> shaderNodes;
    private ShaderGenerationInfo shaderGenerationInfo;

    private boolean noRender = false;
    private RenderState renderState;
    private RenderState forcedRenderState;

    private LightMode lightMode   = LightMode.Disable;
    private ShadowMode shadowMode = ShadowMode.Disable;

    private HashMap<String, String> defineParams;
    private ArrayList<UniformBinding> worldBinds;

    /**
     * Creates a new technique definition.
     * <p>
     * Used internally by the J3M/J3MD loader.
     *
     * @param name The name of the technique, should be set to <code>null</code>
     * for default techniques.
     */
    public TechniqueDef(String name){
        this();
        this.name = name == null ? "Default" : name;
    }

    /**
     * Serialization only. Do not use.
     */
    public TechniqueDef(){
        shaderLanguages=new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
        shaderNames=new EnumMap<Shader.ShaderType, String>(Shader.ShaderType.class);
    }

    /**
     * Returns the name of this technique as specified in the J3MD file.
     * Default techniques have the name "Default".
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
     * @deprecated jME3 always requires shaders now
     */
    @Deprecated
    public boolean isUsingShaders(){
        return true;
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
    }


    /**
     * Sets the shaders that this technique definition will use.
     *
     * @param shaderNames EnumMap containing all shader names for this stage
     * @param shaderLanguages EnumMap containing all shader languages for this stage
     */
    public void setShaderFile(EnumMap<Shader.ShaderType, String> shaderNames, EnumMap<Shader.ShaderType, String> shaderLanguages) {
        requiredCaps.clear();

        for (Shader.ShaderType shaderType : shaderNames.keySet()) {
            String language = shaderLanguages.get(shaderType);
            String shaderFile = shaderNames.get(shaderType);

            this.shaderLanguages.put(shaderType, language);
            this.shaderNames.put(shaderType, shaderFile);

            Caps vertCap = Caps.valueOf(language);
            requiredCaps.add(vertCap);

            if (shaderType.equals(Shader.ShaderType.Geometry)) {
                requiredCaps.add(Caps.GeometryShader);
            } else if (shaderType.equals(Shader.ShaderType.TessellationControl)) {
                requiredCaps.add(Caps.TesselationShader);
            }
        }
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
        if (defineParams == null) {
            return null;
        }
        return defineParams.get(paramName);
    }

    /**
     * Adds a define linked to a material parameter.
     * <p>
     * Any time the material parameter on the parent material is altered,
     * the appropriate define on the technique will be modified as well.
     * See the method
     * {@link DefineList#set(java.lang.String, com.jme3.shader.VarType, java.lang.Object) }
     * on the exact details of how the material parameter changes the define.
     *
     * @param paramName The name of the material parameter to link to.
     * @param defineName The name of the define parameter, e.g. USE_LIGHTING
     */
    public void addShaderParamDefine(String paramName, String defineName){
        if (defineParams == null) {
            defineParams = new HashMap<String, String>();
        }
        defineParams.put(paramName, defineName);
    }

    /**
     * Returns the {@link DefineList} for the preset defines.
     *
     * @return the {@link DefineList} for the preset defines.
     *
     * @see #addShaderPresetDefine(java.lang.String, com.jme3.shader.VarType, java.lang.Object)
     */
    public DefineList getShaderPresetDefines() {
        return presetDefines;
    }

    /**
     * Adds a preset define.
     * <p>
     * Preset defines do not depend upon any parameters to be activated,
     * they are always passed to the shader as long as this technique is used.
     *
     * @param defineName The name of the define parameter, e.g. USE_LIGHTING
     * @param type The type of the define. See
     * {@link DefineList#set(java.lang.String, com.jme3.shader.VarType, java.lang.Object) }
     * to see why it matters.
     *
     * @param value The value of the define
     */
    public void addShaderPresetDefine(String defineName, VarType type, Object value){
        if (presetDefines == null) {
            presetDefines = new DefineList();
        }
        presetDefines.set(defineName, type, value);
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

        oc.write(presetDefines, "presetDefines", null);
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
        presetDefines = (DefineList) ic.readSavable("presetDefines", null);
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

    //todo: make toString return something usefull
    @Override
    public String toString() {
        return "TechniqueDef{" + "requiredCaps=" + requiredCaps + ", name=" + name /*+ ", vertName=" + vertName + ", fragName=" + fragName + ", vertLanguage=" + vertLanguage + ", fragLanguage=" + fragLanguage */+ ", presetDefines=" + presetDefines + ", usesNodes=" + usesNodes + ", shaderNodes=" + shaderNodes + ", shaderGenerationInfo=" + shaderGenerationInfo + ", renderState=" + renderState + ", forcedRenderState=" + forcedRenderState + ", lightMode=" + lightMode + ", shadowMode=" + shadowMode + ", defineParams=" + defineParams + ", worldBinds=" + worldBinds + ", noRender=" + noRender + '}';
    }
}
