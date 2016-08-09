/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.cinematic;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.cinematic.events.AbstractCinematicEvent;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.export.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl;
import com.jme3.scene.control.CameraControl.ControlDirection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An appstate for composing and playing cut scenes in a game. The cinematic
 * schedules CinematicEvents over a timeline. Once the Cinematic created it has
 * to be attached to the stateManager.
 *
 * You can add various CinematicEvents to a Cinematic, see package
 * com.jme3.cinematic.events
 *
 * Two main methods can be used to add an event :
 *
 * @see Cinematic#addCinematicEvent(float,
 * com.jme3.cinematic.events.CinematicEvent) , that adds an event at the given
 * time form the cinematic start.
 *
 * @see
 * Cinematic#enqueueCinematicEvent(com.jme3.cinematic.events.CinematicEvent)
 * that enqueue events one after the other according to their initialDuration
 *
 * a cinematic has convenient methods to handle the playback :
 * @see Cinematic#play()
 * @see Cinematic#pause()
 * @see Cinematic#stop()
 *
 * A cinematic is itself a CinematicEvent, meaning you can embed several
 * Cinematics Embed cinematics must not be added to the stateManager though.
 *
 * Cinematic has a way to handle several point of view by creating CameraNode
 * over a cam and activating them on schedule.
 * @see Cinematic#bindCamera(java.lang.String, com.jme3.renderer.Camera)
 * @see Cinematic#activateCamera(float, java.lang.String)
 * @see Cinematic#setActiveCamera(java.lang.String)
 *
 * @author Nehon
 */
public class Cinematic extends AbstractCinematicEvent implements AppState {

    private static final Logger logger = Logger.getLogger(Application.class.getName());
    private Node scene;
    protected TimeLine timeLine = new TimeLine();
    private int lastFetchedKeyFrame = -1;
    private List<CinematicEvent> cinematicEvents = new ArrayList<CinematicEvent>();
    private Map<String, CameraNode> cameras = new HashMap<String, CameraNode>();
    private CameraNode currentCam;
    private boolean initialized = false;
    private Map<String, Map<Object, Object>> eventsData;
    private float nextEnqueue = 0;

    /**
     * Used for serialization creates a cinematic, don't use this constructor
     * directly
     */
    public Cinematic() {
    }

    /**
     * creates a cinematic
     *
     * @param scene the scene in which the cinematic should take place
     */
    public Cinematic(Node scene) {
        this.scene = scene;
    }

    /**
     * creates a cinematic
     *
     * @param scene the scene in which the cinematic should take place
     * @param initialDuration the duration of the cinematic (without considering
     * the speed)
     */
    public Cinematic(Node scene, float initialDuration) {
        super(initialDuration);
        this.scene = scene;
    }

    /**
     * creates a cinematic
     *
     * @param scene the scene in which the cinematic should take place
     * @param loopMode tells if this cinematic should be looped or not
     */
    public Cinematic(Node scene, LoopMode loopMode) {
        super(loopMode);
        this.scene = scene;
    }

    /**
     * creates a cinematic
     *
     * @param scene the scene in which the cinematic should take place
     * @param initialDuration the duration of the cinematic (without considering
     * the speed)
     * @param loopMode tells if this cinematic should be looped or not
     */
    public Cinematic(Node scene, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.scene = scene;
    }

    /**
     * called internally
     */
    @Override
    public void onPlay() {
        if (isInitialized()) {
            if (playState == PlayState.Paused) {
                for (int i = 0; i < cinematicEvents.size(); i++) {
                    CinematicEvent ce = cinematicEvents.get(i);
                    if (ce.getPlayState() == PlayState.Paused) {
                        ce.play();
                    }
                }
            }
        }
    }

