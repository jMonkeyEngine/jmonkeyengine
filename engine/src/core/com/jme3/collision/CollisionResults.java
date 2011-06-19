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

package com.jme3.collision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * <code>CollisionResults</code> is a collection returned as a result of a 
 * collision detection operation done by {@link Collidable}.
 * 
 * @author Kirill Vainer
 */
public class CollisionResults implements Iterable<CollisionResult> {

    private final ArrayList<CollisionResult> results = new ArrayList<CollisionResult>();
    private boolean sorted = true;

    /**
     * Clears all collision results added to this list
     */
    public void clear(){
        results.clear();
    }

    /**
     * Iterator for iterating over the collision results.
     * 
     * @return the iterator
     */
    public Iterator<CollisionResult> iterator() {
        if (!sorted){
            Collections.sort(results);
            sorted = true;
        }

        return results.iterator();
    }

    public void addCollision(CollisionResult result){
        results.add(result);
        sorted = false;
    }

    public int size(){
        return results.size();
    }

    public CollisionResult getClosestCollision(){
        if (size() == 0)
            return null;

        if (!sorted){
            Collections.sort(results);
            sorted = true;
        }

        return results.get(0);
    }

    public CollisionResult getFarthestCollision(){
        if (size() == 0)
            return null;

        if (!sorted){
            Collections.sort(results);
            sorted = true;
        }

        return results.get(size()-1);
    }

    public CollisionResult getCollision(int index){
        if (!sorted){
            Collections.sort(results);
            sorted = true;
        }

        return results.get(index);
    }

    /**
     * Internal use only.
     * @param index
     * @return
     */
    public CollisionResult getCollisionDirect(int index){
        return results.get(index);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("CollisionResults[");
        for (CollisionResult result : results){
            sb.append(result).append(", ");
        }
        if (results.size() > 0)
            sb.setLength(sb.length()-2);

        sb.append("]");
        return sb.toString();
    }

}
