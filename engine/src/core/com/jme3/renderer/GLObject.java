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

package com.jme3.renderer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describes a GL object. An encapsulation of a certain object 
 * on the native side of the graphics library.
 * This class is used to track
 */
public abstract class GLObject implements Cloneable {

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
    protected final Type type;

    public static enum Type {
        /**
         * A texture is an image that is applied to geometry.
         */
        Texture,

        /**
         * Vertex buffers are used to describe geometry data and it's attributes.
         */
        VertexBuffer,

        /**
         * ShaderSource is a shader source code that controls the output of
         * a certain rendering pipeline, like vertex position or fragment color.
         */
        ShaderSource,

        /**
         * A Shader is an aggregation of ShaderSources, collectively
         * they cooperate to control the vertex and fragment processor.
         */
        Shader,

        /**
         * FrameBuffer is an offscreen surface which can be rendered to.
         * Can be used to create "Render-to-Texture" effects and
         * scene post processing.
         */
        FrameBuffer,
    }

    public GLObject(Type type){
        this.type = type;
        this.handleRef = new Object();
    }

    /**
     * Protected constructor that doesn't allocate handle ref.
     * This is used in subclasses for the createDestructableClone().
     */
    protected GLObject(Type type, int id){
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

    public void setUpdateNeeded(){
        updateNeeded = true;
    }

    /**
     * 
     */
    public void clearUpdateNeeded(){
        updateNeeded = false;
    }

    public boolean isUpdateNeeded(){
        return updateNeeded;
    }

    @Override
    public String toString(){
        return type.name() + " " + Integer.toHexString(hashCode());
    }

    /**
     * This should create a deep clone. For a shallow clone, use
     * createDestructableClone().
     */
    @Override
    protected GLObject clone(){
        try{
            GLObject obj = (GLObject) super.clone();
            obj.handleRef = new Object();
            obj.id = -1;
            obj.updateNeeded = true;
            return obj;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

//    @Override
//    public boolean equals(Object other){
//        if (this == other)
//            return true;
//        if (!(other instanceof GLObject))
//            return false;
//
//    }

    // Specialized calls to be used by object manager only.

    /**
     * Called when the GL context is restarted to reset all IDs. Prevents
     * "white textures" on display restart.
     */
    public abstract void resetObject();

    /**
     * Deletes the GL object from the GPU when it is no longer used. Called
     * automatically by the GL object manager.
     * @param r
     */
    public abstract void deleteObject(Renderer r);

    /**
     * Creates a shallow clone of this GL Object. The deleteObject method
     * should be functional for this object.
     */
    public abstract GLObject createDestructableClone();
}
