/*
 * Copyright (c) 2024 jMonkeyEngine
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
import com.jme3.scene.Spatial;

import java.io.IOException;

/**
 * Mask that excludes joints from participating in the layer
 * if a higher layer is using those joints in an animation.
 * 
 * @author codex
 */
public class SingleLayerInfluenceMask extends ArmatureMask {
    
    private String layer;
    private AnimComposer anim;

    /**
     * Serialization only.
     */
    public SingleLayerInfluenceMask() {}

    /**
     * @param layer The layer this mask is targeted for. It is important
     * that this match the name of the layer this mask is (or will be) part of. You
     * can use {@link #makeLayer(AnimComposer)} to ensure this.
     */
    public SingleLayerInfluenceMask(String layer) {
        super();
        this.layer = layer;
    }
    
    /**
     * Makes a layer from this mask.
     *
     * @param anim AnimComposer to use
     * @return this instance
     */
    public SingleLayerInfluenceMask makeLayer(AnimComposer anim) {
        this.anim = anim;
        if (this.anim != null) {
            anim.makeLayer(layer, this);
        }
        return this;
    }

    /**
     * Makes a layer from this mask.
     *
     * @return this instance
     */
    public SingleLayerInfluenceMask makeLayer() {
        anim.makeLayer(layer, this);
        return this;
    }
    
    /**
     * Adds all joints to this mask.
     *
     * @param armature
     * @return this instance
     */
    public SingleLayerInfluenceMask addAll(Armature armature) {
        for (Joint j : armature.getJointList()) {
            super.addBones(armature, j.getName());
        }
        return this;
    }
    
    /**
     * Adds the given joints to this mask.
     *
     * @param joints
     * @return this instance
     */
    public SingleLayerInfluenceMask addJoints(Armature armature, String... joints) {
        super.addBones(armature, joints);
        return this;
    }

    /**
     * Sets the AnimComposer used by this mask to determine joint use.
     * <p>If null, joint use check will be skipped and any higher that may
     * have been using the joint may be overriden.</p>
     *
     * @param anim
     */
    public void setAnimComposer(AnimComposer anim) {
        this.anim = anim;
    }

    /**
     * Get the layer this mask is targeted for.
     * <p>It is extremely important that this value match the actual layer
     * this is included in, because checking upper layers may not work if
     * they are different.
     * @return target layer
     */
    public String getTargetLayer() {
        return layer;
    }

    /**
     * Get the {@link AnimComposer} this mask is for.
     * @return anim composer
     */
    public AnimComposer getAnimComposer() {
        return anim;
    }
    
    @Override
    public boolean contains(Object target) {
        return simpleContains(target) && (anim == null || !isAffectedByUpperLayers(target));
    }

    private boolean simpleContains(Object target) {
        return super.contains(target);
    }
    
    private boolean isAffectedByUpperLayers(Object target) {
        boolean higher = false;
        for (String name : anim.getLayerNames()) {
            if (name.equals(layer)) {
                higher = true;
                continue;
            }
            if (!higher) {
                continue;
            }
            AnimLayer lyr = anim.getLayer(name);  
            // if there is no action playing, no joints are used, so we can skip
            if (lyr.getCurrentAction() == null) continue;
            if (lyr.getMask() instanceof SingleLayerInfluenceMask) {
                // dodge some needless recursion by calling a simpler method
                if (((SingleLayerInfluenceMask)lyr.getMask()).simpleContains(target)) {
                    return true;
                }
            }
            else if (lyr.getMask().contains(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(layer, "layer", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        layer = in.readString("layer", null);
    }
    
    /**
     * Creates an {@code SingleLayerInfluenceMask} for all joints.
     * @param layer layer the returned mask is, or will be, assigned to
     * @param anim anim composer
     * @param armature armature
     * @return new mask
     */
    public static SingleLayerInfluenceMask all(String layer, AnimComposer anim, Armature armature) {
        return new SingleLayerInfluenceMask(layer).makeLayer(anim).addAll(armature);
    }
    
}

