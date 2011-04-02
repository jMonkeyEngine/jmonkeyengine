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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
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
public final class AnimControl extends AbstractControl implements Savable, Cloneable {

    /**
     * List of targets which this controller effects.
     */
    //  Mesh[] targets;
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
    HashMap<String, BoneAnimation> animationMap;
    /**
     * Animation channels
     */
    transient ArrayList<AnimChannel> channels = new ArrayList<AnimChannel>();
    transient ArrayList<AnimEventListener> listeners = new ArrayList<AnimEventListener>();

    /**
     * Create a new <code>AnimControl</code> that will animate the given skins
     * using the skeleton and provided animations.
     *
     * @param model The root node of all the skins specified in
     * <code>meshes</code> argument.
     * @param meshes The skins, or meshes, to animate. Should have
     * properly set BoneIndex and BoneWeight buffers.
     * @param skeleton The skeleton structure represents a bone hierarchy
     * to be animated.
     * @deprecated AnimControl doesnt' hande the skinning anymore, use AnimControl(Skeleton skeleton);
     * Then create a SkeletonControl(Node model, Mesh[] meshes, Skeleton skeleton);
     * and add it to the spatial.
     */
    @Deprecated
    public AnimControl(Node model, Mesh[] meshes, Skeleton skeleton) {
        super(model);

        this.skeleton = skeleton;

        skeletonControl = new SkeletonControl(model, meshes, this.skeleton);
        reset();
    }

    public AnimControl(Skeleton skeleton) {
        this.skeleton = skeleton;
        reset();
    }

    /**
     * Used only for Saving/Loading models (all parameters of the non-default
     * constructor are restored from the saved model, but the object must be
     * constructed beforehand)
     */
    public AnimControl() {
    }

    public Control cloneForSpatial(Spatial spatial) {
        try {
            AnimControl clone = (AnimControl) super.clone();
            clone.spatial = spatial;
            clone.skeleton = new Skeleton(skeleton);
            clone.channels = new ArrayList<AnimChannel>();
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
    public void setAnimations(HashMap<String, BoneAnimation> animations) {
        animationMap = animations;
    }

    /**
     * Retrieve an animation from the list of animations.
     * @param name The name of the animation to retrieve.
     * @return The animation corresponding to the given name, or null, if no
     * such named animation exists.
     */
    public BoneAnimation getAnim(String name) {
        return animationMap.get(name);
    }

    /**
     * Adds an animation to be available for playing to this
     * <code>AnimControl</code>.
     * @param anim The animation to add.
     */
    public void addAnim(BoneAnimation anim) {
        animationMap.put(anim.getName(), anim);
    }

    /**
     * Remove an animation so that it is no longer available for playing.
     * @param anim The animation to remove.
     */
    public void removeAnim(BoneAnimation anim) {
        if (!animationMap.containsKey(anim.getName())) {
            throw new IllegalArgumentException("Given animation does not exist "
                    + "in this AnimControl");
        }

        animationMap.remove(anim.getName());
    }

    /**
     * 
     * @param boneName the name of the bone
     * @return the node attached to this bone
     * @deprecated use SkeletonControl.getAttachementNode instead.
     */
    @Deprecated
    public Node getAttachmentsNode(String boneName) {
        Bone b = skeleton.getBone(boneName);
        if (b == null) {
            throw new IllegalArgumentException("Given bone name does not exist "
                    + "in the skeleton.");
        }

        Node n = b.getAttachmentsNode();
        if (spatial != null) {
            Node model = (Node) spatial;
            model.attachChild(n);
        }
        return n;
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
     * @return The targets, or skins, being influenced by this
     * <code>AnimControl</code>.
     * @deprecated use SkeletonControl.getTargets() instead
     * get the SkeletonControl doing spatial.getControl(SkeletonControl.class);
     */
    @Deprecated
    public Mesh[] getTargets() {
        return skeletonControl.getTargets();
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

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        //Backward compatibility.
        if (skeletonControl != null) {
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
        BoneAnimation a = animationMap.get(name);
        if (a == null) {
            throw new IllegalArgumentException("The animation " + name
                    + " does not exist in this AnimControl");
        }

        return a.getLength();
    }

    @Override
    protected void controlUpdate(float tpf) {
        skeleton.reset(); // reset skeleton to bind pose

        for (int i = 0; i < channels.size(); i++) {
            channels.get(i).update(tpf);
        }

        skeleton.updateWorldVectors();
    }

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
        animationMap = (HashMap<String, BoneAnimation>) in.readStringSavableMap("animations", null);


        //changed for backward compatibility with j3o files generated before the AnimControl/SkeletonControl split
        //if we find a target mesh array the AnimControl creates the SkeletonControl for old files and add it to the spatial.        
        //When backward compatibility won't be needed anymore this can deleted        
        Savable[] sav = in.readSavableArray("targets", null);
        if (sav != null) {
            Mesh[] tg = null;
            tg = new Mesh[sav.length];
            System.arraycopy(sav, 0, tg, 0, sav.length);
            skeletonControl = new SkeletonControl((Node) spatial, tg, skeleton);
            spatial.addControl(skeletonControl);
        }
        //------

    }
}
