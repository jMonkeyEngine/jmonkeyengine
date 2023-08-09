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

/**
 * Default java-like implementation of ResourceLoaderImpl.
 * Loads from classpath.
 */
public class ResourcesLoaderJImpl implements ResourcesLoaderImpl {

    public InputStream getResourceAsStream(String path, Class<?> parent) {
        if (parent == null) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        } else {
            return parent.getResourceAsStream(path);
        }
    }

    @Override
    public URL getResource(String path, Class<?> parent) {
        if (parent == null) {
            return Thread.currentThread().getContextClassLoader().getResource(path);
        } else {
            return parent.getResource(path);
        }
    }

    @Override
    public Enumeration<URL> getResources(String path, Class<?> parent) throws IOException {
        if (parent == null) {
            return Thread.currentThread().getContextClassLoader().getResources(path);
        } else {             
            if (!path.startsWith("/")) {
                Class<?> c = parent;
                while (c.isArray()) {
                    c = c.getComponentType();
                }
                String baseName = c.getName();
                int index = baseName.lastIndexOf('.');
                if (index != -1) {
                    path = baseName.substring(0, index).replace('.', '/') + "/" + path;
                }
            } else {
                path = path.substring(1);
            }
            return parent.getClassLoader().getResources(path);
        }
    }

}
