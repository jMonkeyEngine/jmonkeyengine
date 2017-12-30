package com.jme3.anim;

import com.jme3.anim.tween.Tween;
import com.jme3.export.*;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

/**
 * Created by Nehon on 20/12/2017.
 */
public class AnimClip implements JmeCloneable, Savable {

    private String name;
    private double length;

    private TransformTrack[] tracks;

    public AnimClip() {
    }

    public AnimClip(String name) {
        this.name = name;
    }

    public void setTracks(TransformTrack[] tracks) {
        this.tracks = tracks;
        for (TransformTrack track : tracks) {
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


    public TransformTrack[] getTracks() {
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
        TransformTrack[] newTracks = new TransformTrack[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            newTracks[i] = (cloner.clone(tracks[i]));
        }
        this.tracks = newTracks;
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
            tracks = new TransformTrack[arr.length];
            for (int i = 0; i < arr.length; i++) {
                TransformTrack t = (TransformTrack) arr[i];
                tracks[i] = t;
                if (t.getLength() > length) {
                    length = t.getLength();
                }
            }
        }
    }

}
