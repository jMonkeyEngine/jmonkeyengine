/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.obj;

import com.jme3.asset.AssetManager;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.file.FbxId;
import java.util.logging.Logger;

public abstract class FbxObject<JT> {
    
    private static final Logger logger = Logger.getLogger(FbxObject.class.getName());
    
    protected AssetManager assetManager;
    protected String sceneFolderName;
    
    protected FbxId id;
    protected String name;
    protected String className;
    protected String subclassName;
    
    protected JT jmeObject; // lazily initialized
    
    protected FbxObject(AssetManager assetManager, String sceneFolderName) {
        this.assetManager = assetManager;
        this.sceneFolderName = sceneFolderName;
    }
    
    public FbxId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public String getSubclassName() {
        return subclassName;
    }
    
    public String getFullClassName() { 
        if (subclassName.equals("")) {
            return className;
        } else {
            return subclassName + " : " + className;
        }
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }

    protected void fromElement(FbxElement element) {
        id = FbxId.getObjectId(element);
        String nameAndClass;
        if (element.propertiesTypes.length == 3) {
            nameAndClass = (String) element.properties.get(1);
            subclassName = (String) element.properties.get(2);
        } else if (element.propertiesTypes.length == 2) {
            nameAndClass = (String) element.properties.get(0);
            subclassName = (String) element.properties.get(1);
        } else {
            throw new UnsupportedOperationException("This is not an FBX object: " + element.id);
        }
        
        int splitter = nameAndClass.indexOf("\u0000\u0001");
        
        if (splitter != -1) {
            name      = nameAndClass.substring(0, splitter);
            className = nameAndClass.substring(splitter + 2);
        } else {
            name      = nameAndClass;
            className = null;
        }
    }
    
    public final JT getJmeObject() {
        if (jmeObject == null) {
            jmeObject = toJmeObject();
            if (jmeObject == null) {
                throw new UnsupportedOperationException("FBX object subclass "
                                                      + "failed to resolve to a jME3 object");
            }
        }
        return jmeObject;
    }
    
    public final boolean isJmeObjectCreated() {
        return jmeObject != null;
    }
    
    protected final void unsupportedConnectObject(FbxObject object) {
        throw new IllegalArgumentException("Cannot attach objects of this class (" + 
                                            object.getFullClassName() + 
                                            ") to " + getClass().getSimpleName());
    }
    
    protected final void unsupportedConnectObjectProperty(FbxObject object, String property) {
        throw new IllegalArgumentException("Cannot attach objects of this class (" + 
                                            object.getFullClassName() + 
                                            ") to property " + getClass().getSimpleName() + 
                                            "[\"" + property + "\"]");
    }
    
    protected abstract JT toJmeObject();
    
    public abstract void connectObject(FbxObject object);
    
    public abstract void connectObjectProperty(FbxObject object, String property);
}
