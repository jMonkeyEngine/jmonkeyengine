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

package com.jme3.light;

import com.jme3.export.*;
import com.jme3.scene.Spatial;
import com.jme3.util.SortUtil;
import java.io.IOException;
import java.util.*;

/**
 * <code>LightList</code> is used internally by {@link Spatial}s to manage
 * lights that are attached to them.
 * 
 * @author Kirill Vainer
 */
public final class LightList implements Iterable<Light>, Savable, Cloneable {

    private Light[] list, tlist;
    private float[] distToOwner;
    private int listSize;
    private Spatial owner;

    private static final int DEFAULT_SIZE = 1;

    private static final Comparator<Light> c = new Comparator<Light>() {
        /**
         * This assumes lastDistance have been computed in a previous step.
         */
        public int compare(Light l1, Light l2) {
            if (l1.lastDistance < l2.lastDistance)
                return -1;
            else if (l1.lastDistance > l2.lastDistance)
                return 1;
            else
                return 0;
        }
    };

    /**
     * Default constructor for serialization. Do not use
     */
    public LightList(){
    }

    /**
     * Creates a <code>LightList</code> for the given {@link Spatial}.
     * 
     * @param owner The spatial owner
     */
    public LightList(Spatial owner) {
        listSize = 0;
        list = new Light[DEFAULT_SIZE];
        distToOwner = new float[DEFAULT_SIZE];
        Arrays.fill(distToOwner, Float.NEGATIVE_INFINITY);
        this.owner = owner;
    }

    /**
     * Set the owner of the LightList. Only used for cloning.
     * @param owner 
     */
    public void setOwner(Spatial owner){
        this.owner = owner;
    }

    private void doubleSize(){
        Light[] temp = new Light[list.length * 2];
        float[] temp2 = new float[list.length * 2];
        System.arraycopy(list, 0, temp, 0, list.length);
        System.arraycopy(distToOwner, 0, temp2, 0, list.length);
        list = temp;
        distToOwner = temp2;
    }

    /**
     * Adds a light to the list. List size is doubled if there is no room.
     *
     * @param l
     *            The light to add.
     */
    public void add(Light l) {
        if (listSize == list.length) {
            doubleSize();
        }
        list[listSize] = l;
        distToOwner[listSize++] = Float.NEGATIVE_INFINITY;
    }

    /**
     * Remove the light at the given index.
     * 
     * @param index
     */
    public void remove(int index){
        if (index >= listSize || index < 0)
            throw new IndexOutOfBoundsException();

        listSize --;
        if (index == listSize){
            list[listSize] = null;
            return;
        }

        for (int i = index; i < listSize; i++){
            list[i] = list[i+1];
        }
        list[listSize] = null;
    }

    /**
     * Removes the given light from the LightList.
     * 
     * @param l the light to remove
     */
    public void remove(Light l){
        for (int i = 0; i < listSize; i++){
            if (list[i] == l){
                remove(i);
                return;
            }
        }
    }

    /**
     * @return The size of the list.
     */
    public int size(){
        return listSize;
    }

    /**
     * @return the light at the given index.
     * @throws IndexOutOfBoundsException If the given index is outside bounds.
     */
    public Light get(int num){
        if (num >= listSize || num < 0)
            throw new IndexOutOfBoundsException();

        return list[num];
    }

    /**
     * Resets list size to 0.
     */
    public void clear() {
        if (listSize == 0)
            return;

        for (int i = 0; i < listSize; i++)
            list[i] = null;

        if (tlist != null)
            Arrays.fill(tlist, null);

        listSize = 0;
    }

    /**
     * Sorts the elements in the list acording to their Comparator.
     * There are two reasons why lights should be resorted. 
     * First, if the lights have moved, that means their distance to 
     * the spatial changed. 
     * Second, if the spatial itself moved, it means the distance from it to 
     * the individual lights might have changed.
     * 
     *
     * @param transformChanged Whether the spatial's transform has changed
     */
    public void sort(boolean transformChanged) {
        if (listSize > 1) {
            // resize or populate our temporary array as necessary
            if (tlist == null || tlist.length != list.length) {
                tlist = list.clone();
            } else {
                System.arraycopy(list, 0, tlist, 0, list.length);
            }

            if (transformChanged){
                // check distance of each light
                for (int i = 0; i < listSize; i++){
                    list[i].computeLastDistance(owner);
                }
            }

            // now merge sort tlist into list
            SortUtil.msort(tlist, list, 0, listSize - 1, c);
        }
    }

    /**
     * Updates a "world-space" light list, using the spatial's local-space
     * light list and its parent's world-space light list.
     *
     * @param local
     * @param parent
     */
    public void update(LightList local, LightList parent){
        // clear the list as it will be reconstructed
        // using the arguments
        clear();

        while (list.length <= local.listSize){
            doubleSize();
        }

        // add the lights from the local list
        System.arraycopy(local.list, 0, list, 0, local.listSize);
        for (int i = 0; i < local.listSize; i++){
//            list[i] = local.list[i];
            distToOwner[i] = Float.NEGATIVE_INFINITY;
        }

        // if the spatial has a parent node, add the lights
        // from the parent list as well
        if (parent != null){
            int sz = local.listSize + parent.listSize;
            while (list.length <= sz)
                doubleSize();

            for (int i = 0; i < parent.listSize; i++){
                int p = i + local.listSize;
                list[p] = parent.list[i];
                distToOwner[p] = Float.NEGATIVE_INFINITY;
            }
            
            listSize = local.listSize + parent.listSize;
        }else{
            listSize = local.listSize;
        }
    }

    /**
     * Returns an iterator that can be used to iterate over this LightList.
     * 
     * @return an iterator that can be used to iterate over this LightList.
     */
    public Iterator<Light> iterator() {
        return new Iterator<Light>(){

            int index = 0;

            public boolean hasNext() {
                return index < size();
            }

            public Light next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                
                return list[index++];
            }
            
            public void remove() {
                LightList.this.remove(--index);
            }
        };
    }

    @Override
    public LightList clone(){
        try{
            LightList clone = (LightList) super.clone();
            
            clone.owner = null;
            clone.list = list.clone();
            clone.distToOwner = distToOwner.clone();
            clone.tlist = null; // list used for sorting only

            return clone;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
//        oc.write(owner, "owner", null);

        ArrayList<Light> lights = new ArrayList<Light>();
        for (int i = 0; i < listSize; i++){
            lights.add(list[i]);
        }
        oc.writeSavableArrayList(lights, "lights", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
//        owner = (Spatial) ic.readSavable("owner", null);

        List<Light> lights = ic.readSavableArrayList("lights", null);
        listSize = lights.size();
        
        // NOTE: make sure the array has a length of at least 1
        int arraySize = Math.max(DEFAULT_SIZE, listSize);
        list = new Light[arraySize];
        distToOwner = new float[arraySize];

        for (int i = 0; i < listSize; i++){
            list[i] = lights.get(i);
        }
        
        Arrays.fill(distToOwner, Float.NEGATIVE_INFINITY);
    }

}
