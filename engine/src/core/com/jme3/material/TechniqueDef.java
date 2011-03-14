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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.Caps;
import com.jme3.shader.DefineList;
import com.jme3.shader.UniformBinding;
import com.jme3.shader.VarType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public class TechniqueDef implements Savable {

    public enum LightMode {
        Disable,
        SinglePass,
        MultiPass,
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

    public TechniqueDef(String name){
        this.name = name == null ? "Default" : name;
    }

    /**
     * Do not use this constructor.
     */
    public TechniqueDef(){
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

    public String getName(){
        return name;
    }

    public LightMode getLightMode() {
        return lightMode;
    }

    public void setLightMode(LightMode lightMode) {
        this.lightMode = lightMode;
    }

    public ShadowMode getShadowMode() {
        return shadowMode;
    }

    public void setShadowMode(ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }

    public RenderState getRenderState() {
        return renderState;
    }

    public void setRenderState(RenderState renderState) {
        this.renderState = renderState;
    }

    public boolean isUsingShaders(){
        return usesShaders;
    }

    public EnumSet<Caps> getRequiredCaps() {
        return requiredCaps;
    }

    public void setShaderFile(String vert, String frag, String lang){
        this.vertName = vert;
        this.fragName = frag;
        this.shaderLang = lang;

        Caps langCap = Caps.valueOf(lang);
        requiredCaps.add(langCap);

        usesShaders = true;
    }

    public DefineList getShaderPresetDefines() {
        return presetDefines;
    }

    public String getShaderParamDefine(String paramName){
        if (defineParams == null)
            return null;
        
        return defineParams.get(paramName);
    }

    public void addShaderParamDefine(String paramName, String defineName){
        if (defineParams == null)
            defineParams = new HashMap<String, String>();

        defineParams.put(paramName, defineName);
    }

    public void addShaderPresetDefine(String defineName, VarType type, Object value){
        if (presetDefines == null)
            presetDefines = new DefineList();

        presetDefines.set(defineName, type, value);
    }

    public String getFragName() {
        return fragName;
    }

    public String getVertName() {
        return vertName;
    }

    public String getShaderLanguage() {
        return shaderLang;
    }

    public boolean addWorldParam(String name) {
        if (worldBinds == null){
            worldBinds = new ArrayList<UniformBinding>();
        }
        for (UniformBinding binding : UniformBinding.values()) {
            if (binding.name().equals(name)) {
                worldBinds.add(binding);
                return true;
            }
        }
        return false;
    }

    public List<UniformBinding> getWorldBindings() {
        return worldBinds;
    }

}
