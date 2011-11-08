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

package com.jme3.scene.plugins.ogre;

import com.jme3.asset.ModelKey;
import com.jme3.material.MaterialList;

/**
 * OgreMeshKey is used to load Ogre3D mesh.xml models with a specific
 * material file or list. This allows customizing from where the materials
 * are retrieved, instead of loading the material file as the same
 * name as the model (the default).
 * 
 * @author Kirill Vainer
 */
public class OgreMeshKey extends ModelKey {

    private MaterialList materialList;
    private String materialName;

    public OgreMeshKey(){
        super();
    }

    public OgreMeshKey(String name){
        super(name);
    }
    
    public OgreMeshKey(String name, MaterialList materialList){
        super(name);
        this.materialList = materialList;
    }
    
    public OgreMeshKey(String name, String materialName){
        super(name);
        this.materialName = materialName;
    }

    public MaterialList getMaterialList() {
        return materialList;
    }
    
    public void setMaterialList(MaterialList materialList){
        this.materialList = materialList;
    }
    
    public String getMaterialName() {
        return materialName;
    }
    
    public void setMaterialName(String name) {
        materialName = name;
    }

}
