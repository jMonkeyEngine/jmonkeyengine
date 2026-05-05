/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.renderer.opengl;

/**
 * Describes an OpenGL image format.
 * 
 * @author Kirill Vainer
 */
public final class GLImageFormat {

    public final int internalFormat;
    public final int format;
    public final int dataType;
    public final boolean compressed;
    public final boolean colorRenderable;
    public final boolean depthRenderable;
    public final boolean filterable;
    public final boolean swizzleRequired;

    /**
     * Constructor for formats.
     * 
     * @param internalFormat OpenGL internal format
     * @param format OpenGL format
     * @param dataType OpenGL datatype
     */
    public GLImageFormat(int internalFormat, int format, int dataType, boolean colorRenderable, boolean depthRenderable, boolean filterable) {
        this.internalFormat = internalFormat;
        this.format = format;
        this.dataType = dataType;
        this.compressed = false;
        this.colorRenderable = colorRenderable;
        this.depthRenderable = depthRenderable;
        this.filterable = filterable;
        this.swizzleRequired = false;
    }
    
    /**
     * Constructor for formats.
     * 
     * @param internalFormat OpenGL internal format
     * @param format OpenGL format
     * @param dataType OpenGL datatype
     * @param compressed Format is compressed
     * @param colorRenderable Format can be used as a color render target
     * @param depthRenderable Format can be used as a depth render target
     * @param filterable Format can be filtered
     */
    public GLImageFormat(int internalFormat, int format, int dataType, boolean compressed, boolean colorRenderable, boolean depthRenderable, boolean filterable) {
        this.internalFormat = internalFormat;
        this.format = format;
        this.dataType = dataType;
        this.compressed = compressed;
        this.colorRenderable = colorRenderable;
        this.depthRenderable = depthRenderable;
        this.filterable = filterable;
        this.swizzleRequired = false;
    }
    
    /**
     * Constructor for formats.
     * 
     * @param internalFormat OpenGL internal format
     * @param format OpenGL format
     * @param dataType OpenGL datatype
     * @param compressed Format is compressed
     * @param swizzleRequired Need to use texture swizzle to upload texture
     */
    public GLImageFormat(int internalFormat, int format, int dataType, boolean compressed, boolean swizzleRequired, boolean colorRenderable, boolean depthRenderable, boolean filterable) {
        this.internalFormat = internalFormat;
        this.format = format;
        this.dataType = dataType;
        this.compressed = compressed;
        this.colorRenderable = colorRenderable;
        this.depthRenderable = depthRenderable;
        this.filterable = filterable;
        this.swizzleRequired = swizzleRequired;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GLImageFormat other = (GLImageFormat) obj;
        return internalFormat == other.internalFormat
                && format == other.format
                && dataType == other.dataType
                && compressed == other.compressed
                && colorRenderable == other.colorRenderable
                && depthRenderable == other.depthRenderable
                && filterable == other.filterable
                && swizzleRequired == other.swizzleRequired;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.internalFormat;
        hash = 97 * hash + this.format;
        hash = 97 * hash + this.dataType;
        hash = 97 * hash + (this.compressed ? 1 : 0);
        hash = 97 * hash + (this.colorRenderable ? 1 : 0);
        hash = 97 * hash + (this.depthRenderable ? 1 : 0);
        hash = 97 * hash + (this.filterable ? 1 : 0);
        hash = 97 * hash + (this.swizzleRequired ? 1 : 0);
        return hash;
    }
}
