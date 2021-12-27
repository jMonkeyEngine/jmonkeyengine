/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.system;

/**
 * Holds information about a native library for a particular platform.
 * 
 * @author Kirill Vainer
 */
final class NativeLibrary {
    
    private final String name;
    private final Platform platform;
    private final String pathInNativesJar;
    private final String extractedAsFileName;

    /**
     * Key for map to find a library for a name and platform.
     */
    static final class Key {

        private final String name;
        private final Platform platform;

        public Key(String name, Platform platform) {
            this.name = name;
            this.platform = platform;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + this.name.hashCode();
            hash = 79 * hash + this.platform.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            final Key other = (Key) obj;
            if (!this.name.equals(other.name)) {
                return false;
            }
            if (this.platform != other.platform) {
                return false;
            }
            return true;
        }
    }
    
    /**
     * The name of the library. 
     * Generally only used as a way to uniquely identify the library.
     * 
     * @return name of the library.
     */
    public String getName() {
        return name;
    }

    /**
     * The OS + architecture combination for which this library
     * should be extracted.
     * 
     * @return platform associated to this native library
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * The filename that the library should be extracted as.
     * 
     * In some cases, this differs from the {@link #getPathInNativesJar() path in the natives jar},
     * since the names of the libraries specified in the jars are often incorrect.
     * If set to <code>null</code>, then the filename in the
     * natives jar shall be used.
     * 
     * @return the name that should be given to the extracted file.
     */
    public String getExtractedAsName() {
        return extractedAsFileName;
    }

    /**
     * Path inside the natives jar or classpath where the library is located.
     * 
     * This library must be compatible with the {@link #getPlatform() platform}
     * which this library is associated with.
     * 
     * @return path to the library in the classpath
     */
    public String getPathInNativesJar() {
        return pathInNativesJar;
    }

    /**
     * Create a new NativeLibrary.
     */
    public NativeLibrary(String name, Platform platform, String pathInNativesJar, String extractedAsFileName) {
        this.name = name;
        this.platform = platform;
        this.pathInNativesJar = pathInNativesJar;
        this.extractedAsFileName = extractedAsFileName;
    }

    /**
     * Create a new NativeLibrary.
     */
    public NativeLibrary(String name, Platform platform, String pathInNativesJar) {
        this(name, platform, pathInNativesJar, null);
    }
}
