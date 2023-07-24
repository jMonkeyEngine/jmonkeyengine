/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.anim;

import com.jme3.scene.Spatial;
import java.util.Iterator;

/**
 * Extension of {@link ArmatureMask}.
 * 
 * <p>Provides a feature which checks higher layers for joint use before it
 * approves the layer to use a joint.
 * 
 * @author codex
 */
public class AlertArmatureMask extends ArmatureMask {
    
    private final String layer;
    private final AnimComposer anim;
    private final SkinningControl skin;
    private boolean checkUpperLayers = true;
    
    /**
     * @param layer The layer this mask is targeted for. It is extremely important
     * that this match the name of the layer this mask is (or will be) part of. You
     * can use {@link makeLayer} to ensure this.
     * @param spatial Spatial containing necessary controls ({@link AnimComposer} and {@link SkinningControl})
     */
    public AlertArmatureMask(String layer, Spatial spatial) {
        super();
        this.layer = layer;
        anim = spatial.getControl(AnimComposer.class);
        skin = spatial.getControl(SkinningControl.class);
    }
    public AlertArmatureMask(String layer, AnimComposer anim, SkinningControl skin) {
        super();
        this.layer = layer;
        this.anim = anim;
        this.skin = skin;
    }
    
    /**
     * Creates a copyFor of this {@code AlertArmatureMask} for the given layer.
     * @param layer
     * @return copyFor of this {@code AlertArmatureMask} for the layer
     */
    public AlertArmatureMask copyFor(String layer) {
        return new AlertArmatureMask(layer, anim, skin);
    }
    /**
     * Makes a layer for this mask.
     */
    public void makeLayer() {
        anim.makeLayer(layer, this);
    }
    
    /**
     * Adds all joints to this mask.
     * @return 
     */
    public AlertArmatureMask addAll() {
        for (Joint j : skin.getArmature().getJointList()) {
            super.addBones(skin.getArmature(), j.getName());
        }
        return this;
    }
    /**
     * Adds the given joint and all its children to this mask.
     * @param joint
     * @return 
     */
    public AlertArmatureMask addFromJoint(String joint) {
        super.addFromJoint(skin.getArmature(), joint);
        return this;
    }
    /**
     * Adds the given joints to this mask.
     * @param joints
     * @return 
     */
    public AlertArmatureMask addJoints(String... joints) {
        super.addBones(skin.getArmature(), joints);
        return this;
    }
    
    /**
     * Makes this mask check if each joint is being used by a higher layer
     * before it uses them.
     * <p>Not checking is more efficient, but checking can avoid some
     * interpolation issues between layers. Default=true
     * @param check 
     */
    public void setCheckUpperLayers(boolean check) {
        checkUpperLayers = check;
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
     * @return 
     */
    public AnimComposer getAnimComposer() {
        return anim;
    }
    /**
     * Get the {@link SkinningControl} this mask is for.
     * @return 
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
        /**
         * ... Since AnimComposer does not provide a Collection that
         * has a decending iterator, we will just have to skip over
         * a bunch of lower layers before we actually need to do anything.
         */
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
            if (lyr.getMask() instanceof AlertArmatureMask) {
                // dodge some needless recursion by calling a simpler method
                if (((AlertArmatureMask)lyr.getMask()).simpleContains(target)) {
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
     * Creates an {@code AlertArmatureMask} for all joints.
     * @param layer
     * @param spatial
     * @return 
     */
    public static AlertArmatureMask all(String layer, Spatial spatial) {
        return new AlertArmatureMask(layer, spatial).addAll();
    }
    /**
     * Creates an {@code AlertArmatureMask} for all joints.
     * @param layer
     * @param anim
     * @param skin
     * @return 
     */
    public static AlertArmatureMask all(String layer, AnimComposer anim, SkinningControl skin) {
        return new AlertArmatureMask(layer, anim, skin).addAll();
    }
    
}

