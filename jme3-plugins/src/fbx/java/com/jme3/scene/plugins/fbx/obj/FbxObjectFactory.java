/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import com.jme3.scene.plugins.fbx.anim.FbxAnimCurve;
import com.jme3.scene.plugins.fbx.anim.FbxAnimCurveNode;
import com.jme3.scene.plugins.fbx.anim.FbxAnimLayer;
import com.jme3.scene.plugins.fbx.anim.FbxAnimStack;
import com.jme3.scene.plugins.fbx.anim.FbxBindPose;
import com.jme3.scene.plugins.fbx.anim.FbxCluster;
import com.jme3.scene.plugins.fbx.anim.FbxLimbNode;
import com.jme3.scene.plugins.fbx.anim.FbxSkinDeformer;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.material.FbxImage;
import com.jme3.scene.plugins.fbx.material.FbxMaterial;
import com.jme3.scene.plugins.fbx.material.FbxTexture;
import com.jme3.scene.plugins.fbx.mesh.FbxMesh;
import com.jme3.scene.plugins.fbx.node.FbxNode;
import com.jme3.scene.plugins.fbx.node.FbxNullAttribute;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for producing FBX objects given an FBXElement.
 */
public final class FbxObjectFactory {
    
    private static final Logger logger = Logger.getLogger(FbxObjectFactory.class.getName());
    
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private FbxObjectFactory() {
    }

    private static Class<? extends FbxObject> getImplementingClass(String elementName, String subclassName) {
        if (elementName.equals("NodeAttribute")) {
            if (subclassName.equals("Root")) {
                // Root of skeleton, may not actually be set.
                return FbxNullAttribute.class;
            } else if (subclassName.equals("LimbNode")) {
                // Specifies some limb attributes, optional.
                return FbxNullAttribute.class;
            } else if (subclassName.equals("Null")) {
                // An "Empty" or "Node" without any specific behavior.
                return FbxNullAttribute.class;
            } else if (subclassName.equals("IKEffector") ||
                       subclassName.equals("FKEffector")) {
                // jME3 does not support IK.
                return FbxNullAttribute.class;
            } else if (subclassName.equals("Light")) {
                // TODO: support lights
                return FbxNullAttribute.class;
            } else if (subclassName.equals("Camera")) {
                // TODO: support cameras
                return FbxNullAttribute.class;
            } else {
                // NodeAttribute - Unknown
                logger.log(Level.WARNING, "Unknown object subclass: {0}. Ignoring.", subclassName);
                return FbxUnknownObject.class;
            }
        } else if (elementName.equals("Geometry") && subclassName.equals("Mesh")) {
            // NodeAttribute - Mesh Data
            return FbxMesh.class;
        } else if (elementName.equals("Model")) {
            // Scene Graph Node
            // Determine specific subclass (e.g. Mesh, Null, or LimbNode?)
            if (subclassName.equals("LimbNode")) {
                return FbxLimbNode.class; // Child Bone of Skeleton?
            } else {
                return FbxNode.class;
            }
        } else if (elementName.equals("Pose")) {
            if (subclassName.equals("BindPose")) {
                // Bind Pose Information
                return FbxBindPose.class;
            } else {
                // Rest Pose Information
                // OR
                // Other Data (???)
                logger.log(Level.WARNING, "Unknown object subclass: {0}. Ignoring.", subclassName);
                return FbxUnknownObject.class;
            }
        } else if (elementName.equals("Material")) {
            return FbxMaterial.class;
        } else if (elementName.equals("Deformer")) {
            // Deformer
            if (subclassName.equals("Skin")) {
                // FBXSkinDeformer (mapping between FBXMesh & FBXClusters)
                return FbxSkinDeformer.class;
            } else if (subclassName.equals("Cluster")) {
                // Cluster (aka mapping between FBXMesh vertices & weights for bone)
                return FbxCluster.class;
            } else {
                logger.log(Level.WARNING, "Unknown deformer subclass: {0}. Ignoring.", subclassName);
                return FbxUnknownObject.class;
            }
        } else if (elementName.equals("Video")) {
            if (subclassName.equals("Clip")) {
                return FbxImage.class;
            } else {
                logger.log(Level.WARNING, "Unknown object subclass: {0}. Ignoring.", subclassName);
                return FbxUnknownObject.class;
            }
        } else if (elementName.equals("Texture")) {
            return FbxTexture.class;
        } else if (elementName.equals("AnimationStack")) {
            // AnimationStack (jME Animation)
            return FbxAnimStack.class;
        } else if (elementName.equals("AnimationLayer")) {
            // AnimationLayer (for blended animation - not supported)
            return FbxAnimLayer.class;
        } else if (elementName.equals("AnimationCurveNode")) {
            // AnimationCurveNode
            return FbxAnimCurveNode.class;
        } else if (elementName.equals("AnimationCurve")) {
            // AnimationCurve (Data)
            return FbxAnimCurve.class;
        } else if (elementName.equals("SceneInfo")) {
            // Old-style FBX 6.1 uses this. Nothing useful here.
            return FbxUnknownObject.class;
        } else {
            logger.log(Level.WARNING, "Unknown object class: {0}. Ignoring.", elementName);
            return FbxUnknownObject.class;
        }
    }
    
    /**
     * Automatically create an FBXObject by inspecting its class / subclass
     * properties.
     * 
     * @param element The element from which to create an object.
     * @param assetManager AssetManager to load dependent resources
     * @param sceneFolderName Folder relative to which resources shall be loaded
     * @return The object, or null if not supported (?)
     */
    public static FbxObject createObject(FbxElement element, AssetManager assetManager, String sceneFolderName) {
        String elementName = element.id;
        String subclassName;
        
        if (element.propertiesTypes.length == 3) {
            // FBX 7.x (all objects start with Long ID)
            subclassName = (String) element.properties.get(2);
        } else if (element.propertiesTypes.length == 2) {
            // FBX 6.x (objects only have name and subclass)
            subclassName = (String) element.properties.get(1);
        } else {
            // Not an object or invalid data.
            return null;
        }
        
        Class<? extends FbxObject> javaFbxClass = getImplementingClass(elementName, subclassName);
        
        if (javaFbxClass != null) {
            try {
                // This object is supported by FBX importer, create new instance.
                // Import the data into the object from the element, then return it.
                Constructor<? extends FbxObject> ctor = javaFbxClass.getConstructor(AssetManager.class, String.class);
                FbxObject obj = ctor.newInstance(assetManager, sceneFolderName);
                obj.fromElement(element);
                
                String subClassName = elementName + ", " + subclassName;
                if (obj.assetManager == null) {
                    throw new IllegalStateException("FBXObject subclass (" + subClassName + 
                                                    ") forgot to call super() in their constructor");
                } else if (obj.className == null) {
                    throw new IllegalStateException("FBXObject subclass (" + subClassName + 
                                                    ") forgot to call super.fromElement() in their fromElement() implementation");
                }
                return obj;
            } catch (InvocationTargetException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException ex) {
                // Programmer error.
                throw new IllegalStateException(ex);
            }
        }
        
        // Not supported object.
        return null;
    }
}
