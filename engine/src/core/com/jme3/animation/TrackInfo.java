/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.animation;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is intended as a UserData added to a Spatial that is referenced by a Track.
 * (ParticleEmitter for EffectTrack and AudioNode for AudioTrack)
 * It holds the list of tracks that are directly referencing the Spatial.
 * 
 * This is used when loading a Track to find the cloned reference of a Spatial in the cloned model returned by the assetManager.
 *
 * @author Nehon
 */
public class TrackInfo implements Savable {

    ArrayList<Track> tracks = new ArrayList<Track>();

    public TrackInfo() {
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule c = ex.getCapsule(this);
        c.writeSavableArrayList(tracks, "tracks", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule c = im.getCapsule(this);
        tracks = c.readSavableArrayList("tracks", null);
    }

    public ArrayList<Track> getTracks() {
        return tracks;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }
}