    /**
     * called internally
     */
    @Override
    public void onStop() {
        time = 0;
        lastFetchedKeyFrame = -1;
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            ce.setTime(0);
            ce.forceStop();
        }
        setEnableCurrentCam(false);
    }

    /**
     * called internally
     */
    @Override
    public void onPause() {
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            if (ce.getPlayState() == PlayState.Playing) {
                ce.pause();
            }
        }
    }

    /**
     * used internally for serialization
     *
     * @param ex
     * @throws IOException
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        oc.writeSavableArrayList((ArrayList) cinematicEvents, "cinematicEvents", null);
        oc.writeStringSavableMap(cameras, "cameras", null);
        oc.write(timeLine, "timeLine", null);

    }

    /**
     * used internally for serialization
     *
     * @param im
     * @throws IOException
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        cinematicEvents = ic.readSavableArrayList("cinematicEvents", null);
        cameras = (Map<String, CameraNode>) ic.readStringSavableMap("cameras", null);
        timeLine = (TimeLine) ic.readSavable("timeLine", null);
    }

    /**
     * sets the speed of the cinematic. Note that it will set the speed of all
     * events in the cinematic. 1 is normal speed. use 0.5f to make the
     * cinematic twice slower, use 2 to make it twice faster
     *
     * @param speed the speed
     */
    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            ce.setSpeed(speed);
        }


    }

    /**
     * used internally
     *
     * @param stateManager the state manager
     * @param app the application
     */
    public void initialize(AppStateManager stateManager, Application app) {
        initEvent(app, this);
        for (CinematicEvent cinematicEvent : cinematicEvents) {
            cinematicEvent.initEvent(app, this);
        }

        initialized = true;
    }

    /**
     * used internally
     *
     * @return
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * passing true has the same effect as play() you should use play(),
     * pause(), stop() to handle the cinematic playing state.
     *
     * @param enabled true or false
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            play();
        }
    }

    /**
     * return true if the cinematic appstate is enabled (the cinematic is
     * playing)
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return playState == PlayState.Playing;
    }

    /**
     * called internally
     *
     * @param stateManager the state manager
     */
    public void stateAttached(AppStateManager stateManager) {
    }

    /**
     * called internally
     *
     * @param stateManager the state manager
     */
    public void stateDetached(AppStateManager stateManager) {
        stop();
    }

    /**
     * called internally don't call it directly.
     *
     * @param tpf
     */
    public void update(float tpf) {
        if (isInitialized()) {
            internalUpdate(tpf);
        }
    }

    /**
     * used internally, don't call this directly.
     *
     * @param tpf
     */
    @Override
    public void onUpdate(float tpf) {
        int keyFrameIndex = timeLine.getKeyFrameIndexFromTime(time);

        //iterate to make sure every key frame is triggered
        for (int i = lastFetchedKeyFrame + 1; i <= keyFrameIndex; i++) {
            KeyFrame keyFrame = timeLine.get(i);
            if (keyFrame != null) {
                keyFrame.trigger();
            }
        }

        
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            ce.internalUpdate(tpf);
        }

        
        lastFetchedKeyFrame = keyFrameIndex;
    }

    /**
     * This is used internally but can be called to shuffle through the
     * cinematic.
     *
     * @param time the time to shuffle to.
     */
    @Override
    public void setTime(float time) {

        //stopping all events
        onStop();
        super.setTime(time);

        int keyFrameIndex = timeLine.getKeyFrameIndexFromTime(time);
        //triggering all the event from start to "time" 
        //then computing timeOffset for each event
        for (int i = 0; i <= keyFrameIndex; i++) {
            KeyFrame keyFrame = timeLine.get(i);
            if (keyFrame != null) {
                for (CinematicEvent ce : keyFrame.getCinematicEvents()) {
                    float t = this.time - timeLine.getKeyFrameTime(keyFrame);
                    if (t >= 0 && (t <= ce.getInitialDuration() || ce.getLoopMode() != LoopMode.DontLoop)) {
                        ce.play();
                    }
                    ce.setTime(t);
                }
            }
        }
        lastFetchedKeyFrame = keyFrameIndex;
        if (playState != PlayState.Playing) {
            pause();
        }
    }

    /**
     * Adds a cinematic event to this cinematic at the given timestamp. This
     * operation returns a keyFrame
     *
     * @param timeStamp the time when the event will start after the beginning of
     * the cinematic
     * @param cinematicEvent the cinematic event
     * @return the keyFrame for that event.
     */
    public KeyFrame addCinematicEvent(float timeStamp, CinematicEvent cinematicEvent) {
        KeyFrame keyFrame = timeLine.getKeyFrameAtTime(timeStamp);
        if (keyFrame == null) {
            keyFrame = new KeyFrame();
            timeLine.addKeyFrameAtTime(timeStamp, keyFrame);
        }
        keyFrame.cinematicEvents.add(cinematicEvent);
        cinematicEvents.add(cinematicEvent);
        if (isInitialized()) {
            cinematicEvent.initEvent(null, this);
        }
        return keyFrame;
    }

    /**
     * enqueue a cinematic event to a cinematic. This is a handy method when you
     * want to chain event of a given duration without knowing their initial
     * duration
     *
     * @param cinematicEvent the cinematic event to enqueue
     * @return the timestamp the event was scheduled.
     */
    public float enqueueCinematicEvent(CinematicEvent cinematicEvent) {
        float scheduleTime = nextEnqueue;
        addCinematicEvent(scheduleTime, cinematicEvent);
        nextEnqueue += cinematicEvent.getInitialDuration();
        return scheduleTime;
    }

    /**
     * removes the first occurrence found of the given cinematicEvent.
     *
     * @param cinematicEvent the cinematicEvent to remove
     * @return true if the element has been removed
     */
    public boolean removeCinematicEvent(CinematicEvent cinematicEvent) {
        cinematicEvent.dispose();
        cinematicEvents.remove(cinematicEvent);
        for (KeyFrame keyFrame : timeLine.values()) {
            if (keyFrame.cinematicEvents.remove(cinematicEvent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * removes the first occurrence found of the given cinematicEvent for the
     * given time stamp.
     *
     * @param timeStamp the timestamp when the cinematicEvent has been added
     * @param cinematicEvent the cinematicEvent to remove
     * @return true if the element has been removed
     */
    public boolean removeCinematicEvent(float timeStamp, CinematicEvent cinematicEvent) {
        cinematicEvent.dispose();
        KeyFrame keyFrame = timeLine.getKeyFrameAtTime(timeStamp);
        return removeCinematicEvent(keyFrame, cinematicEvent);
    }

    /**
     * removes the first occurrence found of the given cinematicEvent for the
     * given keyFrame
     *
     * @param keyFrame the keyFrame returned by the addCinematicEvent method.
     * @param cinematicEvent the cinematicEvent to remove
     * @return true if the element has been removed
     */
    public boolean removeCinematicEvent(KeyFrame keyFrame, CinematicEvent cinematicEvent) {
        cinematicEvent.dispose();
        boolean ret = keyFrame.cinematicEvents.remove(cinematicEvent);
        cinematicEvents.remove(cinematicEvent);
        if (keyFrame.isEmpty()) {
            timeLine.removeKeyFrame(keyFrame.getIndex());
        }
        return ret;
    }

    /**
     * called internally
     *
     * @see AppState#render(com.jme3.renderer.RenderManager) 
     */
    public void render(RenderManager rm) {
    }

    /**
     * called internally
     *
     * @see AppState#postRender()
     */
    public void postRender() {
    }

    /**
     * called internally
     *
     * @see AppState#cleanup()
     */
    public void cleanup() {
    }

    /**
     * fits the duration of the cinematic to the duration of all its child
     * cinematic events
     */
    public void fitDuration() {
        KeyFrame kf = timeLine.getKeyFrameAtIndex(timeLine.getLastKeyFrameIndex());
        float d = 0;
        for (int i = 0; i < kf.getCinematicEvents().size(); i++) {
            CinematicEvent ce = kf.getCinematicEvents().get(i);
            float dur = timeLine.getKeyFrameTime(kf) + ce.getDuration() * ce.getSpeed();
            if (d < dur) {
                d = dur;
            }
        }

        initialDuration = d;
    }

    /**
     * Binds a camera to this cinematic, tagged by a unique name. This methods
     * creates and returns a CameraNode for the cam and attach it to the scene.
     * The control direction is set to SpatialToCamera. This camera Node can
     * then be used in other events to handle the camera movements during the
     * playback
     *
     * @param cameraName the unique tag the camera should have
     * @param cam the scene camera.
     * @return the created CameraNode.
     */
    public CameraNode bindCamera(String cameraName, Camera cam) {
        if (cameras.containsKey(cameraName)) {
            throw new IllegalArgumentException("Camera " + cameraName + " is already binded to this cinematic");
        }
        CameraNode node = new CameraNode(cameraName, cam);
        node.setControlDir(ControlDirection.SpatialToCamera);
        node.getControl(CameraControl.class).setEnabled(false);
        cameras.put(cameraName, node);
        scene.attachChild(node);
        return node;
    }

    /**
     * returns a cameraNode given its name
     *
     * @param cameraName the camera name (as registered in
     * Cinematic#bindCamera())
     * @return the cameraNode for this name
     */
    public CameraNode getCamera(String cameraName) {
        return cameras.get(cameraName);
    }

    /**
     * enable/disable the camera control of the cameraNode of the current cam
     *
     * @param enabled
     */
    private void setEnableCurrentCam(boolean enabled) {
        if (currentCam != null) {
            currentCam.getControl(CameraControl.class).setEnabled(enabled);
        }
    }

    /**
     * Sets the active camera instantly (use activateCamera if you want to
     * schedule that event)
     *
     * @param cameraName the camera name (as registered in
     * Cinematic#bindCamera())
     */
    public void setActiveCamera(String cameraName) {
        setEnableCurrentCam(false);
        currentCam = cameras.get(cameraName);
        if (currentCam == null) {
            logger.log(Level.WARNING, "{0} is not a camera bond to the cinematic, cannot activate", cameraName);
        }
        setEnableCurrentCam(true);
    }

    /**
     * schedule an event that will activate the camera at the given time
     *
     * @param timeStamp the time to activate the cam
     * @param cameraName the camera name (as registered in
     * Cinematic#bindCamera())
     */
    public void activateCamera(final float timeStamp, final String cameraName) {
        addCinematicEvent(timeStamp, new AbstractCinematicEvent() {
            @Override
            public void play() {
                super.play();
                stop();
            }

            @Override
            public void onPlay() {
                setActiveCamera(cameraName);
            }

            @Override
            public void onUpdate(float tpf) {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onPause() {
            }

            @Override
            public void forceStop() {
            }

            @Override
            public void setTime(float time) {
                play();
            }
        });
    }

    /**
     * returns the complete eventdata map
     *
     * @return the eventdata map
     */
    private Map<String, Map<Object, Object>> getEventsData() {
        if (eventsData == null) {
            eventsData = new HashMap<String, Map<Object, Object>>();
        }
        return eventsData;
    }

    /**
     * used internally put an eventdata in the cinematic
     *
     * @param type the type of data
     * @param key the key
     * @param object the data
     */
    public void putEventData(String type, Object key, Object object) {
        Map<String, Map<Object, Object>> data = getEventsData();
        Map<Object, Object> row = data.get(type);
        if (row == null) {
            row = new HashMap<Object, Object>();
        }
        row.put(key, object);
        data.put(type, row);
    }

    /**
     * used internally return and event data
     *
     * @param type the type of data
     * @param key the key
     * @return
     */
    public Object getEventData(String type, Object key) {
        if (eventsData != null) {
            Map<Object, Object> row = eventsData.get(type);
            if (row != null) {
                return row.get(key);
            }
        }
        return null;
    }

    /**
     * Used internally remove an eventData
     *
     * @param type the type of data
     * @param key the key of the data
     */
    public void removeEventData(String type, Object key) {
        if (eventsData != null) {
            Map<Object, Object> row = eventsData.get(type);
            if (row != null) {
                row.remove(key);
            }
        }
    }

    /**
     * sets the scene to use for this cinematic it is expected that the scene is
     * added before adding events to the cinematic
     *
     * @param scene the scene where the cinematic should take place.
     */
    public void setScene(Node scene) {
        this.scene = scene;
    }

    /**
     * return the scene where the cinematic occur
     *
     * @return the scene
     */
    public Node getScene() {
        return scene;
    }

    /**
     * clear the cinematic of its events.
     */
    public void clear() {
        dispose();
        cinematicEvents.clear();
        timeLine.clear();
        if (eventsData != null) {
            eventsData.clear();
        }
    }

    /**
     * used internally to cleanup the cinematic. Called when the clear() method
     * is called
     */
    @Override
    public void dispose() {
        for (CinematicEvent event : cinematicEvents) {
            event.dispose();
        }
    }
}
