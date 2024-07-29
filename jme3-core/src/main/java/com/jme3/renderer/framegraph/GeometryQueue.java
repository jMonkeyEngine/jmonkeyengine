/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.framegraph;

import com.jme3.renderer.DepthRange;
import com.jme3.renderer.Camera;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.scene.Geometry;
import com.jme3.util.ListSort;
import java.util.ArrayList;

/**
 * Queue of ordered geometries for rendering.
 * <p>
 * Similar to {@link GeometryList}, but designed for use in FrameGraphs. Specifically,
 * this can store other GeometryQueues internally, essentially making queues able
 * to merge very quickly and still maintain geometry order.
 * 
 * @author codex
 */
public class GeometryQueue {
    
    private static final int DEFAULT_SIZE = 32;
    
    private Geometry[] geometries;
    private GeometryComparator comparator;
    private Camera cam;
    private final ListSort listSort;
    private final ArrayList<GeometryQueue> internalQueues = new ArrayList<>();
    private final DepthRange depth = new DepthRange();
    private boolean updateFlag = true;
    private boolean perspective = true;
    private int size;
    
    /**
     * Geometry queue with default settings and a {@link NullComparator}.
     */
    public GeometryQueue() {
        this(new NullComparator());
    }
    /**
     * Geometry queue with default settings and the given comparator.
     * 
     * @param comparator 
     */
    public GeometryQueue(GeometryComparator comparator) {
        size = 0;
        geometries = new Geometry[DEFAULT_SIZE];
        this.comparator = comparator;
        listSort = new ListSort<Geometry>();
    }
    
    /**
     * Sorts this queue and all internal queues.
     */
    public void sort() {
        if (updateFlag && size > 1) {
            // sort the spatial list using the comparator
            if (listSort.getLength() != size) {
                listSort.allocateStack(size);
            }
            listSort.sort(geometries, comparator);
            updateFlag = false;
        }
        for (GeometryQueue q : internalQueues) {
            q.sort();
        }
    }
    /**
     * Renders this queue and all internal queues.
     * 
     * @param renderManager 
     * @param handler 
     */
    public void render(RenderManager renderManager, GeometryRenderHandler handler) {
        GeometryRenderHandler h;
        if (handler == null) {
            h = GeometryRenderHandler.DEFAULT;
        } else {
            h = handler;
        }
        renderManager.getRenderer().setDepthRange(depth);
        if (!perspective) {
            renderManager.setCamera(cam, true);
        }
        for (Geometry g : geometries) {
            if (g == null) continue;
            h.renderGeometry(renderManager, g);
            g.queueDistance = Float.NEGATIVE_INFINITY;
        }
        if (!perspective) {
            renderManager.setCamera(cam, false);
        }
        renderManager.getRenderer().setDepthRange(DepthRange.IDENTITY);
        for (GeometryQueue q : internalQueues) {
            q.render(renderManager, handler);
        }
    }
    
    /**
     * Adds the geometry to the queue.
     * 
     * @param g 
     */
    public void add(Geometry g) {
        if (size == geometries.length) {
            Geometry[] temp = new Geometry[size * 2];
            System.arraycopy(geometries, 0, temp, 0, size);
            geometries = temp; // original list replaced by double-size list
        }
        geometries[size++] = g;
        updateFlag = true;
    }
    /**
     * Sets the element at the given index.
     *
     * @param index The index to set
     * @param value The value
     */
    public void set(int index, Geometry value) {
        geometries[index] = value;
        updateFlag = true;
    }
    /**
     * Adds the geometry queue.
     * 
     * @param q 
     */
    public void add(GeometryQueue q) {
        internalQueues.add(q);
    }
    /**
     * Adds the geometry queue at the index.
     * 
     * @param q
     * @param index 
     */
    public void add(GeometryQueue q, int index) {
        internalQueues.add(index, q);
    }

    /**
     * Resets list size to 0.
     * <p>
     * Clears internal queue list, but does not clear internal queues.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            geometries[i] = null;
        }
        internalQueues.clear();
        updateFlag = true;
        size = 0;
    }
    
    /**
     * Marks this list as requiring sorting.
     */
    public void setUpdateNeeded() {
        updateFlag = true;
    }
    /**
     * Sets the comparator used to sort geometries.
     * 
     * @param comparator 
     */
    public void setComparator(GeometryComparator comparator) {
        if (this.comparator != comparator) {
            this.comparator = comparator;
            updateFlag = true;
        }
    }
    /**
     * Set the camera that will be set on the geometry comparators
     * via {@link GeometryComparator#setCamera(com.jme3.renderer.Camera)}.
     *
     * @param cam Camera to use for sorting.
     */
    public void setCamera(Camera cam) {
        if (this.cam != cam) {
            this.cam = cam;
            comparator.setCamera(this.cam);
            updateFlag = true;
        }
        for (GeometryQueue q : internalQueues) {
            q.setCamera(cam);
        }
    }
    /**
     * Sets the depth range geometries in this queue (not internal queues)
     * are rendered at.
     * 
     * @param depth 
     */
    public void setDepth(DepthRange depth) {
        this.depth.set(depth);
    }
    /**
     * Sets this queue (not internal queues) to render in perspective mode
     * (as opposed to parallel projection or orthogonal).
     * 
     * @param perspective 
     */
    public void setPerspective(boolean perspective) {
        this.perspective = perspective;
    }

    /**
     * Returns the GeometryComparator that this Geometry list uses
     * for sorting.
     *
     * @return the pre-existing instance
     */
    public GeometryComparator getComparator() {
        return comparator;
    }
    /**
     * Returns the number of elements in this GeometryList.
     *
     * @return Number of elements in the list
     */
    public int size() {
        return size;
    }
    /**
     * Returns the element at the given index.
     *
     * @param index The index to lookup
     * @return Geometry at the index
     */
    public Geometry get(int index) {
        return geometries[index];
    }
    /**
     * 
     * @return 
     */
    public DepthRange getDepth() {
        return depth;
    }
    /**
     * 
     * @return 
     */
    public boolean isPerspective() {
        return perspective;
    }
    
}
