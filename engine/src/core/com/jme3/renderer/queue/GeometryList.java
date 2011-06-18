/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.renderer.queue;

import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.util.SortUtil;

/**
 * This class is a special purpose list of {@link Geometry} objects for render
 * queuing.
 *
 * @author Jack Lindamood
 * @author Three Rings - better sorting alg.
 * @author Kirill Vainer
 */
public class GeometryList {

    private static final int DEFAULT_SIZE = 32;

    private Geometry[] geometries;
    private Geometry[] geometries2;
    private int size;
    private GeometryComparator comparator;

    /**
     * Initializes the GeometryList to use the given {@link GeometryComparator}
     * to use for comparing geometries.
     * 
     * @param comparator The comparator to use.
     */
    public GeometryList(GeometryComparator comparator) {
        size = 0;
        geometries = new Geometry[DEFAULT_SIZE];
        geometries2 = new Geometry[DEFAULT_SIZE];
        this.comparator = comparator;
    }

    /**
     * Set the camera that will be set on the geometry comparators 
     * via {@link GeometryComparator#setCamera(com.jme3.renderer.Camera)}.
     * 
     * @param cam Camera to use for sorting.
     */
    public void setCamera(Camera cam){
        this.comparator.setCamera(cam);
    }

    /**
     * Returns the number of elements in this GeometryList.
     * 
     * @return Number of elements in the list
     */
    public int size(){
        return size;
    }

    /**
     * Returns the element at the given index.
     * 
     * @param index The index to lookup
     * @return Geometry at the index
     */
    public Geometry get(int index){
        return geometries[index];
    }

    /**
     * Adds a geometry to the list. 
     * List size is doubled if there is no room.
     *
     * @param g
     *            The geometry to add.
     */
    public void add(Geometry g) {
        if (size == geometries.length) {
            Geometry[] temp = new Geometry[size * 2];
            System.arraycopy(geometries, 0, temp, 0, size);
            geometries = temp; // original list replaced by double-size list
            
            geometries2 = new Geometry[size * 2];
        }
        geometries[size++] = g;
    }

    /**
     * Resets list size to 0.
     */
    public void clear() {
        for (int i = 0; i < size; i++){
            geometries[i] = null;
        }

        size = 0;
    }

    /**
     * Sorts the elements in the list according to their Comparator.
     */
    public void sort() {
        if (size > 1) {
            // sort the spatial list using the comparator
            
//            SortUtil.qsort(geometries, 0, size, comparator);
//            Arrays.sort(geometries, 0, size, comparator);            
            
            System.arraycopy(geometries, 0, geometries2, 0, size);
            SortUtil.msort(geometries2, geometries, 0, size-1, comparator);
            

        }
    }
}