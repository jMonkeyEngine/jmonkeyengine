package com.jme3.animation;

/**
 * @deprecated use Animation instead with tracks of selected type (ie. BoneTrack, SpatialTrack, MeshTrack)
 */
@Deprecated
public class SpatialAnimation extends Animation {
    public SpatialAnimation(String name, float length) {
        super(name, length);
    }
}
