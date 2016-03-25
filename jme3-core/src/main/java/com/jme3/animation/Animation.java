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
package com.jme3.animation;

import com.jme3.export.*;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * The animation class updates the animation target with the tracks of a given type.
 * 
 * @author Kirill Vainer, Marcin Roguski (Kaelthas)
 */
public class Animation implements Savable, Cloneable, JmeCloneable {

    /** 
     * The name of the animation. 
     */
    private String name;
    /** 
     * The length of the animation. 
     */
    private float length;
    /** 
     * The tracks of the animation. 
     */
    private SafeArrayList<Track> tracks = new SafeArrayList<Track>(Track.class);

    /**
     * Serialization-only. Do not use.
     */
    public Animation() {
    }

    /**
     * Creates a new <code>Animation</code> with the given name and length.
     * 
     * @param name The name of the animation.
     * @param length Length in seconds of the animation.
     */
    public Animation(String name, float length) {
        this.name = name;
        this.length = length;
    }

    /**
     * The name of the bone animation
     * @return name of the bone animation
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the length in seconds of this animation
     * 
     * @return the length in seconds of this animation
     */
    public float getLength() {
        return length;
    }

    /**
     * This method sets the current time of the animation.
     * This method behaves differently for every known track type.
     * Override this method if you have your own type of track.
     * 
     * @param time the time of the animation
     * @param blendAmount the blend amount factor
     * @param control the animation control
     * @param channel the animation channel
     */
    void setTime(float time, float blendAmount, AnimControl control, AnimChannel channel, TempVars vars) {
        if (tracks == null) {
            return;
        }

        for (Track track : tracks) {
            track.setTime(time, blendAmount, control, channel, vars);
        }
    }

    /**
     * Set the {@link Track}s to be used by this animation.
     * 
     * @param tracksArray The tracks to set.
     */
    public void setTracks(Track[] tracksArray) {
        for (Track track : tracksArray) {
            tracks.add(track);
        }
    }

    /**
     * Adds a track to this animation
     * @param track the track to add
     */
    public void addTrack(Track track) {
        tracks.add(track);
    }

    /**
     * removes a track from this animation
     * @param track the track to remove
     */
    public void removeTrack(Track track) {
        tracks.remove(track);
        if (track instanceof ClonableTrack) {
            ((ClonableTrack) track).cleanUp();
        }
    }

    /**
     * Returns the tracks set in {@link #setTracks(com.jme3.animation.Track[]) }.
     * 
     * @return the tracks set previously
     */
    public Track[] getTracks() {
        return tracks.getArray();
    }

    /**
     * This method creates a clone of the current object.
     * @return a clone of the current object
     */
    @Override
    public Animation clone() {
        try {
            Animation result = (Animation) super.clone();
            result.tracks = new SafeArrayList<Track>(Track.class);
            for (Track track : tracks) {
                result.tracks.add(track.clone());
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * 
     * @param spat
     * @return 
     */
    public Animation cloneForSpatial(Spatial spat) {
        try {
            Animation result = (Animation) super.clone();
            result.tracks = new SafeArrayList<Track>(Track.class);
            for (Track track : tracks) {
                if (track instanceof ClonableTrack) {
                    result.tracks.add(((ClonableTrack) track).cloneForSpatial(spat));
                } else {
                    result.tracks.add(track);
                }
            }
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override   
    public Object jmeClone() {
        try {
            return super.clone();
        } catch( CloneNotSupportedException e ) {
            throw new RuntimeException("Error cloning", e);
        }
    }     

    @Override   
    public void cloneFields( Cloner cloner, Object original ) {
         
        // There is some logic here that I'm copying but I'm not sure if
        // it's a mistake or not.  If a track is not a CloneableTrack then it
        // isn't cloned at all... even though they all implement clone() methods. -pspeed
        SafeArrayList<Track> newTracks = new SafeArrayList<>(Track.class);
        for( Track track : tracks ) {
            if( track instanceof ClonableTrack ) {
                newTracks.add(cloner.clone(track));
            } else {
                // this is the part that seems fishy 
                newTracks.add(track);
            }
        }
        this.tracks = newTracks;
    }
         
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + name + ", length=" + length + ']';
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", null);
        out.write(length, "length", 0f);
        out.write(tracks.getArray(), "tracks", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", null);
        length = in.readFloat("length", 0f);

        Savable[] arr = in.readSavableArray("tracks", null);
        if (arr != null) {
            // NOTE: Backward compat only .. Some animations have no
            // tracks set at all even though it makes no sense.
            // Since there's a null check in setTime(),
            // its only appropriate that the check is made here as well.
            tracks = new SafeArrayList<Track>(Track.class);
            for (Savable savable : arr) {
                tracks.add((Track) savable);
            }
        }
    }
}
