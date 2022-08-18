/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.*;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.*;

/**
 * AnimComposer is a Spatial control that allows manipulation of
 * {@link Armature armature} (skeletal) animation.
 *
 * @author Nehon
 */
public class AnimComposer extends AbstractControl {
    /**
     * The name of the default layer.
     */
    public static final String DEFAULT_LAYER = "Default";
    private Map<String, AnimClip> animClipMap = new HashMap<>();

    private Map<String, Action> actions = new HashMap<>();
    private float globalSpeed = 1f;
    private Map<String, AnimLayer> layers = new LinkedHashMap<>(4);

    /**
     * Instantiate a composer with a single layer, no actions, and no clips.
     */
    public AnimComposer() {
        layers.put(DEFAULT_LAYER, new AnimLayer(this, DEFAULT_LAYER, null));
    }

    /**
     * Tells if an animation is contained in the list of animations.
     *
     * @param name The name of the animation.
     * @return true, if the named animation is in the list of animations.
     */
    public boolean hasAnimClip(String name) {
        return animClipMap.containsKey(name);
    }

    /**
     * Retrieve an animation from the list of animations.
     *
     * @param name The name of the animation to retrieve.
     * @return The animation corresponding to the given name, or null, if no
     * such named animation exists.
     */
    public AnimClip getAnimClip(String name) {
        return animClipMap.get(name);
    }

    /**
     * Adds an animation to be available for playing to this
     * <code>AnimControl</code>.
     *
     * @param anim The animation to add.
     */
    public void addAnimClip(AnimClip anim) {
        animClipMap.put(anim.getName(), anim);
    }

    /**
     * Remove an animation so that it is no longer available for playing.
     *
     * @param anim The animation to remove.
     */
    public void removeAnimClip(AnimClip anim) {
        if (!animClipMap.containsKey(anim.getName())) {
            throw new IllegalArgumentException("Given animation does not exist "
                    + "in this AnimControl");
        }

        animClipMap.remove(anim.getName());
    }

    /**
     * Run an action on the default layer. By default action will loop.
     *
     * @param name The name of the action to run.
     * @return The action corresponding to the given name.
     */
    public Action setCurrentAction(String name) {
        return setCurrentAction(name, DEFAULT_LAYER);
    }

    /**
     * Run an action on specified layer. By default action will loop.
     *
     * @param actionName The name of the action to run.
     * @param layerName The layer on which action should run.
     * @return The action corresponding to the given name.
     */
    public Action setCurrentAction(String actionName, String layerName) {
        return setCurrentAction(actionName, layerName, true);
    }

    /**
     * Run an action on specified layer.
     *
     * @param actionName The name of the action to run.
     * @param layerName The layer on which action should run.
     * @param loop True if the action must loop.
     * @return The action corresponding to the given name.
     */
    public Action setCurrentAction(String actionName, String layerName, boolean loop) {
        AnimLayer l = getLayer(layerName);
        Action currentAction = action(actionName);
        l.setCurrentAction(actionName, currentAction, loop);

        return currentAction;
    }

    /**
     * Return the current action on the default layer.
     *
     * @return The action corresponding to the given name.
     */
    public Action getCurrentAction() {
        return getCurrentAction(DEFAULT_LAYER);
    }

    /**
     * Return current action on specified layer.
     *
     * @param layerName The layer on which action should run.
     * @return The action corresponding to the given name.
     */
    public Action getCurrentAction(String layerName) {
        AnimLayer l = getLayer(layerName);
        Action result = l.getCurrentAction();

        return result;
    }

    /**
     * Remove current action on default layer.
     */
    public void removeCurrentAction() {
        removeCurrentAction(DEFAULT_LAYER);
    }

    /**
     * Remove current action on specified layer.
     *
     * @param layerName The name of the layer we want to remove its action.
     */
    public void removeCurrentAction(String layerName) {
        AnimLayer l = getLayer(layerName);
        l.setCurrentAction(null);
    }

    /**
     * Returns current time of the default layer.
     *
     * @return The current time.
     */
    public double getTime() {
        return getTime(DEFAULT_LAYER);
    }

