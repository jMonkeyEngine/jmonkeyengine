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

import com.jme3.asset.AssetManager;
import com.jme3.shader.VarType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MaterialDef {

    private static final Logger logger = Logger.getLogger(MaterialDef.class.getName());

    private String name;
    private String assetName;
    private AssetManager assetManager;

    private List<TechniqueDef> defaultTechs;
    private Map<String, TechniqueDef> techniques;
    private Map<String, MatParam> matParams;

    public MaterialDef(){
    }
    
    public MaterialDef(AssetManager assetManager, String name){
        this.assetManager = assetManager;
        this.name = name;
        techniques = new HashMap<String, TechniqueDef>();
        matParams = new HashMap<String, MatParam>();
        defaultTechs = new ArrayList<TechniqueDef>();
        logger.log(Level.INFO, "Loaded material definition: {0}", name);
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public AssetManager getAssetManager(){
        return assetManager;
    }

    public String getName(){
        return name;
    }

    public void addMaterialParam(VarType type, String name, Object value, FixedFuncBinding ffBinding) {
        matParams.put(name, new MatParam(type, name, value, ffBinding));
    }
    
    public MatParam getMaterialParam(String name){
        return matParams.get(name);
    }

    public void addTechniqueDef(TechniqueDef technique){
        if (technique.getName().equals("Default")){
            defaultTechs.add(technique);
        }else{
            techniques.put(technique.getName(), technique);
        }
    }

    public List<TechniqueDef> getDefaultTechniques(){
        return defaultTechs;
    }

    public TechniqueDef getTechniqueDef(String name) {
        return techniques.get(name);
    }

}
