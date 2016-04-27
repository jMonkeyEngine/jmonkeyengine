/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

import java.util.Collection;
import java.util.List;

/**
 * A wrapper for an OpenCL platform. A platform is the highest object in the
 * object hierarchy, it creates the {@link Device}s which are then used to
 * create the {@link Context}.<br>
 * This class is mostly used within {@link PlatformChooser}.
 * 
 * @author shaman
 */
public interface Platform {

    /**
     * @return the list of available devices for this platform
     */
    List<? extends Device> getDevices();
    
    /**
     * @return The profile string
     */
    String getProfile();
    /**
     * @return {@code true} if this platform implements the full profile
     */
    boolean isFullProfile();
    /**
     * @return {@code true} if this platform implements the embedded profile
     */
    boolean isEmbeddedProfile();
    
    /**
     * @return the version string
     */
    String getVersion();
    /**
     * Extracts the major version from the version string
     * @return the major version
     */
    int getVersionMajor();
    /**
     * Extracts the minor version from the version string
     * @return the minor version
     */
    int getVersionMinor();
    
    /**
     * @return the name of the platform
     */
    String getName();
    /**
     * @return the vendor of the platform
     */
    String getVendor();
    /**
     * Queries if this platform supports OpenGL interop at all.
     * This value has also to be tested for every device.
     * @return {@code true} if OpenGL interop is supported
     */
    boolean hasOpenGLInterop();
    /**
     * Queries if the specified extension is available.
     * This value has to be tested also for every device.
     * @param extension the extension string
     * @return {@code true} if this extension is supported by the platform
     * (however, not all devices might support it as well)
     */
    boolean hasExtension(String extension);
    /**
     * @return All available extensions
     */
    Collection<? extends String> getExtensions();
    
}
