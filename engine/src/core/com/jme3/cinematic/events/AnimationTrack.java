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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.PlayState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public class AnimationTrack extends AbstractCinematicEvent {

    private static final Logger log = Logger.getLogger(AnimationTrack.class.getName());
    protected AnimChannel channel;
    protected String animationName;
    protected String modelName;

    public AnimationTrack() {
    }

    public AnimationTrack(Spatial model, String animationName) {
        modelName = model.getName();
        this.animationName = animationName;
    }

    public AnimationTrack(Spatial model, String animationName, float initialDuration) {
        super(initialDuration);
        modelName = model.getName();
        this.animationName = animationName;
    }

    public AnimationTrack(Spatial model, String animationName, LoopMode loopMode) {
        super(loopMode);
        modelName = model.getName();
        this.animationName = animationName;
    }

    public AnimationTrack(Spatial model, String animationName, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        modelName = model.getName();
        this.animationName = animationName;
    }

    @Override
    public void initEvent(Application app, Cinematic cinematic) {
        if (channel == null) {
            Object s = cinematic.getEventData("modelChannels", modelName);
            if (s != null && s instanceof AnimChannel) {
                this.channel = (AnimChannel) s;
            } else if (s == null) {
                Spatial model = cinematic.getScene().getChild(modelName);
                if (model != null) {
                    channel = model.getControl(AnimControl.class).createChannel();
                    cinematic.putEventData("modelChannels", modelName, channel);
                } else {
                    log.log(Level.WARNING, "spatial {0} not found in the scene, cannot perform animation", modelName);
                }
            }

        }
    }

    @Override
    public void onPlay() {
        channel.getControl().setEnabled(true);
        if (playState == PlayState.Stopped) {
            channel.setAnim(animationName);
        }
    }

    @Override
    public void onUpdate(float tpf) {
    }

    @Override
    public void onStop() {
        channel.getControl().setEnabled(false);
    }

    @Override
    public void onPause() {
        channel.getControl().setEnabled(false);
    }

    @Override
    public void setLoopMode(LoopMode loopMode) {
        super.setLoopMode(loopMode);
        channel.setLoopMode(loopMode);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(modelName, "modelName", "");
        oc.write(animationName, "animationName", "");
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        modelName = ic.readString("modelName", "");
        animationName = ic.readString("animationName", "");
    }
}
