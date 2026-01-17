/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.cinematic.events;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.action.Action;
import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.PlayState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CinematicEvent that plays a canned animation action in an
 * {@link com.jme3.anim.AnimComposer}.
 *
 * Inspired by Nehon's {@link AnimationEvent}.
 */
public class AnimEvent extends AbstractCinematicEvent {

    private static final Logger logger
            = Logger.getLogger(AnimEvent.class.getName());
			
    private static final String CINEMATIC_REF = "Cinematic:Ref";

    private static final AtomicLong spatialId = new AtomicLong();

    /**
     * Reference ID used to find the associated Spatial.
     */
    protected String spatialRef;
    /*
     * Control that will play the animation
     */
    private AnimComposer composer;
    /*
     * Cinematic that contains this event
     */
    private Cinematic cinematic;
    /*
     * name of the animation action to be played
     */
    private String actionName;
    /*
     * name of the animation layer on which the action will be played
     */
    private String layerName;
	
	/**
     * Constructs a new AnimEvent to play the named action on the default layer.
     *
     * @param composer the Control that will play the animation (not null).
     * @param actionName the name of the animation action to play.
     */
    public AnimEvent(AnimComposer composer, String actionName) {
        this(composer, actionName, AnimComposer.DEFAULT_LAYER);
    }

    /**
     * Instantiate a non-looping event to play the named action on the named
     * layer of the specified AnimComposer.
     *
     * @param composer the Control that will play the animation (not null)
     * @param actionName the name of the animation action to be played
     * @param layerName the name of the animation layer on which the action will
     * be played
     */
    public AnimEvent(AnimComposer composer, String actionName,
            String layerName) {
        this.composer = composer;
        this.actionName = actionName;
        this.layerName = layerName;
        /*
         * Override initialDuration, which defaults to 10 seconds.
         */
        Action eventAction = composer.action(actionName);
        initialDuration = (float) eventAction.getLength();
		
		spatialRef = generateSpatialRef();
        composer.getSpatial().setUserData(CINEMATIC_REF, spatialRef);
    }

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected AnimEvent() {
        super();
    }

    /**
     * Generate a unique identifier used to tag a Spatial in the scene graph.
     *
     * @return a unique string identifier
     */
    private String generateSpatialRef() {
        return "cine" + System.currentTimeMillis() + "_" + (spatialId.incrementAndGet());
    }

    /**
     * Recursively search the scene graph for a Spatial whose CINEMATIC_REF
     * matches the stored spatialRef.
     *
     * @param sp the root Spatial to start searching from (not null)
     * @return the matching Spatial, or null if not found
     */
    private Spatial findModelByRef(Spatial sp) {
        String refId = sp.getUserData(CINEMATIC_REF);
        if (spatialRef.equals(refId)) {
            return sp;
        }

        if (sp instanceof Node) {
            for (Spatial child : ((Node) sp).getChildren()) {
                Spatial model = findModelByRef(child);
                if (model != null) {
                    return model;
                }
            }
        }

        return null;
    }

    /**
     * Initialize this event. (for internal use)
     *
     * @param app the Application that contains this event
     * @param cinematic the Cinematic that contains this event
     */
    @Override
    public void initEvent(Application app, Cinematic cinematic) {
        super.initEvent(app, cinematic);
        this.cinematic = cinematic;

        if (composer == null) {
            Spatial model = findModelByRef(cinematic.getScene());
            if (model != null) {
                composer = model.getControl(AnimComposer.class);
            } else {
                throw new UnsupportedOperationException(
                        "No Spatial found in the scene with Cinematic:Ref=" + spatialRef);
            }
        }
    }
	
	@Override
    public void dispose() {
        super.dispose();
        cinematic = null;
        composer = null;
    }

    /**
     * Callback when the event is paused.
     */
    @Override
    public void onPause() {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "layer={0} action={1}",
                    new Object[]{layerName, actionName});
        }

        Object layerManager = composer.getLayerManager(layerName);
        if (layerManager == this) {
            Action eventAction = composer.action(actionName);
            eventAction.setSpeed(0f);
        }
    }

    /**
     * Callback when the event is started.
     */
    @Override
    public void onPlay() {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "layer={0} action={1}",
                    new Object[]{layerName, actionName});
        }

        Action currentAction = composer.getCurrentAction(layerName);
        Action eventAction = composer.action(actionName);
        if (currentAction != eventAction) {
            composer.setCurrentAction(actionName, layerName);
            assert composer.getCurrentAction(layerName) == eventAction;
        }

        if (playState == PlayState.Stopped) {
            composer.setTime(layerName, 0.0);
        }
        eventAction.setSpeed(speed);
        composer.setLayerManager(layerName, this);
    }

    /**
     * Callback when the event is stopped.
     */
    @Override
    public void onStop() {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "layer={0} action={1}",
                    new Object[]{layerName, actionName});
        }
        Object layerManager = composer.getLayerManager(layerName);
        if (layerManager == this) {
            composer.removeCurrentAction(layerName);
            composer.setLayerManager(layerName, null);
        }
    }

    /**
     * Callback on each render pass while the event is playing.
     *
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void onUpdate(float tpf) {
        // do nothing
    }

    /**
     * Alter the speed of the animation.
     *
     * @param speed the relative speed (default=1)
     */
    @Override
    public void setSpeed(float speed) {
        logger.log(Level.INFO, "speed = {0}", speed);
        super.setSpeed(speed);

        if (playState != PlayState.Stopped) {
            Action eventAction = composer.action(actionName);
            eventAction.setSpeed(speed);
        }
    }

    /**
     * Jump to the specified time.
     *
     * @param time the desired time (in seconds, time=0 is the start of the
     * event)
     */
    @Override
    public void setTime(float time) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "layer={0} action={1} time={2}",
                    new Object[]{layerName, actionName, time});
        }
        super.setTime(time);

        Action currentAction = composer.getCurrentAction(layerName);
        Action eventAction = composer.action(actionName);
        if (currentAction != eventAction) {
            composer.setCurrentAction(actionName, layerName);
            assert composer.getCurrentAction(layerName) == eventAction;
        }

        float t = time;
        float duration = (float) eventAction.getLength();
        if (loopMode == LoopMode.Loop) {
            t %= duration;
        } else if (loopMode == LoopMode.Cycle) {
            float direction = (float) Math.ceil(time / duration);
            if (direction > 0f && direction % 2f == 0f) {
                t = duration - t % duration;
            } else {
                t %= duration;
            }
        }

        if (t < 0f) {
            composer.setTime(layerName, 0.0);
        } else if (t > duration) {
            composer.setTime(layerName, t);
            stop();
        } else {
            composer.setTime(layerName, t);
        }
    }

    /**
     * De-serialize this event from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule ic = importer.getCapsule(this);
        spatialRef = ic.readString("spatialRef", null);
        actionName = ic.readString("actionName", null);
        layerName = ic.readString("layerName", AnimComposer.DEFAULT_LAYER);
    }

    /**
     * Serialize this event to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule oc = exporter.getCapsule(this);
        oc.write(spatialRef, "spatialRef", null);
        oc.write(actionName, "actionName", null);
        oc.write(layerName, "layerName", AnimComposer.DEFAULT_LAYER);
    }
}
