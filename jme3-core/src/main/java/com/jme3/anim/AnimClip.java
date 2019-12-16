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
