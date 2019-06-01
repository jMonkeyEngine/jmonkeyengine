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
package com.jme3.opencl.jocl;

import com.jme3.opencl.Platform;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author shaman
 */
public final class JoclPlatform implements Platform {
    
    final CLPlatform platform;
    List<JoclDevice> devices;
    
    public JoclPlatform(CLPlatform platform) {
        this.platform = platform;
    }

    public CLPlatform getPlatform() {
        return platform;
    }
    
    @Override
    public List<JoclDevice> getDevices() {
        if (devices == null) {
            devices = new ArrayList<>();
            for (CLDevice d : platform.listCLDevices()) {
                devices.add(new JoclDevice(d, this));
            }
        }
        return devices;
    }

    @Override
    public String getProfile() {
        return platform.getProfile();
    }

    @Override
    public boolean isFullProfile() {
        return getProfile().contains("FULL_PROFILE");
    }

    @Override
    public boolean isEmbeddedProfile() {
        return getProfile().contains("EMBEDDED_PROFILE");
    }

    @Override
    public String getVersion() {
        return platform.getVendor();
    }

    @Override
    public int getVersionMajor() {
        return Utils.getMajorVersion(getVersion(), "OpenCL ");
    }

    @Override
    public int getVersionMinor() {
        return Utils.getMinorVersion(getVersion(), "OpenCL ");
    }

    @Override
    public String getName() {
        return platform.getName();
    }

    @Override
    public String getVendor() {
        return platform.getVendor();
    }

    @Override
    public boolean hasExtension(String extension) {
        return getExtensions().contains(extension);
    }

    @Override
    public boolean hasOpenGLInterop() {
        return hasExtension("cl_khr_gl_sharing");
    }

    @Override
    public Collection<? extends String> getExtensions() {
        return platform.getExtensions();
    }

	@Override
	public String toString() {
		return getName();
	}

}
