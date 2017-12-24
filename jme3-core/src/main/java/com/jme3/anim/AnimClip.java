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
public class AnimClip implements Tween, JmeCloneable, Savable {

    private String name;
    private double length;

    private SafeArrayList<Tween> tracks = new SafeArrayList<>(Tween.class);

    public AnimClip() {
    }

    public AnimClip(String name) {
        this.name = name;
    }

    public void setTracks(Tween[] tracks) {
        for (Tween track : tracks) {
            addTrack(track);
        }
    }

    public void addTrack(Tween track) {
        tracks.add(track);
        if (track.getLength() > length) {
            length = track.getLength();
        }
    }

    public void removeTrack(Tween track) {
        if (tracks.remove(track)) {
            length = 0;
            for (Tween t : tracks.getArray()) {
                if (t.getLength() > length) {
                    length = t.getLength();
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public double getLength() {
        return length;
    }

    @Override
    public boolean interpolate(double t) {
        // Sanity check the inputs
        if (t < 0) {
            return true;
        }

        for (Tween track : tracks.getArray()) {
            track.interpolate(t);
        }
        return t <= length;
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
        SafeArrayList<Tween> newTracks = new SafeArrayList<>(Tween.class);
        for (Tween track : tracks) {
            newTracks.add(cloner.clone(track));
        }
        this.tracks = newTracks;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", null);
        oc.write(tracks.getArray(), "tracks", null);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", null);
        Savable[] arr = ic.readSavableArray("tracks", null);
        if (arr != null) {
            tracks = new SafeArrayList<>(Tween.class);
            for (Savable savable : arr) {
                addTrack((Tween) savable);
            }
        }
    }

}
