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
package com.jme3.scene.plugins.fbx.anim;

import com.jme3.asset.AssetManager;
import com.jme3.math.Matrix4f;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.file.FbxId;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import java.util.HashMap;
import java.util.Map;

public class FbxBindPose extends FbxObject<Map<FbxId, Matrix4f>> {

    private final Map<FbxId, Matrix4f> bindPose = new HashMap<FbxId, Matrix4f>();
    
    public FbxBindPose(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        for (FbxElement child : element.children) {
            if (!child.id.equals("PoseNode")) {
                continue;
            }
            
            FbxId node = null;
            float[] matData = null;
            
            for (FbxElement e : child.children) {
                if (e.id.equals("Node")) {
                    node = FbxId.create(e.properties.get(0));
                } else if (e.id.equals("Matrix")) {
                    double[] matDataDoubles = (double[]) e.properties.get(0);
                    
                    if (matDataDoubles.length != 16) {
                        // corrupt
                        throw new UnsupportedOperationException("Bind pose matrix "
                                + "must have 16 doubles, but it has " 
                                + matDataDoubles.length + ". Data is corrupt");
                    }
                    
                    matData = new float[16];
                    for (int i = 0; i < matDataDoubles.length; i++) {
                        matData[i] = (float) matDataDoubles[i];
                    }
                }
            }
            
            if (node != null && matData != null) {
                Matrix4f matrix = new Matrix4f(matData);
                bindPose.put(node, matrix);
            }
        }
    }
    
    @Override
    protected Map<FbxId, Matrix4f> toJmeObject() {
        return bindPose;
    }

    @Override
    public void connectObject(FbxObject object) {
        unsupportedConnectObject(object);
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        unsupportedConnectObjectProperty(object, property);
    }
    
}
