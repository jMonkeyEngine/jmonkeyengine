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

import com.jme3.opencl.Device;
import com.jogamp.opencl.CLDevice;
import java.util.Collection;

/**
 *
 * @author shaman
 */
public final class JoclDevice implements Device {

    final long id;
    final CLDevice device;
    final JoclPlatform platform;

    public JoclDevice(CLDevice device, JoclPlatform platform) {
        this.id = device.ID;
        this.device = device;
        this.platform = platform;
    }

    public long getId() {
        return id;
    }

    public CLDevice getDevice() {
        return device;
    }
    
    @Override
    public JoclPlatform getPlatform() {
        return platform;
    }

    @Override
    public DeviceType getDeviceType() {
        CLDevice.Type type = device.getType();
        switch (type) {
            case ACCELERATOR: return DeviceType.ACCELEARTOR;
            case CPU: return DeviceType.CPU;
            case GPU: return DeviceType.GPU;
            default: return DeviceType.DEFAULT;
        }
    }

    @Override
    public int getVendorId() {
        return (int) device.getVendorID();
    }

    @Override
    public boolean isAvailable() {
        return device.isAvailable();
    }

    @Override
    public boolean hasCompiler() {
        return device.isCompilerAvailable();
    }

    @Override
    public boolean hasDouble() {
        return hasExtension("cl_khr_fp64");
    }

    @Override
    public boolean hasHalfFloat() {
        return hasExtension("cl_khr_fp16");
    }

    @Override
    public boolean hasErrorCorrectingMemory() {
        return device.isErrorCorrectionSupported();
    }

    @Override
    public boolean hasUnifiedMemory() {
        return device.isMemoryUnified();
    }

    @Override
    public boolean hasImageSupport() {
        return device.isImageSupportAvailable();
    }
    
    @Override
    public boolean hasWritableImage3D() {
        return hasExtension("cl_khr_3d_image_writes");
    }

    @Override
    public boolean hasOpenGLInterop() {
        return hasExtension("cl_khr_gl_sharing");
    }
    
    @Override
    public boolean hasExtension(String extension) {
        return getExtensions().contains(extension);
    }

    @Override
    public Collection<? extends String> getExtensions() {
        return device.getExtensions();
    }

    @Override
    public int getComputeUnits() {
        return device.getMaxComputeUnits();
    }

    @Override
    public int getClockFrequency() {
        return device.getMaxClockFrequency();
    }

    @Override
    public int getAddressBits() {
        return device.getAddressBits();
    }

    @Override
    public boolean isLittleEndian() {
        return device.isLittleEndian();
    }

    @Override
    public long getMaximumWorkItemDimensions() {
        return device.getMaxWorkItemDimensions();
    }

    @Override
    public long[] getMaximumWorkItemSizes() {
        int[] sizes = device.getMaxWorkItemSizes();
        long[] s = new long[sizes.length];
        for (int i=0; i<sizes.length; ++i) {
            s[i] = sizes[i];
        }
        return s;
    }

    @Override
    public long getMaxiumWorkItemsPerGroup() {
        return device.getMaxWorkGroupSize();
    }

    @Override
    public int getMaximumSamplers() {
        return device.getMaxSamplers();
    }

    @Override
    public int getMaximumReadImages() {
        return device.getMaxReadImageArgs();
    }

    @Override
    public int getMaximumWriteImages() {
        return device.getMaxWriteImageArgs();
    }

    @Override
    public long[] getMaximumImage2DSize() {
        return new long[] {
            device.getMaxImage2dWidth(),
            device.getMaxImage2dHeight()
        };
    }

    @Override
    public long[] getMaximumImage3DSize() {
        return new long[] {
            device.getMaxImage3dWidth(),
            device.getMaxImage3dHeight(),
            device.getMaxImage3dDepth()
        };
    }
    
    @Override
    public long getMaximumAllocationSize() {
        return device.getMaxMemAllocSize();
    }
    
    @Override
    public long getGlobalMemorySize() {
        return device.getGlobalMemSize();
    }
    
    @Override
    public long getLocalMemorySize() {
        return device.getLocalMemSize();
    }
    
    @Override
    public long getMaximumConstantBufferSize() {
        return device.getMaxConstantBufferSize();
    }
    
    @Override
    public int getMaximumConstantArguments() {
        return (int) device.getMaxConstantArgs();
    }

    @Override
    public String getProfile() {
        return device.getProfile();
    }

    @Override
    public String getVersion() {
        return device.getVersion().toString();
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
    public String getCompilerVersion() {
        return "OpenCL C 1.1"; //at most OpenCL 1.1 is supported at all
    }

    @Override
    public int getCompilerVersionMajor() {
        return Utils.getMajorVersion(getCompilerVersion(), "OpenCL C ");
    }

    @Override
    public int getCompilerVersionMinor() {
        return Utils.getMinorVersion(getCompilerVersion(), "OpenCL C ");
    }

    @Override
    public String getDriverVersion() {
        return device.getDriverVersion();
    }

    @Override
    public int getDriverVersionMajor() {
        return Utils.getMajorVersion(getDriverVersion(), "");
    }

    @Override
    public int getDriverVersionMinor() {
        return Utils.getMinorVersion(getDriverVersion(), "");
    }

    @Override
    public String getName() {
        return device.getName();
    }

    @Override
    public String getVendor() {
        return device.getVendor();
    }

    @Override
    public String toString() {
        return getName();
    }
    
}
