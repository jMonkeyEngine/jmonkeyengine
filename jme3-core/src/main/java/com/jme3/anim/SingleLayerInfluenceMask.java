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

import com.jme3.scene.Spatial;

/**
 * Mask that excludes joints from participating in the layer
 * if a higher layer is using those joints in an animation.
 * 
 * @author codex
 */
public class SingleLayerInfluenceMask extends ArmatureMask {
    
    private final String layer;
    private final AnimComposer anim;
    private final SkinningControl skin;
    private boolean checkUpperLayers = true;
    
    /**
     * @param layer The layer this mask is targeted for. It is important
     * that this match the name of the layer this mask is (or will be) part of. You
     * can use {@link makeLayer} to ensure this.
     * @param spatial Spatial containing necessary controls ({@link AnimComposer} and {@link SkinningControl})
     */
    public SingleLayerInfluenceMask(String layer, Spatial spatial) {
        super();
        this.layer = layer;
        anim = spatial.getControl(AnimComposer.class);
        skin = spatial.getControl(SkinningControl.class);
    }
    /**
     * @param layer The layer this mask is targeted for. It is important
     * that this match the name of the layer this mask is (or will be) part of. You
     * can use {@link makeLayer} to ensure this.
     * @param anim anim composer this mask is assigned to
     * @param skin skinning control complimenting the anim composer.
     */
    public SingleLayerInfluenceMask(String layer, AnimComposer anim, SkinningControl skin) {
        super();
        this.layer = layer;
        this.anim = anim;
        this.skin = skin;
    }
    
    /**
     * Makes a layer from this mask.
     */
    public void makeLayer() {
        anim.makeLayer(layer, this);
    }
    
    /**
     * Adds all joints to this mask.
     * @return this.instance
     */
    public SingleLayerInfluenceMask addAll() {
        for (Joint j : skin.getArmature().getJointList()) {
            super.addBones(skin.getArmature(), j.getName());
        }
        return this;
    }
    
    /**
     * Adds the given joint and all its children to this mask.
     * @param joint
     * @return this instance
     */
    public SingleLayerInfluenceMask addFromJoint(String joint) {
        super.addFromJoint(skin.getArmature(), joint);
        return this;
    }
    
    /**
     * Adds the given joints to this mask.
     * @param joints
     * @return this instance
     */
    public SingleLayerInfluenceMask addJoints(String... joints) {
        super.addBones(skin.getArmature(), joints);
        return this;
    }
    
    /**
     * Makes this mask check if each joint is being used by a higher layer
     * before it uses them.
     * <p>Not checking is more efficient, but checking can avoid some
     * interpolation issues between layers. Default=true
     * @param check 
     * @return this instance
     */
    public SingleLayerInfluenceMask setCheckUpperLayers(boolean check) {
        checkUpperLayers = check;
        return this;
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
    
    /**
     * Get the {@link SkinningControl} this mask is for.
     * @return skinning control
     */
    public SkinningControl getSkinningControl() {
        return skin;
    }
    
    /**
     * Returns true if this mask is checking upper layers for joint use.
     * @return 
     */
    public boolean isCheckUpperLayers() {
        return checkUpperLayers;
    }
    
    @Override
    public boolean contains(Object target) {
        return simpleContains(target) && (!checkUpperLayers || !isAffectedByUpperLayers(target));
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
    
    /**
     * Creates an {@code SingleLayerInfluenceMask} for all joints.
     * @param layer layer the returned mask is, or will be, be assigned to
     * @param spatial spatial containing anim composer and skinning control
     * @return new mask
     */
    public static SingleLayerInfluenceMask all(String layer, Spatial spatial) {
        return new SingleLayerInfluenceMask(layer, spatial).addAll();
    }
    
    /**
     * Creates an {@code SingleLayerInfluenceMask} for all joints.
     * @param layer layer the returned mask is, or will be, assigned to
     * @param anim anim composer
     * @param skin skinning control
     * @return new mask
     */
    public static SingleLayerInfluenceMask all(String layer, AnimComposer anim, SkinningControl skin) {
        return new SingleLayerInfluenceMask(layer, anim, skin).addAll();
    }
    
}