    /**
     * Returns current time of the specified layer.
     *
     * @param layerName The layer from which to get the time.
     * @return the time (in seconds)
     */
    public double getTime(String layerName) {
        AnimLayer l = getLayer(layerName);
        double result = l.getTime();

        return result;
    }

    /**
     * Sets current time on the default layer.
     *
     * @param time the desired time (in seconds)
     */
    public void setTime(double time) {
        setTime(DEFAULT_LAYER, time);
    }

    /**
     * Sets current time on the specified layer.
     *
     * @param layerName the name of the Layer to modify
     * @param time the desired time (in seconds)
     */
    public void setTime(String layerName, double time) {
        AnimLayer l = getLayer(layerName);
        if (l.getCurrentAction() == null) {
            throw new RuntimeException("There is no action running in layer " + layerName);
        }

        l.setTime(time);
    }

    /**
     *
     * @param name The name of the action to return.
     * @return The action registered with specified name. It will make a new action if there isn't any.
     * @see #makeAction(java.lang.String)
     */
    public Action action(String name) {
        Action action = actions.get(name);
        if (action == null) {
            action = makeAction(name);
            actions.put(name, action);
        }
        return action;
    }

    /**
     *
     * @param name The name of the action to return.
     * @return The action registered with specified name or null if nothing is registered.
     */
    public Action getAction(String name) {
        return actions.get(name);
    }

    /**
     * Register given action with specified name.
     *
     * @param name The name of the action.
     * @param action The action to add.
     */
    public void addAction(String name, Action action) {
        actions.put(name, action);
    }

    /**
     * Create a new ClipAction with specified clip name.
     *
     * @param name The name of the clip.
     * @return a new action
     * @throws IllegalArgumentException if clip with specified name not found.
     */
    public Action makeAction(String name) {
        Action action;
        AnimClip clip = animClipMap.get(name);
        if (clip == null) {
            throw new IllegalArgumentException("Cannot find clip named " + name);
        }
        action = new ClipAction(clip);
        return action;
    }

    /**
     * Tells if an action is contained in the list of actions.
     *
     * @param name The name of the action.
     * @return true, if the named action is in the list of actions.
     */
    public boolean hasAction(String name) {
        return actions.containsKey(name);
    }

    /**
     * Remove specified action.
     *
     * @param name The name of the action to remove.
     * @return The removed action.
     */
    public Action removeAction(String name) {
        return actions.remove(name);
    }

    /**
     * Add a layer to this composer.
     *
     * @param name the desired name for the new layer
     * @param mask the desired mask for the new layer (alias created)
     */
    public void makeLayer(String name, AnimationMask mask) {
        AnimLayer l = new AnimLayer(this, name, mask);
        layers.put(name, l);
    }

    /**
     * Remove specified layer. This will stop the current action on this layer.
     *
     * @param name The name of the layer to remove.
     */
    public void removeLayer(String name) {
        layers.remove(name);
    }

    /**
     * Creates an action that will interpolate over an entire sequence
     * of tweens in order.
     *
     * @param name a name for the new Action
     * @param tweens the desired sequence of tweens
     * @return a new instance
     */
    public BaseAction actionSequence(String name, Tween... tweens) {
        BaseAction action = new BaseAction(Tweens.sequence(tweens));
        actions.put(name, action);
        return action;
    }

    /**
     * Creates an action that blends the named clips using the given blend
     * space.
     *
     * @param name a name for the new Action
     * @param blendSpace how to blend the clips (not null, alias created)
     * @param clips the names of the clips to be used (not null)
     * @return a new instance
     */
    public BlendAction actionBlended(String name, BlendSpace blendSpace, String... clips) {
        BlendableAction[] acts = new BlendableAction[clips.length];
        for (int i = 0; i < acts.length; i++) {
            BlendableAction ba = (BlendableAction) makeAction(clips[i]);
            acts[i] = ba;
        }
        BlendAction action = new BlendAction(blendSpace, acts);
        actions.put(name, action);
        return action;
    }

    /**
     * Reset all layers to t=0 with no current action.
     */
    public void reset() {
        for (AnimLayer layer : layers.values()) {
            layer.setCurrentAction(null);
        }
    }

