/*
 * Copyright (c) 2025 jMonkeyEngine
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
package com.jme3.anim;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * Mask that excludes joints from participating in the layer if a higher layer
 * is using those joints in an animation.
 *
 * @author codex
 */
public class SingleLayerInfluenceMask extends ArmatureMask {

    private String targetLayer;
    private AnimComposer animComposer;
    
    /**
     * For serialization only. Do not use
     */
    protected SingleLayerInfluenceMask() {
    }
    
    /**
     * Instantiate a mask that affects all joints in the specified Armature.
     *
     * @param targetLayer  The layer this mask is targeted for.
     * @param animComposer The animation composer associated with this mask.
     * @param armature     The Armature containing the joints.
     */
    public SingleLayerInfluenceMask(String targetLayer, AnimComposer animComposer, Armature armature) {
        super(armature);
        this.targetLayer = targetLayer;
        this.animComposer = animComposer;
    }

    /**
     * Get the layer this mask is targeted for.
     *
     * @return The target layer
     */
    public String getTargetLayer() {
        return targetLayer;
    }

    /**
     * Sets the animation composer for this mask.
     *
     * @param animComposer The new animation composer.
     */
    public void setAnimComposer(AnimComposer animComposer) {
        this.animComposer = animComposer;
    }

    /**
     * Checks if the specified target is contained within this mask.
     *
     * @param target The target to check.
     * @return True if the target is contained within this mask, false otherwise.
     */
    @Override
    public boolean contains(Object target) {
        return simpleContains(target) && (animComposer == null || !isAffectedByUpperLayers(target));
    }

    private boolean simpleContains(Object target) {
        return super.contains(target);
    }

    private boolean isAffectedByUpperLayers(Object target) {
        boolean higher = false;
        for (String layerName : animComposer.getLayerNames()) {
            if (layerName.equals(targetLayer)) {
                higher = true;
                continue;
            }
            if (!higher) {
                continue;
            }
            
            AnimLayer animLayer = animComposer.getLayer(layerName);
            if (animLayer.getCurrentAction() != null) {
                AnimationMask mask = animLayer.getMask();
                
                if (mask instanceof SingleLayerInfluenceMask) {
                    // dodge some needless recursion by calling a simpler method
                    if (((SingleLayerInfluenceMask) mask).simpleContains(target)) {
                        return true;
                    }
                } else if (mask != null && mask.contains(target)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(targetLayer, "targetLayer", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        targetLayer = ic.readString("targetLayer", null);
    }

}