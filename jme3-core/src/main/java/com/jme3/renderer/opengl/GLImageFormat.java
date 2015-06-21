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
    public final boolean swizzleRequired;

    /**
     * Constructor for formats.
     * 
     * @param internalFormat OpenGL internal format
     * @param format OpenGL format
     * @param dataType OpenGL datatype
     */
    public GLImageFormat(int internalFormat, int format, int dataType) {
        this.internalFormat = internalFormat;
        this.format = format;
        this.dataType = dataType;
        this.compressed = false;
        this.swizzleRequired = false;
    }
    
    /**
     * Constructor for formats.
     * 
     * @param internalFormat OpenGL internal format
     * @param format OpenGL format
     * @param dataType OpenGL datatype
     * @param compressed Format is compressed
     */
    public GLImageFormat(int internalFormat, int format, int dataType, boolean compressed) {
        this.internalFormat = internalFormat;
        this.format = format;
        this.dataType = dataType;
        this.compressed = compressed;
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
    public GLImageFormat(int internalFormat, int format, int dataType, boolean compressed, boolean swizzleRequired) {
        this.internalFormat = internalFormat;
        this.format = format;
        this.dataType = dataType;
        this.compressed = compressed;
        this.swizzleRequired = swizzleRequired;
    }
}
