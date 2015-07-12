package com.jme3.scene.plugins.blender.animations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;

/**
 * An abstract representation of animation. The data stored here is mainly a
 * raw action data loaded from blender. It can later be transformed into
 * bone or spatial animation and applied to the specified node.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BlenderAction implements Cloneable {
    /** The action name. */
    /* package */final String     name;
    /** Animation speed - frames per second. */
    /* package */int              fps;
    /**
     * The last frame of the animation (the last ipo curve node position is
     * used as a last frame).
     */
    /* package */int              stopFrame;
    /**
     * Tracks of the features. In case of bone animation the keys are the
     * names of the bones. In case of spatial animation - the node's name is
     * used. A single ipo contains all tracks for location, rotation and
     * scales.
     */
    /* package */Map<String, Ipo> featuresTracks = new HashMap<String, Ipo>();

    public BlenderAction(String name, int fps) {
        this.name = name;
        this.fps = fps;
    }

    public void removeTracksThatAreNotInTheCollection(Collection<String> trackNames) {
        Map<String, Ipo> newTracks = new HashMap<String, Ipo>();
        for (String trackName : trackNames) {
            if (featuresTracks.containsKey(trackName)) {
                newTracks.put(trackName, featuresTracks.get(trackName));
            }
        }
        featuresTracks = newTracks;
    }

    @Override
    public BlenderAction clone() {
        BlenderAction result = new BlenderAction(name, fps);
        result.stopFrame = stopFrame;
        result.featuresTracks = new HashMap<String, Ipo>(featuresTracks);
        return result;
    }

    /**
     * Converts the action into JME spatial animation tracks.
     * 
     * @param node
     *            the node that will be animated
     * @return the spatial tracks for the node
     */
    public SpatialTrack[] toTracks(Node node, BlenderContext blenderContext) {
        List<SpatialTrack> tracks = new ArrayList<SpatialTrack>(featuresTracks.size());
        for (Entry<String, Ipo> entry : featuresTracks.entrySet()) {
            tracks.add((SpatialTrack) entry.getValue().calculateTrack(0, null, node.getLocalTranslation(), node.getLocalRotation(), node.getLocalScale(), 1, stopFrame, fps, true));
        }
        return tracks.toArray(new SpatialTrack[tracks.size()]);
    }

    /**
     * Converts the action into JME bone animation tracks.
     * 
     * @param skeleton
     *            the skeleton that will be animated
     * @return the bone tracks for the node
     */
    public BoneTrack[] toTracks(Skeleton skeleton, BlenderContext blenderContext) {
        List<BoneTrack> tracks = new ArrayList<BoneTrack>(featuresTracks.size());
        for (Entry<String, Ipo> entry : featuresTracks.entrySet()) {
            int boneIndex = skeleton.getBoneIndex(entry.getKey());
            BoneContext boneContext = blenderContext.getBoneContext(skeleton.getBone(boneIndex));
            tracks.add((BoneTrack) entry.getValue().calculateTrack(boneIndex, boneContext, boneContext.getBone().getBindPosition(), boneContext.getBone().getBindRotation(), boneContext.getBone().getBindScale(), 1, stopFrame, fps, false));
        }
        return tracks.toArray(new BoneTrack[tracks.size()]);
    }

    /**
     * @return the name of the action
     */
    public String getName() {
        return name;
    }

    /**
     * @return the time of animations (in seconds)
     */
    public float getAnimationTime() {
        return (stopFrame - 1) / (float) fps;
    }

    /**
     * Determines if the current action has a track of a given name.
     * CAUTION! The names are case sensitive.
     * 
     * @param name
     *            the name of the track
     * @return <B>true</b> if the track of a given name exists for the
     *         action and <b>false</b> otherwise
     */
    public boolean hasTrackName(String name) {
        return featuresTracks.containsKey(name);
    }

    /**
     * @return the amount of tracks in current action
     */
    public int getTracksCount() {
        return featuresTracks.size();
    }

    @Override
    public String toString() {
        return "BlenderTrack [name = " + name + "; tracks = [" + featuresTracks.keySet() + "]]";
    }
}