    /**
     * Returns an unmodifiable collection of all available animations. When an attempt
     * is made to modify the collection, an UnsupportedOperationException is thrown.
     *
     * @return the unmodifiable collection of animations
     */
    public Collection<AnimClip> getAnimClips() {
        return Collections.unmodifiableCollection(animClipMap.values());
    }

    /**
     * Returns an unmodifiable set of all available animation names. When an
     * attempt is made to modify the set, an UnsupportedOperationException is
     * thrown.
     *
     * @return the unmodifiable set of animation names.
     */
    public Set<String> getAnimClipsNames() {
        return Collections.unmodifiableSet(animClipMap.keySet());
    }

    /**
     * used internally
     *
     * @param tpf time per frame (in seconds)
     */
    @Override
    protected void controlUpdate(float tpf) {
        for (AnimLayer layer : layers.values()) {
            layer.update(tpf);
        }
    }

    /**
     * used internally
     *
     * @param rm the RenderManager rendering the controlled Spatial (not null)
     * @param vp the ViewPort being rendered (not null)
     */
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    /**
     * Determine the global speed applied to all layers.
     *
     * @return the speed factor (1=normal speed)
     */
    public float getGlobalSpeed() {
        return globalSpeed;
    }

    /**
     * Alter the global speed applied to all layers.
     *
     * @param globalSpeed the desired speed factor (1=normal speed, default=1)
     */
    public void setGlobalSpeed(float globalSpeed) {
        this.globalSpeed = globalSpeed;
    }

    /**
     * Provides access to the named layer.
     *
     * @param layerName the name of the layer to access
     * @return the pre-existing instance
     */
    public AnimLayer getLayer(String layerName) {
        AnimLayer result = layers.get(layerName);
        if (result == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        return result;
    }

    /**
     * Access the manager of the named layer.
     *
     * @param layerName the name of the layer to access
     * @return the current manager (typically an AnimEvent) or null for none
     */
    public Object getLayerManager(String layerName) {
        AnimLayer layer = getLayer(layerName);
        Object result = layer.getManager();

        return result;
    }

    /**
     * Enumerates the names of all layers.
     *
     * @return an unmodifiable set of names
     */
    public Set<String> getLayerNames() {
        Set<String> result = Collections.unmodifiableSet(layers.keySet());
        return result;
    }

    /**
     * Assign a manager to the named layer.
     *
     * @param layerName the name of the layer to modify
     * @param manager the desired manager (typically an AnimEvent) or null for
     * none
     */
    public void setLayerManager(String layerName, Object manager) {
        AnimLayer layer = getLayer(layerName);
        layer.setManager(manager);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public Object jmeClone() {
        try {
            AnimComposer clone = (AnimComposer) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned composer into a deep-cloned one, using the specified
     * Cloner and original to resolve copied fields.
     *
     * @param cloner the Cloner that's cloning this composer (not null)
     * @param original the instance from which this composer was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        Map<String, AnimClip> clips = new HashMap<>();
        for (String key : animClipMap.keySet()) {
            clips.put(key, cloner.clone(animClipMap.get(key)));
        }
        Map<String, Action> act = new HashMap<>();
        for (String key : actions.keySet()) {
            act.put(key, cloner.clone(actions.get(key)));
        }
        actions = act;
        animClipMap = clips;

        Map<String, AnimLayer> newLayers = new LinkedHashMap<>();
        for (String key : layers.keySet()) {
            newLayers.put(key, cloner.clone(layers.get(key)));
        }

        layers = newLayers;

    }

    /**
     * De-serialize this composer from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        animClipMap = (Map<String, AnimClip>) ic.readStringSavableMap("animClipMap", new HashMap<String, AnimClip>());
        globalSpeed = ic.readFloat("globalSpeed", 1f);
    }

    /**
     * Serialize this composer to the specified exporter, for example when
     * saving to a J3O file.
     *
     * @param ex the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeStringSavableMap(animClipMap, "animClipMap", new HashMap<String, AnimClip>());
        oc.write(globalSpeed, "globalSpeed", 1f);
    }
}
