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
package com.jme3.animation;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.BitSet;

/**
 * <code>BoneAnimation</code> updates each of its tracks with the skeleton and time
 * to apply the animation.
 * 
 * @author Kirill Vainer
 */
public final class BoneAnimation implements Savable {

    private String name;
    private float length;
    private BoneTrack[] tracks;

    /**
     * Creates a new BoneAnimation with the given name and length.
     * 
     * @param name The name of the bone animation.
     * @param length Length in seconds of the bone animation.
     */
    public BoneAnimation(String name, float length) {
        this.name = name;
        this.length = length;
    }

    /**
     * Serialization-only. Do not use.
     */
    public BoneAnimation() {
    }

    /**
     * The name of the bone animation
     * @return name of the bone animation
     */
    public String getName(){
        return name;
    }

    /**
     * Returns the length in seconds of this animation
     * 
     * @return the length in seconds of this animation
     */
    public float getLength(){
        return length;
    }

    /**
     * Set the {@link BoneTrack}s to be used by this animation.
     * <p>
     * The array should be organized so that the appropriate BoneTrack can
     * be retrieved based on a bone index. 
     * 
     * @param tracks The tracks to set.
     */
    public void setTracks(BoneTrack[] tracks){
        this.tracks = tracks;
    }

    /**
     * Returns the tracks set in {@link #setTracks(com.jme3.animation.BoneTrack[]) }.
     * 
     * @return the tracks set previously
     */
    public BoneTrack[] getTracks(){
        return tracks;
    }

    void setTime(float time, Skeleton skeleton, float weight, BitSet affectedBones) {
        for (int i = 0; i < tracks.length; i++) {
            if (affectedBones == null
                    || affectedBones.get(tracks[i].getTargetBoneIndex())) {
                tracks[i].setTime(time, skeleton, weight);
            }
        }
    }

    @Override
    public String toString() {
        return "BoneAnim[name=" + name + ", length=" + length + "]";
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule out = e.getCapsule(this);
        out.write(name, "name", null);
        out.write(length, "length", 0f);
        out.write(tracks, "tracks", null);
    }

    public void read(JmeImporter i) throws IOException {
        InputCapsule in = i.getCapsule(this);
        name = in.readString("name", null);
        length = in.readFloat("length", 0f);

        Savable[] sav = in.readSavableArray("tracks", null);
        if (sav != null) {
            tracks = new BoneTrack[sav.length];
            System.arraycopy(sav, 0, tracks, 0, sav.length);
        }
    }
}
