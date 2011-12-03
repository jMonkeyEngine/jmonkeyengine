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

import com.jme3.export.*;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.shader.DefineList;
import com.jme3.shader.UniformBinding;
import com.jme3.shader.VarType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

/**
 * Describes a technique definition.
 * 
 * @author Kirill Vainer
 */
public class TechniqueDef implements Savable {

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
         * Enable light rendering by using the 
         * {@link Renderer#setLighting(com.jme3.light.LightList) renderer's setLighting} 
         * method.
         * <p>
         * The specific details of rendering the lighting is up to the 
         * renderer implementation.
         */
        FixedPipeline,
    }

    public enum ShadowMode {
        Disable,
        InPass,
        PostPass,
    }

    private EnumSet<Caps> requiredCaps = EnumSet.noneOf(Caps.class);
    private String name;

    private String vertName;
    private String fragName;
    private String shaderLang;
    private DefineList presetDefines;
    private boolean usesShaders;

    private RenderState renderState;
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
        this.name = name == null ? "Default" : name;
    }

    /**
     * Serialization only. Do not use.
     */
    public TechniqueDef(){
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
     * Returns true if this technique uses shaders, false otherwise.
     * 
     * @return true if this technique uses shaders, false otherwise.
     * 
     * @see #setShaderFile(java.lang.String, java.lang.String, java.lang.String) 
     */
    public boolean isUsingShaders(){
        return usesShaders;
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
     * @param shaderLanguage The shader language
     */
    public void setShaderFile(String vertexShader, String fragmentShader, String shaderLanguage){
        this.vertName = vertexShader;
        this.fragName = fragmentShader;
        this.shaderLang = shaderLanguage;

        Caps langCap = Caps.valueOf(shaderLanguage);
        requiredCaps.add(langCap);

        usesShaders = true;
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
        if (defineParams == null)
            return null;
        
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
        if (defineParams == null)
            defineParams = new HashMap<String, String>();

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
        if (presetDefines == null)
            presetDefines = new DefineList();

        presetDefines.set(defineName, type, value);
    }

    /**
     * Returns the name of the fragment shader used by the technique, or null
     * if no fragment shader is specified.
     * 
     * @return the name of the fragment shader to be used.
     */
    public String getFragmentShaderName() {
        return fragName;
    }

    
    /**
     * Returns the name of the vertex shader used by the technique, or null
     * if no vertex shader is specified.
     * 
     * @return the name of the vertex shader to be used.
     */
    public String getVertexShaderName() {
        return vertName;
    }

    /**
     * Returns the shader language of the shaders used in this technique.
     * 
     * @return the shader language of the shaders used in this technique.
     */
    public String getShaderLanguage() {
        return shaderLang;
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
        oc.write(vertName, "vertName", null);
        oc.write(fragName, "fragName", null);
        oc.write(shaderLang, "shaderLang", null);
        oc.write(presetDefines, "presetDefines", null);
        oc.write(lightMode, "lightMode", LightMode.Disable);
        oc.write(shadowMode, "shadowMode", ShadowMode.Disable);
        oc.write(renderState, "renderState", null);
        oc.write(usesShaders, "usesShaders", false);
        // TODO: Finish this when Map<String, String> export is available
//        oc.write(defineParams, "defineParams", null);
        // TODO: Finish this when List<Enum> export is available
//        oc.write(worldBinds, "worldBinds", null);
    }

    public void read(JmeImporter im) throws IOException{
        InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", null);
        vertName = ic.readString("vertName", null);
        fragName = ic.readString("fragName", null);
        shaderLang = ic.readString("shaderLang", null);
        presetDefines = (DefineList) ic.readSavable("presetDefines", null);
        lightMode = ic.readEnum("lightMode", LightMode.class, LightMode.Disable);
        shadowMode = ic.readEnum("shadowMode", ShadowMode.class, ShadowMode.Disable);
        renderState = (RenderState) ic.readSavable("renderState", null);
        usesShaders = ic.readBoolean("usesShaders", false);
    }
    
}
