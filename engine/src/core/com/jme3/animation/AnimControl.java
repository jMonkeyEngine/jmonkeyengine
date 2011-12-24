/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.animation;

import com.jme3.export.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * <code>AnimControl</code> is a Spatial control that allows manipulation
 * of skeletal animation.
 *
 * The control currently supports:
 * 1) Animation blending/transitions
 * 2) Multiple animation channels
 * 3) Multiple skins
 * 4) Animation event listeners
 * 5) Animated model cloning
 * 6) Animated model binary import/export
 *
 * Planned:
 * 1) Hardware skinning
 * 2) Morph/Pose animation
 * 3) Attachments
 * 4) Add/remove skins
 *
 * @author Kirill Vainer
 */
public final class AnimControl extends AbstractControl implements Cloneable {

    /**
     * Skeleton object must contain corresponding data for the targets' weight buffers.
     */
    Skeleton skeleton;
    /** only used for backward compatibility */
    @Deprecated
    private SkeletonControl skeletonControl;
    /**
     * List of animations
     */
    HashMap<String, Animation> animationMap;
    /**
     * Animation channels
     */
    private transient ArrayList<AnimChannel> channels = new ArrayList<AnimChannel>();
    /**
     * Animation event listeners
     */
    private transient ArrayList<AnimEventListener> listeners = new ArrayList<AnimEventListener>();

    /**
     * Creates a new animation control for the given skeleton.
     * The method {@link AnimControl#setAnimations(java.util.HashMap) }
     * must be called after initialization in order for this class to be useful.
     *
     * @param skeleton The skeleton to animate
     */
    public AnimControl(Skeleton skeleton) {
        this.skeleton = skeleton;
        reset();
    }

    /**
     * Serialization only. Do not use.
     */
    public AnimControl() {
    }

    /**
     * Internal use only.
     */
    public Control cloneForSpatial(Spatial spatial) {
        try {
            AnimControl clone = (AnimControl) super.clone();
            clone.spatial = spatial;
            clone.channels = new ArrayList<AnimChannel>();
            clone.listeners = new ArrayList<AnimEventListener>();

            if (skeleton != null) {
                clone.skeleton = new Skeleton(skeleton);
            }

            // animationMap is reference-copied, animation data should be shared
            // to reduce memory usage.

            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * @param animations Set the animations that this <code>AnimControl</code>
     * will be capable of playing. The animations should be compatible
     * with the skeleton given in the constructor.
     */
    public void setAnimations(HashMap<String, Animation> animations) {
        animationMap = animations;
    }

    /**
     * Retrieve an animation from the list of animations.
     * @param name The name of the animation to retrieve.
     * @return The animation corresponding to the given name, or null, if no
     * such named animation exists.
     */
    public Animation getAnim(String name) {
        if (animationMap == null) {
            animationMap = new HashMap<String, Animation>();
        }
        return animationMap.get(name);
    }

    /**
     * Adds an animation to be available for playing to this
     * <code>AnimControl</code>.
     * @param anim The animation to add.
     */
    public void addAnim(Animation anim) {
        if (animationMap == null) {
            animationMap = new HashMap<String, Animation>();
        }
        animationMap.put(anim.getName(), anim);
    }

    /**
     * Remove an animation so that it is no longer available for playing.
     * @param anim The animation to remove.
     */
    public void removeAnim(Animation anim) {
        if (!animationMap.containsKey(anim.getName())) {
            throw new IllegalArgumentException("Given animation does not exist "
                    + "in this AnimControl");
        }

        animationMap.remove(anim.getName());
    }

    /**
     * Create a new animation channel, by default assigned to all bones
     * in the skeleton.
     * 
     * @return A new animation channel for this <code>AnimControl</code>.
     */
    public AnimChannel createChannel() {
        AnimChannel channel = new AnimChannel(this);
        channels.add(channel);
        return channel;
    }

    /**
     * Return the animation channel at the given index.
     * @param index The index, starting at 0, to retrieve the <code>AnimChannel</code>.
     * @return The animation channel at the given index, or throws an exception
     * if the index is out of bounds.
     *
     * @throws IndexOutOfBoundsException If no channel exists at the given index.
     */
    public AnimChannel getChannel(int index) {
        return channels.get(index);
    }

    /**
     * @return The number of channels that are controlled by this
     * <code>AnimControl</code>.
     *
     * @see AnimControl#createChannel()
     */
    public int getNumChannels() {
        return channels.size();
    }

    /**
     * Clears all the channels that were created.
     *
     * @see AnimControl#createChannel()
     */
    public void clearChannels() {
        channels.clear();
    }

    /**
     * @return The skeleton of this <code>AnimControl</code>.
     */
    public Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * Adds a new listener to receive animation related events.
     * @param listener The listener to add.
     */
    public void addListener(AnimEventListener listener) {
        if (listeners.contains(listener)) {
            throw new IllegalArgumentException("The given listener is already "
                    + "registed at this AnimControl");
        }

        listeners.add(listener);
    }

    /**
     * Removes the given listener from listening to events.
     * @param listener
     * @see AnimControl#addListener(com.jme3.animation.AnimEventListener)
     */
    public void removeListener(AnimEventListener listener) {
        if (!listeners.remove(listener)) {
            throw new IllegalArgumentException("The given listener is not "
                    + "registed at this AnimControl");
        }
    }

    /**
     * Clears all the listeners added to this <code>AnimControl</code>
     *
     * @see AnimControl#addListener(com.jme3.animation.AnimEventListener)
     */
    public void clearListeners() {
        listeners.clear();
    }

    void notifyAnimChange(AnimChannel channel, String name) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAnimChange(this, channel, name);
        }
    }

