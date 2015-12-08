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
import com.jme3.scene.plugins.fbx.node.FbxNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Similar to {@link Skeleton jME skeleton} except 
 * contains {@link FbxLimbNode limb nodes}.
 * 
 * This is used to determine the bone indices (for assigning clusters to meshes)
 * as well as the limb hierarchy when creating the jME3 Skeleton.
 * 
 * @author Kirill Vainer
 */
public class FbxSkeleton {
    
    FbxLimbNode[] rootLimbs;
    FbxLimbNode[] allLimbs;
    HashMap<FbxLimbNode, Integer> limbToIndexMap = new HashMap<FbxLimbNode, Integer>();

    private FbxSkeleton() {
    }
    
    public static void populateSkeletonData(FbxNode skeletonRoot) {
//        if (skeletonRoot instanceof FbxLimbNode) {
//            throw new UnsupportedOperationException("Limb node cannot be a skeleton root");
//        }
//        
//        FbxSkeleton skeleton = new FbxSkeleton();
//        skeleton.scanLimbs(skeletonRoot);
//        skeletonRoot.setFbxSkeleton(skeleton);
    }
    
    private void scanLimbs(FbxNode skeletonRoot, FbxLimbNode limb, List<FbxLimbNode> limbList) {
//        limb.skeletonRoot = skeletonRoot;
//        limbList.add(limb);
//        for (FbxNode child : limb.getChildren()) {
//            if (child instanceof FbxLimbNode) {
//                FbxLimbNode childLimb = (FbxLimbNode) child;
//                scanLimbs(skeletonRoot, childLimb, limbList);
//            }
//        }
    }
    
    private void scanLimbs(FbxNode skeletonRoot) {
        List<FbxLimbNode> limbList = new ArrayList<FbxLimbNode>();
        List<FbxLimbNode> rootList = new ArrayList<FbxLimbNode>();
        
        for (FbxNode child : skeletonRoot.getChildren()) {
            if (child instanceof FbxLimbNode) {
                FbxLimbNode limb = (FbxLimbNode) child;
                rootList.add(limb);
                scanLimbs(skeletonRoot, limb, limbList);
            }
        }
        
        allLimbs = limbList.toArray(new FbxLimbNode[0]);
        rootLimbs = rootList.toArray(new FbxLimbNode[0]);
        
        for (int i = 0; i < allLimbs.length; i++) {
            limbToIndexMap.put(allLimbs[i], i);
        }
    }
    
    public int getLimbIndex(FbxLimbNode limbNode) {
        return limbToIndexMap.get(limbNode);
    }
    
    public FbxLimbNode getLimb(int index) {
        return allLimbs[index];
    }
    
    public FbxLimbNode[] getRootLimbs() {
        return rootLimbs;
    }
    
}
