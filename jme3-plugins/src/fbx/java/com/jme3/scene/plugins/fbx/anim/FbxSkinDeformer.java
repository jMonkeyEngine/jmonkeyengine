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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.fbx.node.FbxNode;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import java.util.ArrayList;
import java.util.List;

public class FbxSkinDeformer extends FbxObject<Skeleton> {

    private FbxNode skeletonRoot;
    private final List<FbxCluster> clusters = new ArrayList<FbxCluster>();
    
    public FbxSkinDeformer(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    private boolean isHierarchyCompatible(Bone thisBone, Bone otherBone) {
        Transform thisTransform = thisBone.getBindInverseTransform();
        Transform otherTransform = otherBone.getBindInverseTransform();
        throw new UnsupportedOperationException();
    }
    
    /** 
     * Determine if both skin deformers can share the same 
     * Skeleton object and hence the same SkeletonControl / AnimControl. 
     * 
     * @param skinDeformer The skin deformer to test compatibility against.
     * @return True if the skeletons are identical and can be shared, false
     * otherwise.
     */
    public boolean isCompatible(FbxSkinDeformer skinDeformer) {
        Skeleton thisSkeleton = this.getJmeObject();
        Skeleton otherSkeleton = skinDeformer.getJmeObject();
        Bone[] thisRoots = thisSkeleton.getRoots();
        Bone[] otherRoots = otherSkeleton.getRoots();
        for (int i = 0; i < thisRoots.length; i++) {
            
        }
        throw new UnsupportedOperationException();
    }
    
    /** 
     * Get the root FbxNode containing the skeleton.
     * 
     * The node should have one or more FbxLimbNodes which are
     * the root limbs of the skeleton structure.
     * 
     * This is null until prepareSkeletonData() is called.
     * 
     * @return The root node containing the skeleton.
     */
    public FbxNode getSkeletonRoot() {
        return skeletonRoot;
    }
    
    /** 
     * Derives the skeleton from the skin deformer. 
     * 
     * The Skeleton hierarchy is derived via the {@link #getSkeletonRoot() skeleton root}
     *  whereas the bind poses for the bones is derived from the 
     * {@link #getClusters() clusters}.
     * 
     * FbxLimbNode.prepareSkeletonData() must have been called first
     * The bone's bind pose depends on each cluster's TransformLinkMatrix 
     * and TransformMatrix.
     * The bone's bind pose is derived as follows:
     * <code><pre>
     * Invert(Invert(TransformLinkMatrix) * TransformMatrix * Geometry)
     * </code></pre>
     * 
     * @return The skeleton as described by this skin deformer.
     */
    @Override
    protected Skeleton toJmeObject() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Get the clusters attached to this skin deformer.
     * 
     * @return The skin deformer's clusters.
     */
    public List<FbxCluster> getClusters() {
        return clusters;
    }
    
    @Override
    public void connectObject(FbxObject object) {
        if (object instanceof FbxCluster) {
            clusters.add((FbxCluster) object);
        } else {
            unsupportedConnectObject(object);
        }
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        unsupportedConnectObjectProperty(object, property);
    }

}
