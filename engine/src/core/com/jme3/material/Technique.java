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
import com.jme3.renderer.Caps;
import com.jme3.shader.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a technique instance.
 */
public class Technique /* implements Savable */ {

    private static final Logger logger = Logger.getLogger(Technique.class.getName());
    private TechniqueDef def;
    private Material owner;
    private ArrayList<Uniform> worldBindUniforms;
    private DefineList defines;
    private Shader shader;
    private boolean needReload = true;

    /**
     * Creates a new technique instance that implements the given
     * technique definition.
     * 
     * @param owner The material that will own this technique
     * @param def The technique definition being implemented.
     */
    public Technique(Material owner, TechniqueDef def) {
        this.owner = owner;
        this.def = def;
        if (def.isUsingShaders()) {
            this.worldBindUniforms = new ArrayList<Uniform>();
            this.defines = new DefineList();
        }
    }

    /**
     * Serialization only. Do not use.
     */
    public Technique() {
    }

    /**
     * Returns the technique definition that is implemented by this technique
     * instance. 
     * 
     * @return the technique definition that is implemented by this technique
     * instance. 
     */
    public TechniqueDef getDef() {
        return def;
    }

    /**
     * Returns the shader currently used by this technique instance.
     * <p>
     * Shaders are typically loaded dynamically when the technique is first
     * used, therefore, this variable will most likely be null most of the time.
     * 
     * @return the shader currently used by this technique instance.
     */
    public Shader getShader() {
        return shader;
    }

    /**
     * Returns a list of uniforms that implements the world parameters
     * that were requested by the material definition.
     * 
     * @return a list of uniforms implementing the world parameters.
     */
    public List<Uniform> getWorldBindUniforms() {
        return worldBindUniforms;
    }

    /**
     * Called by the material to tell the technique a parameter was modified.
     * Specify <code>null</code> for value if the param is to be cleared.
     */
    void notifyParamChanged(String paramName, VarType type, Object value) {
        // Check if there's a define binding associated with this
        // parameter.
        String defineName = def.getShaderParamDefine(paramName);
        if (defineName != null) {
            // There is a define. Change it on the define list.
            // The "needReload" variable will determine
            // if the shader will be reloaded when the material
            // is rendered.
            
            if (value == null) {
                // Clear the define.
                needReload = defines.remove(defineName) || needReload;
            } else {
                // Set the define.
                needReload = defines.set(defineName, type, value) || needReload;
            }
        }
    }

    void updateUniformParam(String paramName, VarType type, Object value) {
        if (paramName == null) {
            throw new IllegalArgumentException();
        }
        
        Uniform u = shader.getUniform(paramName);
        switch (type) {
            case TextureBuffer:
            case Texture2D: // fall intentional
            case Texture3D:
            case TextureArray:
            case TextureCubeMap:
            case Int:
                u.setValue(VarType.Int, value);
                break;
            default:
                u.setValue(type, value);
                break;
        }
    }

    /**
     * Returns true if the technique must be reloaded.
     * <p>
     * If a technique needs to reload, then the {@link Material} should
     * call {@link #makeCurrent(com.jme3.asset.AssetManager) } on this
     * technique.
     * 
     * @return true if the technique must be reloaded.
     */
    public boolean isNeedReload() {
        return needReload;
    }

    /**
     * Prepares the technique for use by loading the shader and setting
     * the proper defines based on material parameters.
     * 
     * @param assetManager The asset manager to use for loading shaders.
     */
    public void makeCurrent(AssetManager assetManager, boolean techniqueSwitched, EnumSet<Caps> rendererCaps) {
        if (!def.isUsingShaders()) {
            // No shaders are used, no processing is neccessary. 
            return;
        }
        
        if (techniqueSwitched) {
            // If the technique was switched, check if the define list changed
            // based on material parameters.
            
            Collection<MatParam> params = owner.getParams();
                        
            if (!defines.equalsParams(params,def)) {
                // Defines were changed, update define list
                defines.clear();
                for (MatParam param : params) {
                    String defineName = def.getShaderParamDefine(param.getName());
                    if (defineName != null) {
                        defines.set(defineName, param.getVarType(), param.getValue());
                    }
                }
                needReload = true;
            }
        }

        if (needReload) {
            loadShader(assetManager,rendererCaps);
        }
    }

    private void loadShader(AssetManager manager,EnumSet<Caps> rendererCaps) {
        
        ShaderKey key = new ShaderKey(def.getVertexShaderName(),
                    def.getFragmentShaderName(),
                    getAllDefines(),
                    def.getVertexShaderLanguage(),
                    def.getFragmentShaderLanguage());
        
        if (getDef().isUsingShaderNodes()) {                 
           manager.getShaderGenerator(rendererCaps).initialize(this);           
           key.setUsesShaderNodes(true);
        }   
        shader = manager.loadShader(key);

        // register the world bound uniforms
        worldBindUniforms.clear();
        if (def.getWorldBindings() != null) {
           for (UniformBinding binding : def.getWorldBindings()) {
               Uniform uniform = shader.getUniform("g_" + binding.name());
               uniform.setBinding(binding);
               if (uniform != null) {
                   worldBindUniforms.add(uniform);
               }
           }
        }        
        needReload = false;
    }
    
    /**
     * Computes the define list
     * @return the complete define list
     */
    public DefineList getAllDefines() {
        DefineList allDefines = new DefineList();
        allDefines.addFrom(def.getShaderPresetDefines());
        allDefines.addFrom(defines);
        return allDefines;
    } 
    
    /*
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(def, "def", null);
        oc.writeSavableArrayList(worldBindUniforms, "worldBindUniforms", null);
        oc.write(defines, "defines", null);
        oc.write(shader, "shader", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        def = (TechniqueDef) ic.readSavable("def", null);
        worldBindUniforms = ic.readSavableArrayList("worldBindUniforms", null);
        defines = (DefineList) ic.readSavable("defines", null);
        shader = (Shader) ic.readSavable("shader", null);
    }
    */
}
