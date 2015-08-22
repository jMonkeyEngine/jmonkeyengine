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
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxCluster extends FbxObject {

    private static final Logger logger = Logger.getLogger(FbxCluster.class.getName());
    
    private int[] indexes;
    private double[] weights;
    private FbxLimbNode limb;
    
    private Matrix4f transformMatrix;
    private Matrix4f transformLinkMatrix;
    private Matrix4f transformAssociateModelMatrix;
    
    public FbxCluster(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        for (FbxElement e : element.children) {
            if (e.id.equals("Indexes")) {
                indexes = (int[]) e.properties.get(0);
            } else if (e.id.equals("Weights")) {
                weights = (double[]) e.properties.get(0);
            } else if (e.id.equals("Transform")) {
                double[] data = (double[]) e.properties.get(0);
                transformMatrix = FbxAnimUtil.toMatrix4(data);
            } else if (e.id.equals("TransformLink")) {
                double[] data = (double[]) e.properties.get(0);
                transformLinkMatrix = FbxAnimUtil.toMatrix4(data);
            } else if (e.id.equals("TransformAssociateModel")) {
                double[] data = (double[]) e.properties.get(0);
                transformAssociateModelMatrix = FbxAnimUtil.toMatrix4(data);
            }
        }
    }

    public int[] getVertexIndices() {
        return indexes;
    }

    public double[] getWeights() {
        return weights;
    }

    public FbxLimbNode getLimb() {
        return limb;
    }
    
    @Override
    protected Object toJmeObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void connectObject(FbxObject object) {
        if (object instanceof FbxLimbNode) {
            if (limb != null) {
                logger.log(Level.WARNING, "This cluster already has a limb attached. Ignoring.");
                return;
            }
            limb = (FbxLimbNode) object;
            
            System.out.println(" ----- for limb: " + limb.getName());
            System.out.println(" transform : " + transformMatrix);
            System.out.println(" transform link : " + transformLinkMatrix);
            System.out.println(" transform associate model : " + transformAssociateModelMatrix);
            
            //  Invert(Invert(TransformLinkMatrix) * TransformMatrix * Geometry)
            Matrix4f accumMatrix = transformLinkMatrix.invert();
            accumMatrix.multLocal(transformMatrix);
            accumMatrix.invertLocal();
            
            System.out.println(" limb bind pose : " + accumMatrix);
        } else {
            unsupportedConnectObject(object);
        }
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        unsupportedConnectObjectProperty(object, property);
    }
}