package com.jme3.vulkan.render;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.vulkan.scene.Scene;

import java.util.Comparator;

/**
 * A {@link Bucket}'s own final say on the order its nodes are drawn in, applied within a
 * single bucket/technique pairing after that technique has already grouped its nodes by
 * pipeline. This is deliberately not fixed-function: implement it directly for whatever
 * custom ordering a bucket needs. {@link #sortByDistance} covers the common case - nearest or
 * farthest node first - but a bucket is free to sort by anything else instead.
 */
public interface Bucket {

    /**
     * Builds the comparator this bucket should sort its node ids with for one dispatch,
     * given the scene and camera nodes are being drawn against. Called once per dispatch,
     * not once per comparison, so it is a reasonable place to capture per-frame context
     * (e.g. the camera's current position) that every comparison will need.
     *
     * @param scene scene the nodes being sorted belong to
     * @param camera camera the nodes are being drawn against
     * @return comparator over node ids
     */
    Comparator<Integer> createComparator(Scene scene, Camera camera);

    /**
     * A camera-distance sort.
     *
     * @param frontToBack true to sort nearest-first (e.g. for an opaque bucket, so early
     *                    depth rejection has the best chance of paying off), false to sort
     *                    farthest-first (e.g. for a transparent bucket, so blending
     *                    composites correctly)
     * @return distance-based bucket sort
     */
    static Bucket sortByDistance(boolean frontToBack) {
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        int sign = frontToBack ? 1 : -1;
        return (scene, camera) -> {
            Vector3f camLocation = camera.getLocation();
            return (nodeA, nodeB) -> sign * Float.compare(
                    scene.getWorldTranslation(nodeA, a).distanceSquared(camLocation),
                    scene.getWorldTranslation(nodeB, b).distanceSquared(camLocation));
        };
    }

}
