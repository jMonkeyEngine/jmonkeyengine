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

package com.jme3.util;

/**
 * Describes a native object. An encapsulation of a certain object 
 * on the native side of the graphics or audio library.
 * 
 * This class is used to track when OpenGL and OpenAL native objects are 
 * collected by the garbage collector, and then invoke the proper destructor
 * on the OpenGL library to delete it from memory.
 */
public abstract class NativeObject implements Cloneable {

    /**
     * The ID of the object, usually depends on its type.
     * Typically returned from calls like glGenTextures, glGenBuffers, etc.
     */
    protected int id = -1;

    /**
     * A reference to a "handle". By hard referencing a certain object, it's
     * possible to find when a certain GLObject is no longer used, and to delete
     * its instance from the graphics library.
     */
    protected Object handleRef = null;

    /**
     * True if the data represented by this GLObject has been changed
     * and needs to be updated before used.
     */
    protected boolean updateNeeded = true;

    /**
     * The type of the GLObject, usually specified by a subclass.
     */
    protected final Class<?> type;

    /**
     * Creates a new GLObject with the given type. Should be
     * called by the subclasses.
     * 
     * @param type The type that the subclass represents.
     */
    public NativeObject(Class<?> type){
        this.type = type;
        this.handleRef = new Object();
    }

    /**
     * Protected constructor that doesn't allocate handle ref.
     * This is used in subclasses for the createDestructableClone().
     */
    protected NativeObject(Class<?> type, int id){
        this.type = type;
        this.id = id;
    }

    /**
     * Sets the ID of the GLObject. This method is used in Renderer and must
     * not be called by the user.
     * @param id The ID to set
     */
    public void setId(int id){
        if (this.id != -1)
            throw new IllegalStateException("ID has already been set for this GL object.");

        this.id = id;
    }

    /**
     * @return The ID of the object. Should not be used by user code in most
     * cases.
     */
    public int getId(){
        return id;
    }

    /**
     * Internal use only. Indicates that the object has changed
     * and its state needs to be updated.
     */
    public void setUpdateNeeded(){
        updateNeeded = true;
    }

    /**
     * Internal use only. Indicates that the state changes were applied.
     */
    public void clearUpdateNeeded(){
        updateNeeded = false;
    }

    /**
     * Internal use only. Check if {@link #setUpdateNeeded()} was called before.
     */
    public boolean isUpdateNeeded(){
        return updateNeeded;
    }

    @Override
    public String toString(){
        return "Native" + type.getSimpleName() + " " + id;
    }

    /**
     * This should create a deep clone. For a shallow clone, use
     * createDestructableClone().
     */
    @Override
    protected NativeObject clone(){
        try{
            NativeObject obj = (NativeObject) super.clone();
            obj.handleRef = new Object();
            obj.id = -1;
            obj.updateNeeded = true;
            return obj;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

    /**
     * Called when the GL context is restarted to reset all IDs. Prevents
     * "white textures" on display restart.
     */
    public abstract void resetObject();

    /**
     * Deletes the GL object from the GPU when it is no longer used. Called
     * automatically by the GL object manager.
     * 
     * @param rendererObject The renderer to be used to delete the object
     */
    public abstract void deleteObject(Object rendererObject);

    /**
     * Creates a shallow clone of this GL Object. The deleteObject method
     * should be functional for this object.
     */
    public abstract NativeObject createDestructableClone();
}
