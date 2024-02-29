/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.util.struct;

/**
 * A field of a struct
 * 
 * @author Riccardo Balbo
 */
public abstract class StructField<T> {

    private int position;
    protected T value;
    protected boolean isUpdateNeeded = true;
    private String name;
    private int depth = 0;
    private int group = 0;

    protected StructField(int position, String name, T value) {
        this.position = position;
        this.value = value;
        this.name = name;
    }

    /**
     * Get depth of the field
     * 
     * @return
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the group to which this field belongs (eg. a parent struct)
     * 
     * @return id of the group
     */
    public int getGroup() {
        return group;
    }

    void setGroup(int group) {
        this.group = group;
    }

    void setDepth(int depth) {
        this.depth = depth;
    }

    void setPosition(int position) {
        this.position = position;
    }

    /**
     * Get position inside the struct
     * 
     * @return position inside the struct
     */
    public int getPosition() {
        return position;
    }

    /**
     * Get value of this field
     * 
     * @return value
     */
    public T getValue() {
        return value;
    }

    /**
     * Check if field needs update
     * 
     * @return
     */
    public boolean isUpdateNeeded() {
        return isUpdateNeeded;
    }

    /**
     * Clear update needed used internally
     */
    public void clearUpdateNeeded() {
        isUpdateNeeded = false;
    }

    /**
     * Get simple name of the field
     * 
     * @return
     */
    public String getName() {
        String friendlyName;
        if (name != null) friendlyName = name;
        else friendlyName = value.getClass().getSimpleName();
        return friendlyName;
    }

    @Override
    public String toString() {
        return "StructField[" + getName() + "] = " + value.toString();
    }

}