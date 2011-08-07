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

package com.jme3.export;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <code>JmeExporter</code> specifies an export implementation for jME3 
 * data.
 */
public interface JmeExporter {
    
    /**
     * Export the {@link Savable} to an OutputStream.
     * 
     * @param object The savable to export
     * @param f The output stream
     * @return Always returns true. If an error occurs during export, 
     * an exception is thrown
     * @throws IOException If an io exception occurs during export
     */
    public boolean save(Savable object, OutputStream f) throws IOException;
    
    /**
     * Export the {@link Savable} to a file.
     * 
     * @param object The savable to export
     * @param f The file to export to
     * @return Always returns true. If an error occurs during export, 
     * an exception is thrown
     * @throws IOException If an io exception occurs during export
     */
    public boolean save(Savable object, File f) throws IOException;
    
    /**
     * Returns the {@link OutputCapsule} for the given savable object.
     * 
     * @param object The object to retrieve an output capsule for.
     * @return  
     */
    public OutputCapsule getCapsule(Savable object);
}
