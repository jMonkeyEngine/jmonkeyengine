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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to load resources from the default location usually the
 * classpath.
 */
public class Resources {
    public static final String PROPERTY_RESOURCE_LOADER_IMPLEMENTATION = "com.jme3.ResourceLoaderImplementation";
    private static final String DEFAULT_IMPL = "com.jme3.util.res.DefaultResourceLoader";

    private static final Logger LOGGER = Logger.getLogger(Resources.class.getName());
    private static ResourceLoader impl = null;

    private static ResourceLoader getImpl() {
        if (impl != null) return impl;
        String className = System.getProperty(PROPERTY_RESOURCE_LOADER_IMPLEMENTATION, DEFAULT_IMPL);
        try {
            impl = (ResourceLoader) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (final Throwable e) {
            LOGGER.log(Level.WARNING, "Unable to access {0}", className);
            try {
                impl = (ResourceLoader) Class.forName(DEFAULT_IMPL).getDeclaredConstructor().newInstance();
            } catch (final Throwable e1) {
                throw new RuntimeException("Unable to access default resources loader implementation", e1);
            }
        }
        return impl;
    }

    public static void setImpl(ResourceLoader impl) {
        Resources.impl = impl;
    }

    public static URL getResource(String path) {
        return getImpl().getResource(path, null);
    }

    public static URL getResource(String path, Class<?> parent) {
        return getImpl().getResource(path, parent);
    }

    public static InputStream getResourceAsStream(String path) {
        return getImpl().getResourceAsStream(path, null);
    }

    public static InputStream getResourceAsStream(String path, Class<?> clazz) {
        return getImpl().getResourceAsStream(path, clazz);
    }

    public static Enumeration<URL> getResources(String path) throws IOException {
        return getImpl().getResources(path, null);
    }

    public static Enumeration<URL> getResources(String path, Class<?> clazz) throws IOException {
        return getImpl().getResources(path, clazz);
    }
}
