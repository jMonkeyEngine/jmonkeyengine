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
package com.jme3.cinematic;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.TextureKey;
import com.jme3.cinematic.events.AbstractCinematicEvent;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
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
    private Map<String, Map<String, Object>> eventsData;    
    
    public Cinematic() {
    }
    
    public Cinematic(Node scene) {
        this.scene = scene;
    }
    
    public Cinematic(Node scene, float initialDuration) {
        super(initialDuration);
        this.scene = scene;
    }
    
    public Cinematic(Node scene, LoopMode loopMode) {
        super(loopMode);
        this.scene = scene;
    }
    
    public Cinematic(Node scene, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.scene = scene;
    }
    
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
    
    @Override
    public void onStop() {
        time = 0;
        lastFetchedKeyFrame = -1;
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            ce.stop();
        }
        enableCurrentCam(false);
    }
    
    @Override
    public void onPause() {
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            if (ce.getPlayState() == PlayState.Playing) {
                ce.pause();
            }
        }
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        
        oc.writeSavableArrayList((ArrayList) cinematicEvents, "cinematicEvents", null);
        oc.writeStringSavableMap(cameras, "cameras", null);
        oc.write(timeLine, "timeLine", null);
        
    }
    
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        
        cinematicEvents = ic.readSavableArrayList("cinematicEvents", null);
        cameras = (Map<String, CameraNode>) ic.readStringSavableMap("cameras", null);
        timeLine = (TimeLine) ic.readSavable("timeLine", null);
    }
    
    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed);
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            ce.setSpeed(speed);
        }
        
        
    }
    
    public void initialize(AppStateManager stateManager, Application app) {
        initEvent(app, this);
        for (CinematicEvent cinematicEvent : cinematicEvents) {
            cinematicEvent.initEvent(app, this);
        }
        
        initialized = true;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void setEnabled(boolean enabled) {
        if (enabled) {
            play();
        }
    }
    
    public boolean isEnabled() {
        return playState == PlayState.Playing;
    }
    
    public void stateAttached(AppStateManager stateManager) {
    }
    
    public void stateDetached(AppStateManager stateManager) {
        stop();
    }
    
    public void update(float tpf) {
        if (isInitialized()) {
            internalUpdate(tpf);
        }
    }
    
    @Override
    public void onUpdate(float tpf) {        
        for (int i = 0; i < cinematicEvents.size(); i++) {
            CinematicEvent ce = cinematicEvents.get(i);
            ce.internalUpdate(tpf);
        }
        
        int keyFrameIndex = timeLine.getKeyFrameIndexFromTime(time);

        //iterate to make sure every key frame is triggered
        for (int i = lastFetchedKeyFrame + 1; i <= keyFrameIndex; i++) {
            KeyFrame keyFrame = timeLine.get(i);
            if (keyFrame != null) {
                keyFrame.trigger();
            }
        }
        
        lastFetchedKeyFrame = keyFrameIndex;
    }
    
    @Override
    public void setTime(float time) {
        super.setTime(time);        
        int keyFrameIndex = timeLine.getKeyFrameIndexFromTime(time);

        //triggering all the event from start to "time" 
        //then computing timeOffset for each event
        for (int i = 0; i <= keyFrameIndex; i++) {
            KeyFrame keyFrame = timeLine.get(i);
            if (keyFrame != null) {
                for (CinematicEvent ce : keyFrame.getCinematicEvents()) {
                    ce.play();                    
                    ce.setTime(time - timeLine.getKeyFrameTime(keyFrame));
                }
            }
        }
        if (playState != PlayState.Playing) {
            pause();
        }

        //  step();
    }
    
    public KeyFrame addCinematicEvent(float timeStamp, CinematicEvent cinematicEvent) {
        KeyFrame keyFrame = timeLine.getKeyFrameAtTime(timeStamp);
        if (keyFrame == null) {
            keyFrame = new KeyFrame();
            timeLine.addKeyFrameAtTime(timeStamp, keyFrame);
        }
        keyFrame.cinematicEvents.add(cinematicEvent);
        cinematicEvents.add(cinematicEvent);
        return keyFrame;
    }
    
    public void render(RenderManager rm) {
    }
    
    public void postRender() {
    }
    
    public void cleanup() {
    }

    /**
     * fits the duration of the cinamatic to the duration of all its child cinematic events
     */
    public void fitDuration() {
        KeyFrame kf = timeLine.getKeyFrameAtTime(timeLine.getLastKeyFrameIndex());
        float d = 0;
        for (int i = 0; i < kf.getCinematicEvents().size(); i++) {
            CinematicEvent ce = kf.getCinematicEvents().get(i);
            if (d < (ce.getDuration() * ce.getSpeed())) {
                d = (ce.getDuration() * ce.getSpeed());
            }
        }
        
        initialDuration = d;
    }
    
    public CameraNode bindCamera(String cameraName, Camera cam) {
        CameraNode node = new CameraNode(cameraName, cam);
        node.setControlDir(ControlDirection.SpatialToCamera);
        node.getControl(CameraControl.class).setEnabled(false);
        cameras.put(cameraName, node);
        scene.attachChild(node);
        return node;
    }
    
    public CameraNode getCamera(String cameraName) {
        return cameras.get(cameraName);
    }
    
    private void enableCurrentCam(boolean enabled) {
        if (currentCam != null) {
            currentCam.getControl(CameraControl.class).setEnabled(enabled);
        }
    }
    
    public void setActiveCamera(String cameraName) {
        enableCurrentCam(false);
        currentCam = cameras.get(cameraName);
        if (currentCam == null) {
            logger.log(Level.WARNING, "{0} is not a camera bond to the cinematic, cannot activate", cameraName);
        }
        enableCurrentCam(true);
    }
    
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
            public void setTime(float time) {
                play();
            }
        });
    }
    
    public void setScene(Node scene) {
        this.scene = scene;
    }
    
    private Map<String, Map<String, Object>> getEventsData() {
        if (eventsData == null) {
            eventsData = new HashMap<String, Map<String, Object>>();
        }
        return eventsData;
    }
    
    public void putEventData(String type, String name, Object object) {
        Map<String, Map<String, Object>> data = getEventsData();
        Map<String, Object> row = data.get(type);
        if (row == null) {
            row = new HashMap<String, Object>();
        }
        row.put(name, object);
    }
    
    public Object getEventData(String type, String name) {
        if (eventsData != null) {
            Map<String, Object> row = eventsData.get(type);
            if (row != null) {
                return row.get(name);
            }
        }
        return null;
    }
    
    public Savable removeEventData(String type, String name) {
        if (eventsData != null) {
            Map<String, Object> row = eventsData.get(type);
            if (row != null) {
                row.remove(name);
            }
        }
        return null;
    }
    
    public Node getScene() {
        return scene;
    }
}
