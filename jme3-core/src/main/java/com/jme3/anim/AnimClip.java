/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
 * A named set of animation tracks that can be played in synchrony.
 *
 * Created by Nehon on 20/12/2017.
 */
public class AnimClip implements JmeCloneable, Savable {

    private String name;
    private double length;

    private AnimTrack[] tracks;

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected AnimClip() {
    }

    /**
     * Instantiate a zero-length clip with the specified name.
     *
     * @param name desired name for the new clip
     */
    public AnimClip(String name) {
        this.name = name;
    }

    /**
     * Replace all tracks in this clip. This method may increase the clip's
     * length, but it will never reduce it.
     *
     * @param tracks the tracks to use (alias created)
     */
    public void setTracks(AnimTrack[] tracks) {
        this.tracks = tracks;
        for (AnimTrack track : tracks) {
            if (track.getLength() > length) {
                length = track.getLength();
            }
        }
    }

    /**
     * Determine the name of this clip.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Determine the duration of this clip.
     *
     * @return the duration (in seconds)
     */
    public double getLength() {
        return length;
    }

    /**
     * Access all the tracks in this clip.
     *
     * @return the pre-existing array
     */
    public AnimTrack[] getTracks() {
        return tracks;
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning", e);
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned clip into a deep-cloned one, using the specified Cloner
     * and original to resolve copied fields.
     *
     * @param cloner the Cloner that's cloning this clip (not null)
     * @param original the instance from which this clip was shallow-cloned (not
     * null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        AnimTrack[] newTracks = new AnimTrack[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            newTracks[i] = (cloner.clone(tracks[i]));
        }
        this.tracks = newTracks;
    }

    /**
     * Represent this clip as a String.
     *
     * @return a descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        return "Clip " + name + ", " + length + 's';
    }

    /**
     * Serialize this clip to the specified exporter, for example when saving to
     * a J3O file.
     *
     * @param ex the exporter to write to (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", null);
        oc.write(tracks, "tracks", null);

    }

    /**
     * De-serialize this clip from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im the importer to read from (not null)
     * @throws IOException from the importer
     */
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
