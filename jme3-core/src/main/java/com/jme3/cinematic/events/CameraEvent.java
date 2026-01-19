/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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

import com.jme3.app.Application;
import com.jme3.cinematic.CinematicHandler;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * A `CameraEvent` is a cinematic event that instantly sets the active camera
 * within a `Cinematic` sequence.
 *
 * @author Rickard (neph1 @ github)
 */
public class CameraEvent extends AbstractCinematicEvent {

    /**
     * The name of the camera to activate.
     */
    private String cameraName;
    /**
     * The `Cinematic` instance to which this event belongs and on which the
     * camera will be set.
     */
    private CinematicHandler cinematic;

    /**
     * For serialization only. Do not use.
     */
    public CameraEvent() {
    }

    /**
     * Constructs a new `CameraEvent` with the specified cinematic and camera name.
     *
     * @param cinematic  The `Cinematic` instance this event belongs to (cannot be null).
     * @param cameraName The name of the camera to be activated by this event (cannot be null or empty).
     */
    public CameraEvent(CinematicHandler cinematic, String cameraName) {
        this.cinematic = cinematic;
        this.cameraName = cameraName;
    }

    @Override
    public void initEvent(Application app, CinematicHandler cinematic) {
        super.initEvent(app, cinematic);
        this.cinematic = cinematic;
    }

    @Override
    public void play() {
        super.play();
        stop();
    }

    @Override
    public void onPlay() {
        cinematic.setActiveCamera(cameraName);
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

    /**
     * Returns the `Cinematic` instance associated with this event.
     * @return The `Cinematic` instance.
     */
    public CinematicHandler getCinematic() {
        return cinematic;
    }

    /**
     * Sets the `Cinematic` instance for this event.
     * @param cinematic The `Cinematic` instance to set (cannot be null).
     */
    public void setCinematic(CinematicHandler cinematic) {
        this.cinematic = cinematic;
    }

    /**
     * Returns the name of the camera that this event will activate.
     * @return The camera name.
     */
    public String getCameraName() {
        return cameraName;
    }

    /**
     * Sets the name of the camera that this event will activate.
     * @param cameraName The new camera name (cannot be null or empty).
     */
    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    /**
     * Used internally for serialization.
     *
     * @param ex The exporter (not null).
     * @throws IOException If an I/O error occurs during serialization.
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(cameraName, "cameraName", null);
    }

    /**
     * Used internally for deserialization.
     *
     * @param im The importer (not null).
     * @throws IOException If an I/O error occurs during deserialization.
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        cameraName = ic.readString("cameraName", null);
    }
}
