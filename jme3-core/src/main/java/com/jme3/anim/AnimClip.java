/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

/**
 * Created by Nehon on 20/12/2017.
 */
public class AnimClip implements JmeCloneable, Savable {

    private String name;
    private double length;

    private AnimTrack[] tracks;

    protected AnimClip() {
    }

    public AnimClip(String name) {
        this.name = name;
    }

    public void setTracks(AnimTrack[] tracks) {
        this.tracks = tracks;
        for (AnimTrack track : tracks) {
            if (track.getLength() > length) {
                length = track.getLength();
            }
        }
    }

    public String getName() {
        return name;
    }


    public double getLength() {
        return length;
    }


    public AnimTrack[] getTracks() {
        return tracks;
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning", e);
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        AnimTrack[] newTracks = new AnimTrack[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            newTracks[i] = (cloner.clone(tracks[i]));
        }
        this.tracks = newTracks;
    }

    @Override
    public String toString() {
        return "Clip " + name + ", " + length + 's';
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", null);
        oc.write(tracks, "tracks", null);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", null);
        Savable[] arr = ic.readSavableArray("tracks", null);
        if (arr != null) {
            tracks = new AnimTrack[arr.length];
            for (int i = 0; i < arr.length; i++) {
                AnimTrack t = (AnimTrack) arr[i];
                tracks[i] = t;
                if (t.getLength() > length) {
                    length = t.getLength();
                }
            }
        }
    }

}
