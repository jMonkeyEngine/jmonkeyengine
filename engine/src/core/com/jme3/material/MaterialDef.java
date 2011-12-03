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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describes a J3MD (Material definition).
 * 
 * @author Kirill Vainer
 */
public class MaterialDef {

    private static final Logger logger = Logger.getLogger(MaterialDef.class.getName());

    private String name;
    private String assetName;
    private AssetManager assetManager;

    private List<TechniqueDef> defaultTechs;
    private Map<String, TechniqueDef> techniques;
    private Map<String, MatParam> matParams;

    /**
     * Serialization only. Do not use.
     */
    public MaterialDef(){
    }
    
    /**
     * Creates a new material definition with the given name.
     * 
     * @param assetManager The asset manager to use to load shaders
     * @param name The debug name of the material definition
     */
    public MaterialDef(AssetManager assetManager, String name){
        this.assetManager = assetManager;
        this.name = name;
        techniques = new HashMap<String, TechniqueDef>();
        matParams = new HashMap<String, MatParam>();
        defaultTechs = new ArrayList<TechniqueDef>();
        logger.log(Level.INFO, "Loaded material definition: {0}", name);
    }

    /**
     * Returns the asset key name of the asset from which this material 
     * definition was loaded.
     * 
     * @return Asset key name of the j3md file 
     */
    public String getAssetName() {
        return assetName;
    }

    /**
     * Set the asset key name. 
     * 
     * @param assetName the asset key name
     */
    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    /**
     * Returns the AssetManager passed in the constructor.
     * 
     * @return the AssetManager passed in the constructor.
     */
    public AssetManager getAssetManager(){
        return assetManager;
    }

    /**
     * The debug name of the material definition.
     * 
     * @return debug name of the material definition.
     */
    public String getName(){
        return name;
    }

    /**
     * Adds a new material parameter.
     * 
     * @param type Type of the parameter
     * @param name Name of the parameter
     * @param value Default value of the parameter
     * @param ffBinding Fixed function binding for the parameter
     */
    public void addMaterialParam(VarType type, String name, Object value, FixedFuncBinding ffBinding) {
        matParams.put(name, new MatParam(type, name, value, ffBinding));
    }
    
    /**
     * Returns the material parameter with the given name.
     * 
     * @param name The name of the parameter to retrieve
     * 
     * @return The material parameter, or null if it does not exist.
     */
    public MatParam getMaterialParam(String name){
        return matParams.get(name);
    }
    
    /**
     * Returns a collection of all material parameters declared in this
     * material definition.
     * <p>
     * Modifying the material parameters or the collection will lead
     * to undefined results.
     * 
     * @return All material parameters declared in this definition.
     */
    public Collection<MatParam> getMaterialParams(){
        return matParams.values();
    }

    /**
     * Adds a new technique definition to this material definition.
     * <p>
     * If the technique name is "Default", it will be added
     * to the list of {@link MaterialDef#getDefaultTechniques() default techniques}.
     * 
     * @param technique The technique definition to add.
     */
    public void addTechniqueDef(TechniqueDef technique){
        if (technique.getName().equals("Default")){
            defaultTechs.add(technique);
        }else{
            techniques.put(technique.getName(), technique);
        }
    }

    /**
     * Returns a list of all default techniques.
     * 
     * @return a list of all default techniques.
     */
    public List<TechniqueDef> getDefaultTechniques(){
        return defaultTechs;
    }

    /**
     * Returns a technique definition with the given name.
     * This does not include default techniques which can be
     * retrieved via {@link MaterialDef#getDefaultTechniques() }.
     * 
     * @param name The name of the technique definition to find
     * 
     * @return The technique definition, or null if cannot be found.
     */
    public TechniqueDef getTechniqueDef(String name) {
        return techniques.get(name);
    }

}
