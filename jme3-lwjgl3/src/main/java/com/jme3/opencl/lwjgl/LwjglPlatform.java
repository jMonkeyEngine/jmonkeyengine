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
package com.jme3.opencl.lwjgl;

import com.jme3.lwjgl3.utils.APIBuffer;
import static com.jme3.lwjgl3.utils.APIUtil.apiBuffer;
import com.jme3.opencl.Device;
import com.jme3.opencl.Platform;
import com.jme3.opencl.lwjgl.info.Info;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import static org.lwjgl.system.Pointer.POINTER_SHIFT;

/**
 *
 * @author shaman
 */
public final class LwjglPlatform implements Platform {
    
    final long platform;
    List<LwjglDevice> devices;
    
    public LwjglPlatform(long platform) {
        this.platform = platform;
    }

    public long getPlatform() {
        return platform;
    }
    
    @Override
    public List<LwjglDevice> getDevices() {
        if (devices == null) {
            devices = new ArrayList<>();
            for (long d : getDevices(CL10.CL_DEVICE_TYPE_ALL)) {
                devices.add(new LwjglDevice(d, this));
            }
        }
        return devices;
    }
    
    /**
     * Returns a list of the available devices on this platform that match the
     * specified type, filtered by the specified filter.
     * 
     * Copied from the old release.
     *
     * @param device_type the device type
     * @param filter the device filter
     *
     * @return the available devices
     */
    private long[] getDevices(int device_type) {
        int[] count = new int[1];
        int errcode = CL10.clGetDeviceIDs(platform, device_type, null, count);
        if (errcode == CL10.CL_DEVICE_NOT_FOUND) {
            return new long[0];
        }
        Utils.checkError(errcode, "clGetDeviceIDs");

        int num_devices = count[0];
        if (num_devices == 0) {
            return new long[0];
        }

        PointerBuffer devices = PointerBuffer.allocateDirect(num_devices);

        errcode = CL10.clGetDeviceIDs(platform, device_type,devices, (IntBuffer) null);
        Utils.checkError(errcode, "clGetDeviceIDs");

        long[] deviceIDs = new long[num_devices];
        devices.rewind();
        for (int i = 0; i < num_devices; i++) {
            deviceIDs[i] = devices.get();
        }

        return deviceIDs;
    }

    @Override
    public String getProfile() {
        return Info.clGetPlatformInfoStringASCII(platform, CL10.CL_PLATFORM_PROFILE);
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
        return Info.clGetPlatformInfoStringASCII(platform, CL10.CL_PLATFORM_VERSION);
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
        return Info.clGetPlatformInfoStringASCII(platform, CL10.CL_PLATFORM_NAME);
    }

    @Override
    public String getVendor() {
        return Info.clGetPlatformInfoStringASCII(platform, CL10.CL_PLATFORM_VENDOR);
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
        return Arrays.asList(Info.clGetPlatformInfoStringASCII(platform, CL10.CL_PLATFORM_EXTENSIONS).split(" "));
    }

	@Override
	public String toString() {
		return getName();
	}

}
