package com.jme3.anim;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.*;
import com.jme3.export.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.*;

/**
 * Created by Nehon on 20/12/2017.
 */
public class AnimComposer extends AbstractControl {

    public static final String DEFAULT_LAYER = "Default";
    private Map<String, AnimClip> animClipMap = new HashMap<>();

    private Map<String, Action> actions = new HashMap<>();
    private float globalSpeed = 1f;
    private Map<String, Layer> layers = new LinkedHashMap<>();

    public AnimComposer() {
        layers.put(DEFAULT_LAYER, new Layer());
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

    public Action setCurrentAction(String name) {
        return setCurrentAction(name, DEFAULT_LAYER);
    }

    public Action setCurrentAction(String actionName, String layerName) {
        Layer l = layers.get(layerName);
        if (l == null) {
            throw new IllegalArgumentException("Unknown layer " + layerName);
        }
        Action currentAction = action(actionName);
        l.time = 0;
        l.currentAction = currentAction;
        return currentAction;
    }

    public Action action(String name) {
        Action action = actions.get(name);
        if (action == null) {
            action = makeAction(name);
            actions.put(name, action);
        }
        return action;
    }

    public Action makeAction(String name) {
        Action action;
        AnimClip clip = animClipMap.get(name);
        if (clip == null) {
            throw new IllegalArgumentException("Cannot find clip named " + name);
        }
        action = new ClipAction(clip);
        return action;
    }

    public void makeLayer(String name, AnimationMask mask){
        Layer l = new Layer();
        l.mask = mask;
        layers.put(name, l);
    }


    public BaseAction actionSequence(String name, Tween... tweens) {
        BaseAction action = new BaseAction(Tweens.sequence(tweens));
        actions.put(name, action);
        return action;
    }

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

    public void reset() {
        for (Layer layer : layers.values()) {
            layer.currentAction = null;
            layer.time = 0;
        }
    }

    public Collection<AnimClip> getAnimClips() {
        return Collections.unmodifiableCollection(animClipMap.values());
    }

    public Collection<String> getAnimClipsNames() {
        return Collections.unmodifiableCollection(animClipMap.keySet());
    }

    @Override
    protected void controlUpdate(float tpf) {
        for (Layer layer : layers.values()) {
            Action currentAction = layer.currentAction;
            if (currentAction == null) {
                continue;
            }
            layer.advance(tpf);

            currentAction.setMask(layer.mask);
            boolean running = currentAction.interpolate(layer.time);
            currentAction.setMask(null);

            if (!running) {
                layer.time = 0;
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    public float getGlobalSpeed() {
        return globalSpeed;
    }

    public void setGlobalSpeed(float globalSpeed) {
        this.globalSpeed = globalSpeed;
    }

    @Override
    public Object jmeClone() {
        try {
            AnimComposer clone = (AnimComposer) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

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

        Map<String, Layer> newLayers = new LinkedHashMap<>();
        for (String key : layers.keySet()) {
            newLayers.put(key, cloner.clone(layers.get(key)));
        }

        layers = newLayers;

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        animClipMap = (Map<String, AnimClip>) ic.readStringSavableMap("animClipMap", new HashMap<String, AnimClip>());
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeStringSavableMap(animClipMap, "animClipMap", new HashMap<String, AnimClip>());
    }

    private class Layer implements JmeCloneable {
        private Action currentAction;
        private AnimationMask mask;
        private float weight;
        private double time;

        public void advance(float tpf) {
            time += tpf * currentAction.getSpeed() * globalSpeed;
            // make sure negative time is in [0, length] range
            if (time < 0) {
                double length = currentAction.getLength();
                time = (time % length + length) % length;
            }

        }

        @Override
        public Object jmeClone() {
            try {
                Layer clone = (Layer) super.clone();
                return clone;
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        @Override
        public void cloneFields(Cloner cloner, Object original) {
            currentAction = null;
        }
    }
}
