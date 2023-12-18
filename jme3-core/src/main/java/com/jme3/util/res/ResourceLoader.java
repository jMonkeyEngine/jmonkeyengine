/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.util.res;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

public interface ResourceLoader {
    /**
     * Finds the resource with the given name relative to the given parent class
     * or to the root if the parent is null.
     * 
     * @param path
     *            The resource name
     * @param parent
     *            Optional parent class
     * @return The resource URL or null if not found
     */
    public URL getResource(String path, Class<?> parent);

    /**
     * Finds the resource with the given name relative to the given parent class
     * or to the root if the parent is null.
     * 
     * @param path
     *            The resource name
     * @param parent
     *            Optional parent class
     * @return An input stream to the resource or null if not found
     */
    public InputStream getResourceAsStream(String path, Class<?> parent);


    /**
     * Finds all resources with the given name.
     * 
     * 
     * @param path
     *            The resource name
     * @return An enumeration of {@link java.net.URL <code>URL</code>} objects for
     *         the resource. If no resources could be found, the enumeration
     *         will be empty.
     *
     * @throws IOException
     *             If I/O errors occur
     *
     * @throws IOException
     */
    public Enumeration<URL> getResources(String path) throws IOException;
}
