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
package com.jme3.cinematic.events;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.audio.AudioNode;
import com.jme3.cinematic.Cinematic;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * A sound track to be played in a cinematic.
 * @author Nehon
 */
public class SoundTrack extends AbstractCinematicEvent {

    protected String path;
    protected AudioNode audioNode;
    protected boolean stream = false;

    /**
     * creates a sound track from the given resource path
     * @param path the path to an audi file (ie : "Sounds/mySound.wav")
     */
    public SoundTrack(String path) {
        this.path = path;
    }

    /**
     * creates a sound track from the given resource path
     * @param path the path to an audi file (ie : "Sounds/mySound.wav")
     * @param stream true to make the audio data streamed
     */
    public SoundTrack(String path, boolean stream) {
        this(path);
        this.stream = stream;
    }

    public SoundTrack(String path, boolean stream, float initialDuration) {
        super(initialDuration);
        this.path = path;
        this.stream = stream;
    }

    public SoundTrack(String path, boolean stream, LoopMode loopMode) {
        super(loopMode);
        this.path = path;
        this.stream = stream;
    }

    public SoundTrack(String path, boolean stream, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.path = path;
        this.stream = stream;
    }

    public SoundTrack(String path, float initialDuration) {
        super(initialDuration);
        this.path = path;
    }

    public SoundTrack(String path, LoopMode loopMode) {
        super(loopMode);
        this.path = path;
    }

    public SoundTrack(String path, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.path = path;
    }

    public SoundTrack() {
    }

    @Override
    public void initEvent(Application app, Cinematic cinematic) {
        super.initEvent(app, cinematic);
        audioNode = new AudioNode(app.getAssetManager(), path, stream);
        setLoopMode(loopMode);
    }

    @Override
    public void setTime(float time) {
        super.setTime(time);
        //can occur on rewind
        if (time < 0) {            
            stop();
        }
        audioNode.setTimeOffset(time);
    }

    @Override
    public void onPlay() {
        audioNode.play();
    }

    @Override
    public void onStop() {
        audioNode.stop();

    }

    @Override
    public void onPause() {
        audioNode.pause();
    }

    @Override
    public void onUpdate(float tpf) {
        if (audioNode.getStatus() == AudioNode.Status.Stopped) {
            stop();
        }
    }

    /**
     *  Returns the underlying audion node of this sound track
     * @return
     */
    public AudioNode getAudioNode() {
        return audioNode;
    }

    @Override
    public void setLoopMode(LoopMode loopMode) {
        super.setLoopMode(loopMode);

        if (loopMode != LoopMode.DontLoop) {
            audioNode.setLooping(true);
        } else {
            audioNode.setLooping(false);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(path, "path", "");
        oc.write(stream, "stream", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        path = ic.readString("path", "");
        stream = ic.readBoolean("stream", false);

    }
}
