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
package com.jme3.cinematic;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.cinematic.events.AbstractCinematicEvent;
import com.jme3.cinematic.events.CameraEvent;
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
 * An AppState for composing and playing cutscenes in a game.
 *
 * <p>A cinematic schedules and plays {@link CinematicEvent}s over a timeline.
 * Once a Cinematic is created, you must attach it to the `AppStateManager` to
 * run it. You can add various `CinematicEvent`s, see the `com.jme3.cinematic.events`
 * package for built-in event types.
 *
 * <p>Events can be added in two main ways:
 * <ul>
 * <li>{@link Cinematic#addCinematicEvent(float, CinematicEvent)} adds an event
 * at a specific time from the cinematic's start.</li>
 * <li>{@link Cinematic#enqueueCinematicEvent(CinematicEvent)} adds events
 * one after another, with each starting at the end of the previous one.</li>
 * </ul>
 *
 * <p>Playback can be controlled with methods like:
 * <ul>
 * <li>{@link Cinematic#play()}</li>
 * <li>{@link Cinematic#pause()}</li>
 * <li>{@link Cinematic#stop()}</li>
 * </ul>
 *
 * <p>Since `Cinematic` itself extends `CinematicEvent`, you can nest cinematics
 * within each other. Nested cinematics should not be attached to the `AppStateManager`.
 *
 * <p>This class also handles multiple camera points of view by creating and
 * activating camera nodes on a schedule.
 * <ul>
 * <li>{@link Cinematic#bindCamera(java.lang.String, com.jme3.renderer.Camera)}</li>
 * <li>{@link Cinematic#activateCamera(float, java.lang.String)}</li>
 * <li>{@link Cinematic#setActiveCamera(java.lang.String)}</li>
 * </ul>
 *
 * @author Nehon
 */
public class Cinematic extends AbstractCinematicEvent implements AppState {

    private static final Logger logger = Logger.getLogger(Cinematic.class.getName());

    private Application app;
    private Node scene;
    protected TimeLine timeLine = new TimeLine();
    private int lastFetchedKeyFrame = -1;
    private final List<CinematicEvent> cinematicEvents = new ArrayList<>();
    private Map<String, CameraNode> cameras = new HashMap<>();
    private CameraNode currentCam;
    private boolean initialized = false;
    private Map<String, Map<Object, Object>> eventsData;
    private float nextEnqueue = 0;
    private String id;

    /**
     * Used for serialization creates a cinematic, don't use this constructor
     * directly
     */
    protected Cinematic() {
        super();
    }

    /**
     * Creates a cinematic with a specific duration.
     *
     * @param initialDuration The total duration of the cinematic in seconds.
     */
    public Cinematic(float initialDuration) {
        super(initialDuration);
    }

    /**
     * Creates a cinematic that loops based on the provided loop mode.
     *
     * @param loopMode The loop mode. See {@link LoopMode}.
     */
    public Cinematic(LoopMode loopMode) {
        super(loopMode);
    }

    /**
     * Creates a cinematic with a specific duration and loop mode.
     *
     * @param initialDuration The total duration of the cinematic in seconds.
     * @param loopMode The loop mode. See {@link LoopMode}.
     */
    public Cinematic(float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
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
     * @param ex the exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(cinematicEvents.toArray(new CinematicEvent[cinematicEvents.size()]), "cinematicEvents", null);
        oc.writeStringSavableMap(cameras, "cameras", null);
        oc.write(timeLine, "timeLine", null);
    }

    /**
     * used internally for serialization
     *
     * @param im the importer (not null)
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        Savable[] events = ic.readSavableArray("cinematicEvents", null);
        for (Savable c : events) {
//            addCinematicEvent(((CinematicEvent) c).getTime(), (CinematicEvent) c)
            cinematicEvents.add((CinematicEvent) c);
        }
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
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        this.app = app;
        initEvent(app, this);
        for (CinematicEvent cinematicEvent : cinematicEvents) {
            cinematicEvent.initEvent(app, this);
        }
        if (!cameras.isEmpty()) {
            for (CameraNode n : cameras.values()) {
                n.setCamera(app.getCamera());
            }
        }
        initialized = true;
    }

    /**
     * used internally
     *
     * @return true if initialized, otherwise false
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     *  Sets the unique ID of this app state.  Note: that setting
     *  this while an app state is attached to the state manager will
     *  have no effect on ID-based lookups.
     *
     * @param id the desired ID
     */
    protected void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * passing true has the same effect as play() you should use play(),
     * pause(), stop() to handle the cinematic playing state.
     *
     * @param enabled true or false
     */
    @Override
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
    @Override
    public boolean isEnabled() {
        return playState == PlayState.Playing;
    }

    /**
     * called internally
     *
     * @param stateManager the state manager
     */
    @Override
    public void stateAttached(AppStateManager stateManager) {
        for (CameraNode n : cameras.values()) {
            if (n.getParent() == null) {
                scene.attachChild(n);
                logger.log(Level.INFO, "Attached CameraNode to the scene: {0}", n);
            }
        }
    }

    /**
     * called internally
     *
     * @param stateManager the state manager
     */
    @Override
    public void stateDetached(AppStateManager stateManager) {
        stop();

        for (CameraNode n : cameras.values()) {
            if (n.getParent() != null) {
                scene.detachChild(n);
                logger.log(Level.INFO, "Detached CameraNode from the scene: {0}", n);
            }
        }
    }

    /**
     * called internally don't call it directly.
     *
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void update(float tpf) {
        if (isInitialized() && playState == PlayState.Playing) {
            internalUpdate(tpf);
        }
    }

    /**
     * used internally, don't call this directly.
     *
     * @param tpf time per frame (in seconds)
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
     * @param timeStamp the time when the event will start after the beginning
     * of the cinematic
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
            cinematicEvent.initEvent(app, this);
        }
        return keyFrame;
    }

    /**
     * Enqueue a cinematic event to a Cinematic. This is handy when you
     * want to chain events without knowing their durations.
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
    @Override
    public void render(RenderManager rm) {
    }

    /**
     * called internally
     *
     * @see AppState#postRender()
     */
    @Override
    public void postRender() {
    }

    /**
     * called internally
     *
     * @see AppState#cleanup()
     */
    @Override
    public void cleanup() {
        initialized = false;
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
     * Binds a camera to this Cinematic, tagged by a unique name. This method
     * creates and returns a CameraNode for the cam and attaches it to the scene.
     * The control direction is set to SpatialToCamera. This camera Node can
     * then be used in other events to handle the camera movements during
     * playback.
     *
     * @param cameraName the unique tag the camera should have
     * @param cam the scene camera.
     * @return the created CameraNode.
     */
    public CameraNode bindCamera(String cameraName, Camera cam) {
        if (cameras.containsKey(cameraName)) {
            throw new IllegalArgumentException("Camera " + cameraName + " is already bound to this cinematic");
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
     * Enables or disables the camera control of the cameraNode of the current cam.
     *
     * @param enabled `true` to enable, `false` to disable.
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
        addCinematicEvent(timeStamp, new CameraEvent(this, cameraName));
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
     * @return the pre-existing object, or null
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
        if (!cameras.isEmpty()) {
            for (CameraNode n : cameras.values()) {
                this.scene.attachChild(n);
            }
        }
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
     * Gets the application instance associated with this cinematic.
     *
     * @return The application.
     */
    public Application getApplication() {
        return app;
    }

    /**
     * Remove all events from the Cinematic.
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
     * Clears all camera nodes bound to the cinematic from the scene node.
     * This method removes all previously bound CameraNodes and clears the
     * internal camera map, effectively detaching all cameras from the scene.
     */
    public void clearCameras() {
        for (CameraNode cameraNode : cameras.values()) {
            scene.detachChild(cameraNode);
        }
        cameras.clear();
    }

    /**
     * used internally to clean up the cinematic. Called when the clear() method
     * is called
     */
    @Override
    public void dispose() {
        for (CinematicEvent event : cinematicEvents) {
            event.dispose();
        }
    }
}