    void notifyAnimCycleDone(AnimChannel channel, String name) {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAnimCycleDone(this, channel, name);
        }
    }

    /**
     * Internal use only.
     */
    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial == null && skeletonControl != null) {
            this.spatial.removeControl(skeletonControl);
        }

        super.setSpatial(spatial);

        //Backward compatibility.
        if (spatial != null && skeletonControl != null) {
            spatial.addControl(skeletonControl);
        }
    }

    final void reset() {
        if (skeleton != null) {
            skeleton.resetAndUpdate();
        }
    }

    /**
     * @return The names of all animations that this <code>AnimControl</code>
     * can play.
     */
    public Collection<String> getAnimationNames() {
        return animationMap.keySet();
    }

    /**
     * Returns the length of the given named animation.
     * @param name The name of the animation
     * @return The length of time, in seconds, of the named animation.
     */
    public float getAnimationLength(String name) {
        Animation a = animationMap.get(name);
        if (a == null) {
            throw new IllegalArgumentException("The animation " + name
                    + " does not exist in this AnimControl");
        }

        return a.getLength();
    }

    /**
     * Internal use only.
     */
    @Override
    protected void controlUpdate(float tpf) {
        if (skeleton != null) {
            skeleton.reset(); // reset skeleton to bind pose
        }

        TempVars vars = TempVars.get();
        for (int i = 0; i < channels.size(); i++) {
            channels.get(i).update(tpf, vars);
        }
        vars.release();

        if (skeleton != null) {
            skeleton.updateWorldVectors();
        }
    }

    /**
     * Internal use only.
     */
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(skeleton, "skeleton", null);
        oc.writeStringSavableMap(animationMap, "animations", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        skeleton = (Skeleton) in.readSavable("skeleton", null);
        animationMap = (HashMap<String, Animation>) in.readStringSavableMap("animations", null);

        if (im.getFormatVersion() == 0) {
            // Changed for backward compatibility with j3o files generated 
            // before the AnimControl/SkeletonControl split.

            // If we find a target mesh array the AnimControl creates the 
            // SkeletonControl for old files and add it to the spatial.        
            // When backward compatibility won't be needed anymore this can deleted        
            Savable[] sav = in.readSavableArray("targets", null);
            if (sav != null) {
                Mesh[] targets = new Mesh[sav.length];
                System.arraycopy(sav, 0, targets, 0, sav.length);
                skeletonControl = new SkeletonControl(targets, skeleton);
                spatial.addControl(skeletonControl);
            }
        }
    }
}
