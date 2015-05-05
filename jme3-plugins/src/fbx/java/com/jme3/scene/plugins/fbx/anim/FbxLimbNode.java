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
import com.jme3.scene.plugins.fbx.node.FbxNode;
import java.util.ArrayList;
import java.util.List;

public class FbxLimbNode extends FbxNode {
    
    protected FbxNode skeletonHolder;
    protected Bone bone;
    
    public FbxLimbNode(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    private static void createBones(FbxNode skeletonHolderNode, FbxLimbNode limb, List<Bone> bones) {
        limb.skeletonHolder = skeletonHolderNode;
        
        Bone parentBone = limb.getJmeBone();
        bones.add(parentBone);
        
        for (FbxNode child : limb.children) {
            if (child instanceof FbxLimbNode) {
                FbxLimbNode childLimb = (FbxLimbNode) child;
                createBones(skeletonHolderNode, childLimb, bones);
                parentBone.addChild(childLimb.getJmeBone());
            }
        }
    }
    
    public static Skeleton createSkeleton(FbxNode skeletonHolderNode) {
        if (skeletonHolderNode instanceof FbxLimbNode) {
            throw new UnsupportedOperationException("Limb nodes cannot be skeleton holders");
        }
        
        List<Bone> bones = new ArrayList<Bone>();
        
        for (FbxNode child : skeletonHolderNode.getChildren()) {
            if (child instanceof FbxLimbNode) {
                createBones(skeletonHolderNode, (FbxLimbNode) child, bones);
            }
        }
        
        return new Skeleton(bones.toArray(new Bone[0]));
    }
    
    public FbxNode getSkeletonHolder() {
        return skeletonHolder;
    }
    
    public Bone getJmeBone() {
        if (bone == null) {
            bone = new Bone(name);
            bone.setBindTransforms(jmeLocalBindPose.getTranslation(),
                                   jmeLocalBindPose.getRotation(),
                                   jmeLocalBindPose.getScale());
        }
        return bone;
    }
}
