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
import com.jme3.light.LightList;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.logic.TechniqueDefLogic;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.util.ListMap;
import com.jme3.util.SafeArrayList;
import java.util.EnumSet;

/**
 * Represents a technique instance.
 */
public final class Technique {

    private final TechniqueDef def;
    private final Material owner;
    private final DefineList paramDefines;
    private final DefineList dynamicDefines;

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
        this.paramDefines = def.createDefineList();
        this.dynamicDefines = def.createDefineList();
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
     * Called by the material to tell the technique a parameter was modified.
     * Specify <code>null</code> for value if the param is to be cleared.
     */
    final void notifyParamChanged(String paramName, VarType type, Object value) {
        Integer defineId = def.getShaderParamDefineId(paramName);

        if (defineId == null) {
            return;
        }

        paramDefines.set(defineId, type, value);
    }
    
    /**
     * Called by the material to tell the technique that it has been made
     * current.
     * The technique updates dynamic defines based on the
     * currently set material parameters.
     */
    final void notifyTechniqueSwitched() {
        ListMap<String, MatParam> paramMap = owner.getParamsMap();
        paramDefines.clear();
        for (int i = 0; i < paramMap.size(); i++) {
            MatParam param = paramMap.getValue(i);
            notifyParamChanged(param.getName(), param.getVarType(), param.getValue());
        }
    }

    private void applyOverrides(DefineList defineList, SafeArrayList<MatParamOverride> overrides) {
        for (MatParamOverride override : overrides.getArray()) {
            if (!override.isEnabled()) {
                continue;
            }
            Integer defineId = def.getShaderParamDefineId(override.name);
            if (defineId != null) {
                if (def.getDefineIdType(defineId) == override.type) {
                    defineList.set(defineId, override.type, override.value);
                }
            }
        }
    }

    /**
     * Called by the material to determine which shader to use for rendering.
     * 
     * The {@link TechniqueDefLogic} is used to determine the shader to use
     * based on the {@link LightMode}.
     * 
     * @param renderManager The render manager for which the shader is to be selected.
     * @param rendererCaps The renderer capabilities which the shader should support.
     * @return A compatible shader.
     */
    Shader makeCurrent(RenderManager renderManager, SafeArrayList<MatParamOverride> worldOverrides,
            SafeArrayList<MatParamOverride> forcedOverrides,
            LightList lights, EnumSet<Caps> rendererCaps) {
        TechniqueDefLogic logic = def.getLogic();
        AssetManager assetManager = owner.getMaterialDef().getAssetManager();

        dynamicDefines.clear();
        dynamicDefines.setAll(paramDefines);

        if (worldOverrides != null) {
            applyOverrides(dynamicDefines, worldOverrides);
        }
        if (forcedOverrides != null) {
            applyOverrides(dynamicDefines, forcedOverrides);
        }

        return logic.makeCurrent(assetManager, renderManager, rendererCaps, lights, dynamicDefines);
    }
    
    /**
     * Render the technique according to its {@link TechniqueDefLogic}.
     * 
     * @param renderManager The render manager to perform the rendering against.
     * @param shader The shader that was selected in 
     * {@link #makeCurrent(com.jme3.renderer.RenderManager, java.util.EnumSet)}.
     * @param geometry The geometry to render
     * @param lights Lights which influence the geometry.
     */
    void render(RenderManager renderManager, Shader shader, Geometry geometry, LightList lights, int lastTexUnit) {
        TechniqueDefLogic logic = def.getLogic();
        logic.render(renderManager, shader, geometry, lights, lastTexUnit);
    }
    
    /**
     * Get the {@link DefineList} for dynamic defines.
     * 
     * Dynamic defines are used to implement material parameter -> define
     * bindings as well as {@link TechniqueDefLogic} specific functionality.
     * 
     * @return all dynamic defines.
     */
    public DefineList getDynamicDefines() {
        return dynamicDefines;
    }
    
    /**
     * @return nothing.
     *
     * @deprecated Preset defines are precompiled into
     * {@link TechniqueDef#getShaderPrologue()}, whereas dynamic defines are
     * available via {@link #getParamDefines()}.
     */
    @Deprecated
    public DefineList getAllDefines() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Compute the sort ID. Similar to {@link Object#hashCode()} but used
     * for sorting geometries for rendering.
     * 
     * @return the sort ID for this technique instance.
     */
    public int getSortId() {
        int hash = 17;
        hash = hash * 23 + def.getSortId();
        hash = hash * 23 + paramDefines.hashCode();
        return hash;
    }
}
