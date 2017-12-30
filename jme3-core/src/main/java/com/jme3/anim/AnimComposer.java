package com.jme3.anim;

import com.jme3.anim.tween.AnimClipTween;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.SequenceAction;
import com.jme3.export.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;

import java.io.IOException;
import java.util.*;

/**
 * Created by Nehon on 20/12/2017.
 */
public class AnimComposer extends AbstractControl {

    private Map<String, AnimClip> animClipMap = new HashMap<>();

    private Action currentAction;
    private Map<String, Action> actions = new HashMap<>();
    private float time;

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

    public void setCurrentAction(String name) {
        Action action = action(name);
        if (currentAction != null) {
            currentAction.reset();
        }
        currentAction = action;
        time = 0;
    }

    public Action action(String name) {
        Action action = actions.get(name);
        if (action == null) {
            AnimClipTween tween = tweenFromClip(name);
            action = new SequenceAction(tween);
            actions.put(name, action);
        }
        return action;
    }

    public AnimClipTween tweenFromClip(String clipName) {
        AnimClip clip = animClipMap.get(clipName);
        if (clip == null) {
            throw new IllegalArgumentException("Cannot find clip named " + clipName);
        }
        return new AnimClipTween(clip);
    }

    public SequenceAction actionSequence(String name, Tween... tweens) {
        SequenceAction action = new SequenceAction(tweens);
        actions.put(name, action);
        return action;
    }

    public Action actionBlended(String name, Tween... tweens) {
        return null;
    }

    public void reset() {
        currentAction = null;
        time = 0;
    }

    public Collection<AnimClip> getAnimClips() {
        return Collections.unmodifiableCollection(animClipMap.values());
    }

    public Collection<String> getAnimClipsNames() {
        return Collections.unmodifiableCollection(animClipMap.keySet());
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (currentAction != null) {
            time += tpf;
            boolean running = currentAction.interpolate(time);
            if (!running) {
                time -= currentAction.getLength();
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

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
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        animClipMap = (Map<String, AnimClip>) ic.readStringSavableMap("animClipMap", new HashMap<String, AnimClip>());
        actions = (Map<String, Action>) ic.readStringSavableMap("actions", new HashMap<String, Action>());
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeStringSavableMap(animClipMap, "animClipMap", new HashMap<String, AnimClip>());
        oc.writeStringSavableMap(actions, "actions", new HashMap<String, Action>());
    }
}
