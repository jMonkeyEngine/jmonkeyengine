/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

package com.jme3.vectoreffect;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.util.ArrayList;

/**
 *
 * @author yaRnMcDonuts
 */
public class VectorGroup {
    
    private final ArrayList vectorList = new ArrayList();    
        
    public int getSize(){
        return vectorList.size();
    }
    
    public VectorGroup(Object... vectorObjects) {
        for(int v = 0; v < vectorObjects.length; v++){
            addVectorToGroup(vectorObjects[v]);
        }
    }
    
    public final void addVectorToGroup(Object vectorObj){
        if(isValidVectorObject(vectorObj)){
            vectorList.add(vectorObj);
        } else{  
            throw new IllegalArgumentException(  "VectorGroup must contain valid vector-type objects. This includes: Vector2f, Vector3f, Vector4f, and ColorRGBA/ Incompatible type: " + vectorObj.getClass().getSimpleName());
        }
    }
    
    
   //regardless of type, all vectors in a VectorGroup are converted to and from Vector4f when being operated on internally by a VectorEffect.     
    public Vector4f getAsVector4(int index) {
        Object vectorObj = vectorList.get(index);
        if (vectorObj instanceof Vector4f) {
            Vector4f vec4 = (Vector4f) vectorObj;
            return vec4.clone();  
        }
        else if (vectorObj instanceof Vector3f) {
            Vector3f vec3 = (Vector3f) vectorObj;
            return new Vector4f(vec3.x, vec3.y, vec3.z, 1f); 
        }
        else if (vectorObj instanceof Vector2f) {
            Vector2f vec2 = (Vector2f) vectorObj;
            return new Vector4f(vec2.x, vec2.y, 1f, 1f);  
        }
        else if (vectorObj instanceof ColorRGBA) {
            ColorRGBA color = (ColorRGBA) vectorObj;
            return color.toVector4f(); 
        }
        else {
            throw new IllegalStateException("VectorEffect supports only Vector2f, Vector3f, Vector4f, or ColorRGBA");
        }
    }

    public void updateVectorObject(Vector4f newVal, int index) {
        Object store = vectorList.get(index);
        if (store instanceof Vector4f) {
            ((Vector4f)store).set(newVal);  // set vec4
        }
        else if (store instanceof Vector3f) {
            ((Vector3f)store).set(newVal.x, newVal.y, newVal.z);  
        }
        else if (store instanceof Vector2f) {
            ((Vector2f)store).set(newVal.x, newVal.y);  // drop z,w
        }
        else if (store instanceof ColorRGBA) {
            ((ColorRGBA)store).set(newVal.x, newVal.y, newVal.z, newVal.w);  // map xyzw -> rgba
        }
        else {
            throw new IllegalStateException("VectorEffect supports only Vector2f, Vector3f, Vector4f, or ColorRGBA");
        }
    }
    
    private boolean isValidVectorObject(Object vectorObj){
        return (vectorObj instanceof Vector2f || vectorObj instanceof Vector3f || vectorObj instanceof Vector4f || vectorObj instanceof ColorRGBA || vectorObj instanceof VectorGroup);
    }
    
        
    public VectorGroup copy() {
        VectorGroup newCopy = new VectorGroup();

        for (Object vectorObj : vectorList) {
            if (vectorObj instanceof Vector2f) {
                Vector2f v2 = (Vector2f) vectorObj;
                newCopy.addVectorToGroup(v2.clone());
            } else if (vectorObj instanceof Vector3f) {
                Vector3f v3 = (Vector3f) vectorObj;
                newCopy.addVectorToGroup(v3.clone());
            } else if (vectorObj instanceof Vector4f) {
                Vector4f v4 = (Vector4f) vectorObj;
                newCopy.addVectorToGroup(v4.clone());
            } else if (vectorObj instanceof ColorRGBA) {
                ColorRGBA c = (ColorRGBA) vectorObj;
                newCopy.addVectorToGroup(c.clone());
            } else {
                throw new IllegalStateException("Unsupported object type: " + vectorObj.getClass());
            }
        }
        return newCopy;
    }       
}

