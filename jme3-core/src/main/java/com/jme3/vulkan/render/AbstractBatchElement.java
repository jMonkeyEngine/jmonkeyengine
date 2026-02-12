package com.jme3.vulkan.render;

import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;

public abstract class AbstractBatchElement implements BatchElement {

    private final Geometry geometry;
    private float distanceSq = Float.NaN;
    private float distance = Float.NaN;

    public AbstractBatchElement(Geometry geometry) {
        this.geometry = geometry;
    }

    @Override
    public float computeDistanceSq() {
        if (!Float.isNaN(distanceSq)) return distanceSq;
        return (distanceSq = getCamera().getLocation().distanceSquared(geometry.getWorldTranslation()));
    }

    @Override
    public float computeDistance() {
        if (!Float.isNaN(distance)) return distance;
        return (distance = FastMath.sqrt(computeDistanceSq()));
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

}
