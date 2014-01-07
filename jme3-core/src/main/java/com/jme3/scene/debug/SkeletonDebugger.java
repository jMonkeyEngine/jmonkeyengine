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
package com.jme3.scene.debug;

import java.util.Map;

import com.jme3.animation.Skeleton;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * The class that creates a mesh to display how bones behave.
 * If it is supplied with the bones' lengths it will show exactly how the bones look like on the scene.
 * If not then only connections between each bone heads will be shown.
 */
public class SkeletonDebugger extends Node {
    /** The lines of the bones or the wires between their heads. */
    private SkeletonWire          wires;
    /** The heads and tails points of the bones or only heads if no length data is available. */
    private SkeletonPoints        points;
    /** The dotted lines between a bone's tail and the had of its children. Not available if the length data was not provided. */
    private SkeletonInterBoneWire interBoneWires;

    public SkeletonDebugger() {
    }

    /**
     * Creates a debugger with no length data. The wires will be a connection between the bones' heads only.
     * The points will show the bones' heads only and no dotted line of inter bones connection will be visible.
     * @param name
     *            the name of the debugger's node
     * @param skeleton
     *            the skeleton that will be shown
     */
    public SkeletonDebugger(String name, Skeleton skeleton) {
        this(name, skeleton, null);
    }

    /**
     * Creates a debugger with bone lengths data. If the data is supplied then the wires will show each full bone (from head to tail),
     * the points will display both heads and tails of the bones and dotted lines between bones will be seen.
     * @param name
     *            the name of the debugger's node
     * @param skeleton
     *            the skeleton that will be shown
     * @param boneLengths
     *            a map between the bone's index and the bone's length
     */
    public SkeletonDebugger(String name, Skeleton skeleton, Map<Integer, Float> boneLengths) {
        super(name);

        wires = new SkeletonWire(skeleton, boneLengths);
        points = new SkeletonPoints(skeleton, boneLengths);

        this.attachChild(new Geometry(name + "_wires", wires));
        this.attachChild(new Geometry(name + "_points", points));
        if (boneLengths != null) {
            interBoneWires = new SkeletonInterBoneWire(skeleton, boneLengths);
            this.attachChild(new Geometry(name + "_interwires", interBoneWires));
        }

        this.setQueueBucket(Bucket.Transparent);
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        wires.updateGeometry();
        points.updateGeometry();
        if(interBoneWires != null) {
            interBoneWires.updateGeometry();
        }
    }

    /**
     * @return the skeleton points
     */
    public SkeletonPoints getPoints() {
        return points;
    }

    /**
     * @return the skeleton wires
     */
    public SkeletonWire getWires() {
        return wires;
    }

    /**
     * @return the dotted line between bones (can be null)
     */
    public SkeletonInterBoneWire getInterBoneWires() {
        return interBoneWires;
    }
}